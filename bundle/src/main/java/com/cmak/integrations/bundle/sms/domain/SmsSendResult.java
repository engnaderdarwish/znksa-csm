package com.cmak.integrations.bundle.sms.domain;


import com.cmak.integrations.bundle.config.Constants;


/**
 * Returned by the Service Task so the process can branch on the outcome
 * instead of parsing "1" / "2" off stdout like the old filter did.
 */
public class SmsSendResult {

    private boolean success;
    private String statusCode;
    private String transactionId;
    private String serviceResult;
    private String errorMessage;

    public static SmsSendResult ok(String transactionId, String serviceResult) {
        SmsSendResult result = new SmsSendResult();
        result.success = true;
        result.statusCode = Constants.SmsConstants.LEGACY_STATUS_SUCCESS;
        result.transactionId = transactionId;
        result.serviceResult = serviceResult;
        return result;
    }

    public static SmsSendResult failed(String errorMessage) {
        return failed(errorMessage, null, null);
    }

    public static SmsSendResult failed(String errorMessage, String serviceResult, String transactionId) {
        SmsSendResult result = new SmsSendResult();
        result.success = false;
        result.statusCode = Constants.SmsConstants.LEGACY_STATUS_FAILURE;
        result.errorMessage = errorMessage;
        result.serviceResult = serviceResult;
        result.transactionId = transactionId;
        return result;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    /** "1" or "2" — same contract the AR filter used to read from stdout. */
    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getServiceResult() {
        return serviceResult;
    }

    public void setServiceResult(String serviceResult) {
        this.serviceResult = serviceResult;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}