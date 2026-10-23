package com.cmak.integrations.bundle.sms.domain;
import com.cmak.integrations.bundle.config.Constants;


/**
 * Replaces the if (lang == "English") / else if (lang == "Arabic") chain
 * in the legacy main(). The SOAP service still receives the original
 * language string ("English" / "Arabic") in SoapUser.Language, so the
 * wire value is kept separate from the message type.
 */
public enum SmsLanguage {

    ENGLISH("English", Constants.SmsConstants.MSG_TYPE_LATIN),
    ARABIC("Arabic", Constants.SmsConstants.MSG_TYPE_ARABIC);

    private final String soapLanguage;
    private final String messageType;

    SmsLanguage(String soapLanguage, String messageType) {
        this.soapLanguage = soapLanguage;
        this.messageType = messageType;
    }

    /** Value sent in SoapUser.Language — identical to the legacy behaviour. */
    public String getSoapLanguage() {
        return soapLanguage;
    }

    /** Value sent as the SendSms messageType element. */
    public String getMessageType() {
        return messageType;
    }

    /**
     * @return the matching language, or {@code null} when unsupported.
     *         The legacy code logged an error and returned "2" in that case.
     */
    public static SmsLanguage fromString(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        for (SmsLanguage language : values()) {
            if (language.soapLanguage.equalsIgnoreCase(trimmed)) {
                return language;
            }
        }
        return null;
    }
}