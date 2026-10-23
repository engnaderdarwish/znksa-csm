package com.cmak.integrations.bundle.dawiyat.domain;

import com.cmak.integrations.bundle.config.Constants;

/**
 * The JSON body returned to Dawiyat.
 *
 * The two legacy property names errDisc / errCode are KEPT so Dawiyat's
 * existing parser keeps working. Only the misspelling inside the message
 * is corrected ("Update Succesfull" -> "Update Successful").
 *
 * stagingId is new. The legacy service created the staging record and then
 * threw the id away, so nobody could tie a request to a row afterwards.
 * Adding a property is backwards compatible - an existing parser ignores it.
 */
public class DawiyatResponse {

    private String errDisc;
    private String errCode;
    private String stagingId;

    public DawiyatResponse() {
    }

    private DawiyatResponse(String errDisc, String errCode, String stagingId) {
        this.errDisc = errDisc;
        this.errCode = errCode;
        this.stagingId = stagingId;
    }

    public static DawiyatResponse ok(String stagingId) {
        return new DawiyatResponse(
                Constants.DawiyatConstants.RESPONSE_SUCCESS_MESSAGE,
                Constants.DawiyatConstants.RESPONSE_CODE_SUCCESS,
                stagingId);
    }

    /** Malformed or incomplete payload - the caller can fix this. */
    public static DawiyatResponse badRequest(String reason) {
        return new DawiyatResponse(reason,
                Constants.DawiyatConstants.RESPONSE_CODE_BAD_REQUEST, null);
    }

    /** Our side failed - the caller should retry. */
    public static DawiyatResponse error(String reason) {
        return new DawiyatResponse(reason,
                Constants.DawiyatConstants.RESPONSE_CODE_ERROR, null);
    }

    public String getErrDisc() {
        return errDisc;
    }

    public void setErrDisc(String errDisc) {
        this.errDisc = errDisc;
    }

    public String getErrCode() {
        return errCode;
    }

    public void setErrCode(String errCode) {
        this.errCode = errCode;
    }

    /** Record instance id of the staging row, null when nothing was created. */
    public String getStagingId() {
        return stagingId;
    }

    public void setStagingId(String stagingId) {
        this.stagingId = stagingId;
    }
}