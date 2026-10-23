package com.cmak.integrations.bundle.sms.config;


import com.cmak.integrations.bundle.config.Constants;

/**
 * In-memory view of the SMS configuration record.
 * Replaces the java.util.Properties loaded from Integ_common_config.properties.
 *
 * Legacy key -> field mapping:
 *   SMS_USER            -> smsUser
 *   SMS_PASSWORD        -> smsPassword (already decrypted when this object is built)
 *   SMS_WS_URL          -> smsWsUrl
 *   IT_SMS_SERVICENAME  -> defaultOriginator
 */
public class SmsConfig {

    private String smsUser;
    private String smsPassword;
    private String smsWsUrl;
    private String defaultOriginator;
    private String soapNamespace = Constants.SmsConstants.DEFAULT_SOAP_NAMESPACE;
    private int customerId = Constants.SmsConstants.DEFAULT_CUSTOMER_ID;
    private int connectTimeoutMs = Constants.SmsConstants.DEFAULT_CONNECT_TIMEOUT_MS;
    private int readTimeoutMs = Constants.SmsConstants.DEFAULT_READ_TIMEOUT_MS;
    private boolean enabled = true;

    public String getSmsUser() {
        return smsUser;
    }

    public void setSmsUser(String smsUser) {
        this.smsUser = smsUser;
    }

    public String getSmsPassword() {
        return smsPassword;
    }

    public void setSmsPassword(String smsPassword) {
        this.smsPassword = smsPassword;
    }

    public String getSmsWsUrl() {
        return smsWsUrl;
    }

    public void setSmsWsUrl(String smsWsUrl) {
        this.smsWsUrl = smsWsUrl;
    }

    public String getDefaultOriginator() {
        return defaultOriginator;
    }

    public void setDefaultOriginator(String defaultOriginator) {
        this.defaultOriginator = defaultOriginator;
    }

    public String getSoapNamespace() {
        return soapNamespace;
    }

    public void setSoapNamespace(String soapNamespace) {
        this.soapNamespace = soapNamespace;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** Validation that used to be scattered through the legacy main(). */
    public String validate() {
        if (isBlank(smsUser)) {
            return "SMS user is not configured";
        }
        if (isBlank(smsPassword)) {
            return "SMS password is not configured";
        }
        if (isBlank(smsWsUrl)) {
            return "SMS web service URL is not configured";
        }
        if (isBlank(defaultOriginator)) {
            return "Default originator is not configured";
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /** Never log the password. */
    @Override
    public String toString() {
        return "SmsConfig{user=" + smsUser
                + ", url=" + smsWsUrl
                + ", originator=" + defaultOriginator
                + ", customerId=" + customerId
                + ", enabled=" + enabled + "}";
    }
}
