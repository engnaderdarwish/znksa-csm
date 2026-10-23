package com.cmak.integrations.bundle.lbs.domain;

/**
 * Returned by the Service Task. Replaces the single line the old jar printed.
 *
 * rawResponse is the important field while the integration is new: the old
 * project has no sample MLP reply anywhere, so the first successful call is
 * how you finally see the real format. Once the format is confirmed you can
 * stop mapping it into the process.
 */
public class LbsLocationResult {

    private boolean success;

    /** Decimal degrees, e.g. 24.751028 - ready for a map. */
    private String latitude;
    private String longitude;

    /** Exactly as MLP sent it, e.g. "24 45 03.7N" (degrees minutes seconds). */
    private String latitudeDms;
    private String longitudeDms;

    /** Accuracy radius in metres (semiMajor of the elliptical area). */
    private String accuracyMeters;

    /** Cell tower identity, useful for coverage complaints. */
    private String mcc;
    private String mnc;
    private String lac;
    private String cellId;

    /** The MSISDN echoed back by the service. */
    private String msid;

    private String legacyOutput;

    /** MLP resid: 1 SYSTEM FAILURE, 3 UNAUTHORIZED, 4 UNKNOWN SUBSCRIBER,
     *  5 ABSENT SUBSCRIBER, 6 POSITION METHOD FAILURE, 7 TIMEOUT. */
    private String resultCode;
    private String resultText;
    private String additionalInfo;

    private String errorMessage;
    private String rawResponse;

    public static LbsLocationResult located(String latitudeDms, String longitudeDms,
                                            String latitude, String longitude,
                                            String rawResponse) {
        LbsLocationResult r = new LbsLocationResult();
        r.success = true;
        r.latitudeDms = latitudeDms;
        r.longitudeDms = longitudeDms;
        r.latitude = latitude;
        r.longitude = longitude;
        // The old jar printed the RAW dms values, so keep those here for parity.
        r.legacyOutput = "<lat>" + latitudeDms + "</lat><lon>" + longitudeDms + "</lon>";
        r.rawResponse = rawResponse;
        return r;
    }

    /** Service answered, but would not give a position (phone off, absent, denied). */
    public static LbsLocationResult notLocated(String resultCode, String resultText,
                                               String additionalInfo, String rawResponse) {
        LbsLocationResult r = new LbsLocationResult();
        r.success = false;
        r.resultCode = resultCode;
        r.resultText = resultText;
        r.additionalInfo = additionalInfo;
        // The old jar printed the result text on this path.
        r.legacyOutput = resultText;
        r.errorMessage = "Location not available - " + resultText
                + " (resid " + resultCode + ")"
                + (additionalInfo == null ? "" : ", " + additionalInfo);
        r.rawResponse = rawResponse;
        return r;
    }

    public static LbsLocationResult failed(String errorMessage, String rawResponse) {
        LbsLocationResult r = new LbsLocationResult();
        r.success = false;
        r.errorMessage = errorMessage;
        r.rawResponse = rawResponse;
        return r;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    /** The exact string the old jar printed, for a like-for-like swap. */
    public String getLegacyOutput() {
        return legacyOutput;
    }

    public void setLegacyOutput(String legacyOutput) {
        this.legacyOutput = legacyOutput;
    }

    public String getResultCode() {
        return resultCode;
    }

    public void setResultCode(String resultCode) {
        this.resultCode = resultCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public void setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
    }

    public String getLatitudeDms() {
        return latitudeDms;
    }

    public void setLatitudeDms(String latitudeDms) {
        this.latitudeDms = latitudeDms;
    }

    public String getLongitudeDms() {
        return longitudeDms;
    }

    public void setLongitudeDms(String longitudeDms) {
        this.longitudeDms = longitudeDms;
    }

    public String getAccuracyMeters() {
        return accuracyMeters;
    }

    public void setAccuracyMeters(String accuracyMeters) {
        this.accuracyMeters = accuracyMeters;
    }

    public String getMcc() {
        return mcc;
    }

    public void setMcc(String mcc) {
        this.mcc = mcc;
    }

    public String getMnc() {
        return mnc;
    }

    public void setMnc(String mnc) {
        this.mnc = mnc;
    }

    public String getLac() {
        return lac;
    }

    public void setLac(String lac) {
        this.lac = lac;
    }

    public String getCellId() {
        return cellId;
    }

    public void setCellId(String cellId) {
        this.cellId = cellId;
    }

    public String getMsid() {
        return msid;
    }

    public void setMsid(String msid) {
        this.msid = msid;
    }

    public String getResultText() {
        return resultText;
    }

    public void setResultText(String resultText) {
        this.resultText = resultText;
    }

    public String getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(String additionalInfo) {
        this.additionalInfo = additionalInfo;
    }
}