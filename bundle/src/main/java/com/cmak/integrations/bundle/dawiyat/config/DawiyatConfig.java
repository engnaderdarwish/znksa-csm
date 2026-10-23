package com.cmak.integrations.bundle.dawiyat.config;

/**
 * In-memory view of the DAWIYAT row of the centralized configuration record.
 *
 * Deliberately small. This component is INBOUND - Dawiyat calls us, we call
 * nothing - and authorization is handled by Helix through
 * @AccessControlledMethod(ValidUser), so there are no credentials and no URL
 * to store. The row exists for the on/off switch.
 */
public class DawiyatConfig {

    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return "DawiyatConfig{enabled=" + enabled + "}";
    }
}