package com.cmak.integrations.bundle.lbs.config;

import com.bmc.arsys.rx.application.common.ServiceLocator;
import com.bmc.arsys.rx.services.common.Service;
import com.cmak.integrations.bundle.config.Constants;
import com.cmak.integrations.bundle.config.IntegrationConfigService;

import java.util.Map;

/**
 * Maps the "LBS" row of the centralized configuration record onto LbsConfig.
 *
 *   Hostname_IP              -> lbsUrl
 *   Password                 -> lbsPassword
 *   Username                 -> clientId  (the <id> element in the MLP header)
 *   Default Max Location Age -> defaultMaxLocAgeSeconds
 *   Connection Timeout       -> connectTimeoutMs
 *   Read Timeout             -> readTimeoutMs
 *   Integration Status       -> enabled
 *
 * Username, not Client ID: the properties file had LBS_USER=<your_lbs_client_id> and the
 * deployed row puts <your_lbs_client_id> in Username, leaving Client ID empty.
 */
public class LbsConfigService implements Service {

    private final IntegrationConfigService configService;

    public LbsConfigService(IntegrationConfigService configService) {
        this.configService = configService;
    }

    public LbsConfig getConfig() {
        Map<String, Object> row = configService.getRow(Constants.INTEGRATION_LBS);

        LbsConfig config = new LbsConfig();
        if (row == null) {
            ServiceLocator.getLogger().error("[LBS-CONFIG] No configuration row available - "
                    + "returning empty config, validate() will refuse the call");
            return config;
        }

        config.setLbsUrl(IntegrationConfigService.asString(
                row, Constants.FIELD_ID_HOSTNAME_IP));
        config.setLbsPassword(IntegrationConfigService.asString(
                row, Constants.FIELD_ID_PASSWORD));

        String clientId = IntegrationConfigService.asString(
                row, Constants.FIELD_ID_USERNAME);
        if (clientId == null || clientId.trim().isEmpty()) {
            clientId = IntegrationConfigService.asString(
                    row, Constants.FIELD_ID_CLIENT_ID);
            if (clientId != null && !clientId.trim().isEmpty()) {
                ServiceLocator.getLogger().info("[LBS-CONFIG] Username was empty, "
                        + "using Client ID instead");
            }
        }
        if (clientId != null && !clientId.trim().isEmpty()) {
            config.setClientId(clientId.trim());
        } else {
            ServiceLocator.getLogger().warn("[LBS-CONFIG] Username and Client ID are both "
                    + "empty, using the legacy default "
                    + Constants.LbsConstants.DEFAULT_CLIENT_ID);
        }

        config.setDefaultMaxLocAgeSeconds(IntegrationConfigService.asInt(
                row, Constants.FIELD_ID_MAX_LOCATION_AGE,
                Constants.LbsConstants.DEFAULT_MAX_LOC_AGE_SECONDS));
        config.setConnectTimeoutMs(IntegrationConfigService.asInt(
                row, Constants.FIELD_ID_CONNECTION_TIMEOUT,
                Constants.LbsConstants.DEFAULT_CONNECT_TIMEOUT_MS));
        config.setReadTimeoutMs(IntegrationConfigService.asInt(
                row, Constants.FIELD_ID_READ_TIMEOUT,
                Constants.LbsConstants.DEFAULT_READ_TIMEOUT_MS));

        config.setEnabled(IntegrationConfigService.isEnabled(row));

        ServiceLocator.getLogger().info("[LBS-CONFIG] Mapped configuration: " + config);
        return config;
    }

    public void invalidate() {
        configService.invalidate();
    }
}