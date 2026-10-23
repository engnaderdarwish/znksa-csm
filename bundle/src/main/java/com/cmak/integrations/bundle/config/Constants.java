package com.cmak.integrations.bundle.config;

/**
 * Every constant for every integration in one place.
 *
 * Top level = the centralized configuration record shared by all integrations.
 * The nested classes hold values specific to one component and are referred to
 * as Constants.SmsConstants.X, Constants.LbsConstants.X, etc.
 */
public final class Constants {

    private Constants() {
    }

    /**
     * Note this lives in com.cmak.csm, NOT in the znksa-csm-sms bundle -
     * it is shared by all integrations.
     */
    public static final String CONFIG_RECORD_DEF =
            "com.cmak.csm:CS KSAZN INTG - Centralized Configuration Form";

    /* ------------------------------------------------------------------
     * Integration Name values - the key each integration is looked up by
     * ------------------------------------------------------------------ */

    public static final String INTEGRATION_LBS     = "LBS";
    public static final String INTEGRATION_SMS     = "SMS";
    public static final String INTEGRATION_DAWIYAT = "DAWIYAT";

    /* ------------------------------------------------------------------
     * Field ids - CONFIRMED from the deployed record definition
     * ------------------------------------------------------------------ */

    public static final int FIELD_ID_INSTANCE_ID        = 379;
    public static final int FIELD_ID_CUSTOMER_ID        = 536870913;   // Text, parsed as int
    public static final int FIELD_ID_PASSWORD           = 536870914;
    public static final int FIELD_ID_HOSTNAME_IP        = 536870915;
    public static final int FIELD_ID_CLIENT_ID          = 536870916;
    public static final int FIELD_ID_CLIENT_SECRET      = 536870917;
    public static final int FIELD_ID_USERNAME           = 536870918;
    public static final int FIELD_ID_INTEGRATION_NAME   = 536870919;
    public static final int FIELD_ID_READ_TIMEOUT       = 536870920;
    public static final int FIELD_ID_CONNECTION_TIMEOUT = 536870921;
    public static final int FIELD_ID_MAX_LOCATION_AGE   = 536870922;
    public static final int FIELD_ID_INTEGRATION_STATUS = 536870923;
    public static final int FIELD_ID_DEFAULT_ORIGINATOR = 536870928;
    public static final int FIELD_ID_SOAP_NAMESPACE     = 536870929;

    /** A blank Integration Status counts as ENABLED - do not block on empty. */
    public static final String[] DISABLED_STATUS_VALUES = {
            "Inactive", "Disabled", "Off", "Stopped"
    };

    /** How long a loaded configuration is reused before re-reading. */
    public static final long CACHE_TTL_MS = 5L * 60L * 1000L;

    /* ------------------------------------------------------------------
     * Build marker - BUMP THIS EVERY TIME YOU DEPLOY
     * ------------------------------------------------------------------
     * Printed at startup. If the log shows an old value, the container is
     * still running the previous jar and your change is not live.
     */
    public static final String BUILD_VERSION = "1.1.0";
    public static final String BUILD_DATE    = "2026-08-16";

    /* ==================================================================
     * SMS integration (Zain KSA, SOAP) - OUTBOUND
     * ================================================================== */
    public static final class SmsConstants {

        private SmsConstants() {
        }

        /**
         * Fallback only. The Customer ID now lives in the configuration
         * record; this is what is used when that field is left blank.
         * The legacy jar hard-coded 128.
         */
        public static final int DEFAULT_CUSTOMER_ID = 0;

        /** Token the AR filter puts in the message body to mean "new line". */
        public static final String LINE_BREAK_TOKEN = "#INTG#";

        /** Sender names allowed to override the configured originator. */
        public static final String SENDER_ZAIN = "Zain";
        public static final String SENDER_1555 = "1555";

        /** The old JAR printed these to stdout for the filter to read back. */
        public static final String LEGACY_STATUS_SUCCESS = "1";
        public static final String LEGACY_STATUS_FAILURE = "2";

        public static final String SOAP_RESULT_OK = "OK";

        /* CONFIRMED against the Messenger.asmx WSDL. No "www." */
        public static final String DEFAULT_SOAP_NAMESPACE = "http://pmmsoapmessenger.com/";

        public static final String OP_AUTHENTICATE = "Authenticate";
        public static final String OP_SEND_SMS     = "SendSms";

