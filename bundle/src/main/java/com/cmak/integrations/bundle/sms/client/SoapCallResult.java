package com.cmak.integrations.bundle.sms.client;


/**
 * Flattened view of AuthResult / SendResult from the old generated stubs.
 */
public class SoapCallResult {

    private final boolean transportOk;
    private final String result;
    private final String transactionId;
    private final String errorMessage;

    private SoapCallResult(boolean transportOk, String result, String transactionId, String errorMessage) {
        this.transportOk = transportOk;
        this.result = result;
        this.transactionId = transactionId;
        this.errorMessage = errorMessage;
    }

    public static SoapCallResult success(String result, String transactionId) {
        return new SoapCallResult(true, result, transactionId, null);
    }

    public static SoapCallResult failure(String errorMessage) {
        return new SoapCallResult(false, null, null, errorMessage);
    }

    /** True when the HTTP/SOAP round trip completed — says nothing about Result. */
    public boolean isTransportOk() {
        return transportOk;
    }

    /** The service-level Result element, typically "OK". */
    public String getResult() {
        return result;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isResultOk() {
        return transportOk && result != null && "OK".equalsIgnoreCase(result.trim());
    }
}