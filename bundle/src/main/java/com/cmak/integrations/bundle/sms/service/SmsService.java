package com.cmak.integrations.bundle.sms.service;

import javax.validation.constraints.NotBlank;

import com.bmc.arsys.rx.services.action.domain.Action;
import com.bmc.arsys.rx.services.action.domain.ActionParameter;



import com.bmc.arsys.rx.services.common.Service;
import com.bmc.arsys.rx.services.common.domain.Scope;
import com.bmc.arsys.rx.application.common.ServiceLocator;
import com.cmak.integrations.bundle.config.Constants;
import com.cmak.integrations.bundle.sms.config.SmsConfig;
import com.cmak.integrations.bundle.sms.config.SmsConfigService;
import com.cmak.integrations.bundle.sms.domain.SmsLanguage;
import com.cmak.integrations.bundle.sms.domain.SmsSendResult;
import com.cmak.integrations.bundle.sms.client.PmmSoapClient;
import com.cmak.integrations.bundle.sms.client.SoapCallResult;


/**
 * Innovation Studio replacement for ZainKsaSMSWebService.main(String[]).
 *
 * The four command-line arguments become four @ActionParameter values, and
 * the "1" / "2" printed to stdout becomes a typed return object. Drop the
 * Service Task onto a process and map the parameters from case fields.
 *
 * Argument mapping (old -> new):
 *   args[0] phone         -> recipientPhone
 *   args[1] smsData       -> messageText
 *   args[2] lang          -> language
 *   args[3] smsSenderName -> senderName
 */
public class SmsService implements Service {

    private final SmsConfigService configService;

    public SmsService(SmsConfigService configService) {
        this.configService = configService;
    }

    @Action(name = "sendZainSms",
            scope = Scope.PUBLIC,
            displayName = "Send Zain KSA SMS")
    public SmsSendResult sendZainSms(
            @ActionParameter(name = "recipientPhone") @NotBlank String recipientPhone,
            @ActionParameter(name = "messageText") @NotBlank String messageText,
            @ActionParameter(name = "language") @NotBlank String language,
            @ActionParameter(name = "senderName") String senderName) {

        ServiceLocator.getLogger().info("sendZainSms invoked - Phone: "+ recipientPhone +" | Language: "+language+" | SenderName: "+senderName);

        // ---- 1. Language check (was the trailing else in the legacy main) ----
        SmsLanguage smsLanguage = SmsLanguage.fromString(language);
        if (smsLanguage == null) {
            String message = "Unsupported language [" + language + "]. Expected 'English' or 'Arabic'.";
            ServiceLocator.getLogger().error(message +" SMS not sent.");
            return SmsSendResult.failed(message);
        }

        // ---- 2. Configuration (was the .properties load + decrypt) ----
        SmsConfig config = configService.getConfig();

        if (!config.isEnabled()) {
            String message = "SMS integration is disabled in the configuration record.";
            ServiceLocator.getLogger().warn(message);
            return SmsSendResult.failed(message);
        }

        String configError = config.validate();
        if (configError != null) {
            ServiceLocator.getLogger().error("SMS configuration invalid: "+ configError);
            return SmsSendResult.failed(configError);
        }

        // ---- 3. Message body: #INTG# means new line ----
        String smsText = messageText == null
                ? ""
                : messageText.replace(Constants.SmsConstants.LINE_BREAK_TOKEN, "\n");

        // ---- 4. Originator resolution (identical rule to the legacy code) ----
        String originator = resolveOriginator(senderName, config.getDefaultOriginator());

        PmmSoapClient client = new PmmSoapClient(config);

        // ---- 5. Authenticate; abort if Result != OK ----
        SoapCallResult auth = client.authenticate(smsLanguage.getSoapLanguage());
        if (!auth.isTransportOk()) {
            return SmsSendResult.failed("Authentication call failed: " + auth.getErrorMessage());
        }
        if (!auth.isResultOk()) {
            String message = "Authentication FAILED - Result: " + auth.getResult()
                    + " | User: " + config.getSmsUser();
            ServiceLocator.getLogger().error(message);
            return SmsSendResult.failed(message, auth.getResult(), null);
        }

        // ---- 6. Send ----
        SoapCallResult send = client.sendSms(
                smsLanguage.getSoapLanguage(),
                originator,
                smsText,
                recipientPhone,
                smsLanguage.getMessageType());

        if (!send.isTransportOk()) {
            return SmsSendResult.failed("Send call failed: " + send.getErrorMessage());
        }

        if (send.isResultOk()) {
            ServiceLocator.getLogger().info("SMS delivered - TransactionID: "+send.getTransactionId() +" | Phone: "+ recipientPhone);
            return SmsSendResult.ok(send.getTransactionId(), send.getResult());
        }

        String message = "SMS delivery FAILED - Result: " + send.getResult()
                + " | TransactionID: " + send.getTransactionId()
                + " | Phone: " + recipientPhone;
        ServiceLocator.getLogger().error(message);
        return SmsSendResult.failed(message, send.getResult(), send.getTransactionId());
    }


