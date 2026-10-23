package com.cmak.integrations.bundle.lbs.service;

import javax.validation.constraints.NotBlank;

import com.bmc.arsys.rx.application.common.ServiceLocator;
import com.bmc.arsys.rx.services.action.domain.Action;
import com.bmc.arsys.rx.services.action.domain.ActionParameter;
import com.bmc.arsys.rx.services.common.Service;
import com.bmc.arsys.rx.services.common.domain.Scope;
import com.cmak.integrations.bundle.config.Constants;
import com.cmak.integrations.bundle.lbs.config.LbsConfig;
import com.cmak.integrations.bundle.lbs.config.LbsConfigService;
import com.cmak.integrations.bundle.lbs.domain.LbsLocationResult;
import com.cmak.integrations.bundle.lbs.client.MlpClient;

/**
 * Innovation Studio replacement for lbs.LBS.main(String[]).
 *
 * Argument mapping (old -> new):
 *   args[0] phone without 966 -> phoneNumber (any format now, see normalise)
 *   args[1] max_loc_age       -> maxLocationAgeSeconds
 *
 * The legacy jar also opened an ARServerUser connection on port 6000 and
 * never used it. That is gone - this code already runs inside Helix.
 */
public class LbsService implements Service {

    private final LbsConfigService configService;

    public LbsService(LbsConfigService configService) {
        this.configService = configService;
    }

    @Action(name = "locateSubscriber",
            scope = Scope.PUBLIC,
            displayName = "LBS")
    public LbsLocationResult locateSubscriber(
            @ActionParameter(name = "phoneNumber") @NotBlank String phoneNumber,
            @ActionParameter(name = "maxLocationAgeSeconds") String maxLocationAgeSeconds) {

        ServiceLocator.getLogger().info("locateSubscriber invoked - Phone: " + phoneNumber
                + " | maxLocationAge: " + maxLocationAgeSeconds);

        // ---- 1. Configuration ----
        LbsConfig config = configService.getConfig();

        if (!config.isEnabled()) {
            String message = "LBS integration is disabled in the configuration record.";
            ServiceLocator.getLogger().warn(message);
            return LbsLocationResult.failed(message, null);
        }

        String configError = config.validate();
        if (configError != null) {
            ServiceLocator.getLogger().error("LBS configuration invalid: " + configError);
            return LbsLocationResult.failed(configError, null);
        }

        // ---- 2. Phone number ----
        String msisdn = normaliseMsisdn(phoneNumber);
        if (msisdn == null) {
            String message = "Phone number [" + phoneNumber + "] contains no digits.";
            ServiceLocator.getLogger().error(message);
            return LbsLocationResult.failed(message, null);
        }

        // ---- 3. Max location age ----
        int maxAge = parseMaxAge(maxLocationAgeSeconds, config.getDefaultMaxLocAgeSeconds());

        // ---- 4. Call the LBS service ----
        LbsLocationResult result = new MlpClient(config).locate(msisdn, maxAge);

        if (result.isSuccess()) {
            ServiceLocator.getLogger().info("Location found - MSISDN: " + msisdn
                    + " | lat: " + result.getLatitude() + " | lon: " + result.getLongitude());
        } else {
            ServiceLocator.getLogger().error("Location NOT found - MSISDN: " + msisdn
                    + " | reason: " + result.getErrorMessage());
        }

        return result;
    }

    /**
     * Checks the settings and reachability WITHOUT locating a real subscriber.
     *
     * There is no read-only ping in MLP, so this sends a real slir for a
     * deliberately invalid number. A reply of any kind - even a refusal -
     * proves the URL is reachable, the client id and password were read from
     * the record, and the reply can be parsed. Look at rawResponse: that is
     * the MLP sample the old project never had.
     */
//    @Action(name = "testLbsConnection",
//            scope = Scope.PUBLIC,
//            displayName = "Test LBS Connection")
//    public LbsLocationResult testLbsConnection() {
//
//        LbsConfig config = configService.getConfig();
//
//        String configError = config.validate();
//        if (configError != null) {
//            ServiceLocator.getLogger().error("LBS configuration incomplete: " + configError);
//            return LbsLocationResult.failed(configError, null);
//        }
//
//        // 966500000000 is well formed but should belong to nobody.
//        LbsLocationResult result = new MlpClient(config).locate("966500000000", 3600);
//
//        if (result.getRawResponse() != null && !result.getRawResponse().isEmpty()) {
//            ServiceLocator.getLogger().info(
//                    "LBS connection test reached the service. Reply captured for analysis.");
//        }
//
//        return result;
//    }

    /* ------------------------------------------------------------------ */

    /**
     * The old code blindly did "966" + args[0], so passing an already
     * prefixed number produced 966966512345678. This accepts every common
     * form and always produces 966XXXXXXXXX.
     */
    static String normaliseMsisdn(String raw) {
        if (raw == null) {
            return null;
        }
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return null;
        }
        if (digits.startsWith("00" + Constants.LbsConstants.KSA_COUNTRY_CODE)) {
            digits = digits.substring(2 + Constants.LbsConstants.KSA_COUNTRY_CODE.length());
        } else if (digits.startsWith(Constants.LbsConstants.KSA_COUNTRY_CODE)) {
            digits = digits.substring(Constants.LbsConstants.KSA_COUNTRY_CODE.length());
        } else if (digits.startsWith("0")) {
            digits = digits.substring(1);
        }
        if (digits.isEmpty()) {
            return null;
        }
        return Constants.LbsConstants.KSA_COUNTRY_CODE + digits;
    }

    private static int parseMaxAge(String value, int fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed >= 0 ? parsed : fallback;
        } catch (NumberFormatException e) {
            ServiceLocator.getLogger().warn("maxLocationAgeSeconds [" + value
                    + "] is not a number, using " + fallback);
            return fallback;
        }
    }
}