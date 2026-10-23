package com.cmak.integrations.bundle.sms.config;

import com.bmc.arsys.rx.application.common.ServiceLocator;
import com.bmc.arsys.rx.services.common.Service;
import com.cmak.integrations.bundle.config.Constants;
import com.cmak.integrations.bundle.config.IntegrationConfigService;

import java.util.Map;

/**
 * Maps the "SMS" row of the centralized configuration record onto SmsConfig.
 *
 *   Customer ID        -> customerId         (legacy hard-coded 128)
 *   Username           -> smsUser            (was SMS_USER)
 *   Password           -> smsPassword        (was SMS_PASSWORD)
 *   Hostname_IP        -> smsWsUrl           (was SMS_WS_URL)
 *   Default Originator -> defaultOriginator  (was IT_SMS_SERVICENAME)
 *   SOAP Namespace     -> soapNamespace
 *   Connection Timeout -> connectTimeoutMs
 *   Read Timeout       -> readTimeoutMs
 *   Integration Status -> enabled
 *
 * Every value now comes from the record. The constants in Constants.SmsConstants
 * are only used when a field is left blank.
 */
public class SmsConfigService implements Service {

    private final IntegrationConfigService configService;

    public SmsConfigService(IntegrationConfigService configService) {
        this.configService = configService;
    }

    public SmsConfig getConfig() {
        Map<String, Object> row = configService.getRow(Constants.INTEGRATION_SMS);

        SmsConfig config = new SmsConfig();
        if (row == null) {
            ServiceLocator.getLogger().error("[SMS-CONFIG] No configuration row available - "
                    + "returning empty config, validate() will refuse the call");
            return config;
        }

        config.setSmsUser(IntegrationConfigService.asString(
                row, Constants.FIELD_ID_USERNAME));
        config.setSmsPassword(IntegrationConfigService.asString(
                row, Constants.FIELD_ID_PASSWORD));
        config.setSmsWsUrl(IntegrationConfigService.asString(
                row, Constants.FIELD_ID_HOSTNAME_IP));
        config.setDefaultOriginator(IntegrationConfigService.asString(
                row, Constants.FIELD_ID_DEFAULT_ORIGINATOR));

        String namespace = IntegrationConfigService.asString(
                row, Constants.FIELD_ID_SOAP_NAMESPACE);
        if (namespace != null && !namespace.trim().isEmpty()) {
            config.setSoapNamespace(namespace.trim());
        } else {
            ServiceLocator.getLogger().info("[SMS-CONFIG] SOAP Namespace is empty, using default "
                    + Constants.SmsConstants.DEFAULT_SOAP_NAMESPACE);
        }

        config.setCustomerId(IntegrationConfigService.asInt(
                row, Constants.FIELD_ID_CUSTOMER_ID,
                Constants.SmsConstants.DEFAULT_CUSTOMER_ID));

        config.setConnectTimeoutMs(IntegrationConfigService.asInt(
                row, Constants.FIELD_ID_CONNECTION_TIMEOUT,
                Constants.SmsConstants.DEFAULT_CONNECT_TIMEOUT_MS));
        config.setReadTimeoutMs(IntegrationConfigService.asInt(
                row, Constants.FIELD_ID_READ_TIMEOUT,
                Constants.SmsConstants.DEFAULT_READ_TIMEOUT_MS));

        config.setEnabled(IntegrationConfigService.isEnabled(row));

        ServiceLocator.getLogger().info("[SMS-CONFIG] Mapped configuration: " + config);
        return config;
    }

    public void invalidate() {
        configService.invalidate();
    }
}