        /** MessageType.LATIN in the old enum. */
        public static final String MSG_TYPE_LATIN = "Latin";

        /** MessageType.ARABIC_WITH_ARABIC_NUMBERS in the old enum. */
        public static final String MSG_TYPE_ARABIC = "ArabicWithArabicNumbers";

        public static final int DEFAULT_CONNECT_TIMEOUT_MS = 15000;
        public static final int DEFAULT_READ_TIMEOUT_MS    = 10000;
    }

    /* ==================================================================
     * LBS integration (location, MLP over plain HTTP) - OUTBOUND
     * ================================================================== */
    public static final class LbsConstants {

        private LbsConstants() {
        }

        /** Hard-coded in the old code: <id><your_lbs_client_id></id> */
        public static final String DEFAULT_CLIENT_ID = "your_lbs_client_id";

        /** MLP protocol version used in both hdr and slir. */
        public static final String MLP_VERSION = "3.2.0";

        /** The old code prepended this to args[0]. */
        public static final String KSA_COUNTRY_CODE = "966";

        public static final String MLP_DOCTYPE =
                "<!DOCTYPE svc_init SYSTEM \"MLP_SVC_INIT_320.DTD\">";

        /** Production logs show 3600 on every call. */
        public static final int DEFAULT_MAX_LOC_AGE_SECONDS = 3600;
        public static final int DEFAULT_CONNECT_TIMEOUT_MS  = 15000;
        public static final int DEFAULT_READ_TIMEOUT_MS     = 10000;

        /** How much of the raw reply to keep for diagnosis. */
        public static final int RAW_RESPONSE_LIMIT = 2000;
    }

    /* ==================================================================
     * Dawiyat FTTH integration - INBOUND
     * ==================================================================
     * The only inbound component: Dawiyat PUTs ticket updates to us and we
     * store them in a staging record for downstream workflow to process.
     */
    public static final class DawiyatConstants {

        private DawiyatConstants() {
        }

        /**
         * Staging record definition.
         * >>> CONFIRM the application prefix once the record is created. <<<
         * Assumed to sit alongside the centralized configuration form.
         */
        public static final String STAGING_RECORD_DEF =
                "com.cmak.csm:CS KSAZN INTG - FTTH - Dawiyat - Staging Form";

        /*
         * Field ids - RESERVED, not yet verified against a deployed record.
         * A new record definition numbers its custom fields from 536870913 in
         * creation order, so these hold only if the fields were created in the
         * order listed in STAGING-RECORD-SPEC.md. Verify and correct once the
         * record exists.
         */
        public static final int FIELD_ID_TROUBLE_TICKET_NUMBER = 536870939;
        public static final int FIELD_ID_STATE                 = 536870924;
        public static final int FIELD_ID_COMMENTS              = 536870928;
        public static final int FIELD_ID_CASE_NUMBER           = 536870913;
        public static final int FIELD_ID_PARTNER               = 536870914;
        public static final int FIELD_ID_PROCESSING_STATUS     = 536870952;
        public static final int FIELD_ID_ERROR_MESSAGE         = 536870934;
        public static final int FIELD_ID_RAW_REQUEST           = 536870953;

        /** Processing Status selection values. 0 = New, 1 = Processed, 2 = Failed. */
        public static final String PROCESSING_STATUS_NEW       = "0";
        public static final String PROCESSING_STATUS_PROCESSED = "1";
        public static final String PROCESSING_STATUS_FAILED    = "2";

        /** Field lengths from the spec - longer values are truncated, not rejected. */
        public static final int COMMENTS_LIMIT    = 4000;
        public static final int RAW_REQUEST_LIMIT = 4000;

        /**
         * Response body values. The legacy service spelled this
         * "Update Succesfull" - corrected here, as agreed.
         */
        public static final String RESPONSE_SUCCESS_MESSAGE = "Update Successful";
        public static final String RESPONSE_CODE_SUCCESS     = "200";
        public static final String RESPONSE_CODE_BAD_REQUEST = "400";
        public static final String RESPONSE_CODE_ERROR       = "500";

        /**
         * A RestfulResource cannot take constructor arguments, so the resource
         * looks its service up by class name. Keep this in step with any
         * rename or move of DawiyatService.
         */
        public static final String SERVICE_CLASS_NAME =
                "com.cmak.integrations.bundle.dawiyat.service.DawiyatService";
    }
}

