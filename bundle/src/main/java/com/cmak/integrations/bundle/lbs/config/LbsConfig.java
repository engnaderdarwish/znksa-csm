package com.cmak.integrations.bundle.lbs.config;

import com.cmak.integrations.bundle.config.Constants;

/**
 * In-memory view of the LBS Configuration record.
 *
 * Legacy key -> field mapping:
 *   LBS_URL -> lbsUrl
 *   LBS_PWD -> lbsPassword
 *   (client id was hard-coded as <your_lbs_client_id>)
 */
public class LbsConfig {

    private String lbsUrl;
    private String lbsPassword;
    private String clientId = Constants.LbsConstants.DEFAULT_CLIENT_ID;
    private int defaultMaxLocAgeSeconds = Constants.LbsConstants.DEFAULT_MAX_LOC_AGE_SECONDS;
    private int connectTimeoutMs = Constants.LbsConstants.DEFAULT_CONNECT_TIMEOUT_MS;
    private int readTimeoutMs = Constants.LbsConstants.DEFAULT_READ_TIMEOUT_MS;
    private boolean enabled = true;

    public String getLbsUrl() {
        return lbsUrl;
    }

    public void setLbsUrl(String lbsUrl) {
        this.lbsUrl = lbsUrl;
    }

    public String getLbsPassword() {
        return lbsPassword;
    }

    public void setLbsPassword(String lbsPassword) {
        this.lbsPassword = lbsPassword;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public int getDefaultMaxLocAgeSeconds() {
        return defaultMaxLocAgeSeconds;
    }

    public void setDefaultMaxLocAgeSeconds(int defaultMaxLocAgeSeconds) {
        this.defaultMaxLocAgeSeconds = defaultMaxLocAgeSeconds;
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

    /** Checks the old jar never did - it just crashed instead. */
    public String validate() {
        if (isBlank(lbsUrl)) {
            return "LBS URL is not configured";
        }
        if (isBlank(lbsPassword)) {
            return "LBS password is not configured";
        }
        if (isBlank(clientId)) {
            return "LBS client id is not configured";
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /** Never log the password. */
    @Override
    public String toString() {
        return "LbsConfig{url=" + lbsUrl
                + ", clientId=" + clientId
                + ", maxLocAge=" + defaultMaxLocAgeSeconds
                + ", enabled=" + enabled + "}";
    }
}