    /**
     * Checks the whole setup WITHOUT sending an SMS and WITHOUT any cost.
     *
     * A successful result proves all of this at once:
     *   - the bundle loaded correctly in OSGi
     *   - the configuration record was found by its GUID
     *   - the field ids are correct
     *   - the SMS_WS_URL is reachable from the container
     *   - the SOAP namespace and operation name are correct
     *   - the user name and password are accepted by Zain
     *
     * It does NOT prove the SendSms element names, and it does not prove
     * that a message can actually be delivered to a phone.
     *
     * Run this after every environment migration.
     */
//    @Action(name = "testSmsConnection",
//            scope = Scope.PUBLIC,
//            displayName = "Test Zain SMS Connection")
//    public SmsSendResult testSmsConnection(
//            @ActionParameter(name = "language") String language) {
//
//        SmsLanguage smsLanguage = SmsLanguage.fromString(language);
//        if (smsLanguage == null) {
//            smsLanguage = SmsLanguage.ENGLISH;
//        }
//
//        SmsConfig config = configService.getConfig();
//
//        String configError = config.validate();
//        if (configError != null) {
//            ServiceLocator.getLogger().error("Configuration is incomplete: "+ configError);
//            return SmsSendResult.failed(configError);
//        }
//
//        SoapCallResult auth = new PmmSoapClient(config)
//                .authenticate(smsLanguage.getSoapLanguage());
//
//        if (!auth.isTransportOk()) {
//            ServiceLocator.getLogger().error("Cannot reach the SMS service: "+ auth.getErrorMessage());
//            return SmsSendResult.failed("Cannot reach the SMS service: " + auth.getErrorMessage());
//        }
//
//        if (!auth.isResultOk()) {
//            ServiceLocator.getLogger().error("Reached the service, authentication rejected: "+ auth.getResult());
//            return SmsSendResult.failed(
//                    "Reached the service, authentication rejected: " + auth.getResult(),
//                    auth.getResult(), null);
//        }
//
//        ServiceLocator.getLogger().info("Connection test PASSED - service replied: "+ auth.getResult());
//        return SmsSendResult.ok(null, auth.getResult());
//    }

    /**
     * Legacy rule, unchanged: the caller may only override the originator with
     * "Zain" or "1555"; anything else falls back to IT_SMS_SERVICENAME.
     */
    private String resolveOriginator(String senderName, String configuredOriginator) {
        if (senderName != null && !senderName.trim().isEmpty()
                && (Constants.SmsConstants.SENDER_ZAIN.equalsIgnoreCase(senderName.trim())
                || Constants.SmsConstants.SENDER_1555.equalsIgnoreCase(senderName.trim()))) {
            ServiceLocator.getLogger().info("Originator overridden by senderName: "+senderName.trim());
            return senderName.trim();
        }
        ServiceLocator.getLogger().info("Using originator from configuration: "+ configuredOriginator);
        return configuredOriginator;
    }
}