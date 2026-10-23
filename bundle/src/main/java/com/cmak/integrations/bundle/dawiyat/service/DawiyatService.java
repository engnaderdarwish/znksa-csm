package com.cmak.integrations.bundle.dawiyat.service;

import com.bmc.arsys.rx.application.common.ServiceLocator;
import com.bmc.arsys.rx.services.common.Service;
import com.bmc.arsys.rx.services.record.RecordService;
import com.bmc.arsys.rx.services.record.domain.RecordInstance;
import com.cmak.integrations.bundle.config.Constants;
import com.cmak.integrations.bundle.dawiyat.config.DawiyatConfig;
import com.cmak.integrations.bundle.dawiyat.config.DawiyatConfigService;
import com.cmak.integrations.bundle.dawiyat.domain.DawiyatResponse;
import com.cmak.integrations.bundle.dawiyat.domain.DawiyatTicketUpdate;

/**
 * Innovation Studio replacement for ServiceLogicBuilder.ARSystemAPI.create().
 *
 * The legacy service opened its own ARServerUser connection on port 6000 using
 * credentials from a file on C:\ and wrote to INTG:FTTH-Dawiyat_StagingForm.
 * None of that is needed here - this code already runs inside Helix, so it
 * writes through RecordService directly.
 *
 * Legacy defects fixed on the way across:
 *   - static shared ARServerUser made the old service thread-unsafe; nothing
 *     is shared here, a fresh RecordInstance is built per request
 *   - the created record id was discarded; it is returned now
 *   - a missing JSON key threw and surfaced as a Java stack message;
 *     validation returns a readable reason instead
 *   - the {partner} path segment was accepted and ignored; it is stored
 */
public class DawiyatService implements Service {

    private final DawiyatConfigService configService;

    public DawiyatService(DawiyatConfigService configService) {
        this.configService = configService;
    }

    /**
     * Writes one Dawiyat ticket update into the staging record.
     *
     * @param partner the {partner} path segment
     * @param troubleTicketId the {troubleTicketId} path segment
     * @param update the parsed JSON body
     */
    public DawiyatResponse recordTicketUpdate(String partner,
                                              String troubleTicketId,
                                              DawiyatTicketUpdate update) {

        ServiceLocator.getLogger().info("[DAWIYAT] Update received - partner: " + partner
                + " | pathTicketId: " + troubleTicketId
                + " | body: " + update);

        // ---- 1. Is the integration switched on? ----
        DawiyatConfig config = configService.getConfig();
        if (!config.isEnabled()) {
            String message = "Dawiyat integration is disabled in the configuration record.";
            ServiceLocator.getLogger().warn("[DAWIYAT] " + message);
            return DawiyatResponse.error(message);
        }

        // ---- 2. Is the payload usable? ----
        if (update == null) {
            String message = "Request body is missing or is not valid JSON";
            ServiceLocator.getLogger().error("[DAWIYAT] " + message);
            return DawiyatResponse.badRequest(message);
        }

        String validationError = update.validate();
        if (validationError != null) {
            ServiceLocator.getLogger().error("[DAWIYAT] Rejected: " + validationError);
            return DawiyatResponse.badRequest(validationError);
        }

        // ---- 3. Which ticket id wins? ----
        // The id appears in BOTH the URL and the body. The legacy service read
        // only the body and ignored the URL. Keep that, but warn when they
        // disagree - it usually means the caller has a bug.
        String ticketNumber = update.getU_trouble_ticket_number();
        if (troubleTicketId != null && !troubleTicketId.trim().isEmpty()
                && !troubleTicketId.trim().equals(ticketNumber)) {
            ServiceLocator.getLogger().warn("[DAWIYAT] Ticket id mismatch - URL says ["
                    + troubleTicketId + "] but the body says [" + ticketNumber
                    + "]. Using the body value, as the legacy service did.");
        }

        // ---- 4. Write the staging row ----
        try {
            RecordService recordService = ServiceLocator.getRecordService();

            RecordInstance instance = recordService.buildRecordInstance(
                    Constants.DawiyatConstants.STAGING_RECORD_DEF);

            instance.setFieldValue(
                    Constants.DawiyatConstants.FIELD_ID_TROUBLE_TICKET_NUMBER, ticketNumber);
            instance.setFieldValue(
                    Constants.DawiyatConstants.FIELD_ID_STATE, update.getState());
            instance.setFieldValue(
                    Constants.DawiyatConstants.FIELD_ID_COMMENTS,
                    truncate(update.getComments(), Constants.DawiyatConstants.COMMENTS_LIMIT));
            instance.setFieldValue(
                    Constants.DawiyatConstants.FIELD_ID_CASE_NUMBER, update.getCase_number());
            instance.setFieldValue(
                    Constants.DawiyatConstants.FIELD_ID_PARTNER, partner);
            instance.setFieldValue(
                    Constants.DawiyatConstants.FIELD_ID_PROCESSING_STATUS,
                    Constants.DawiyatConstants.PROCESSING_STATUS_NEW);
            instance.setFieldValue(
                    Constants.DawiyatConstants.FIELD_ID_RAW_REQUEST,
                    truncate(update.toJson(), Constants.DawiyatConstants.RAW_REQUEST_LIMIT));

            recordService.createRecordInstance(instance);

            String stagingId = instance.getId();

            ServiceLocator.getLogger().info("[DAWIYAT] DONE - staging row created."
                    + " stagingId=" + stagingId
                    + " | case=" + update.getCase_number()
                    + " | ticket=" + ticketNumber
                    + " | partner=" + partner);

            return DawiyatResponse.ok(stagingId);

        } catch (Exception e) {
            ServiceLocator.getLogger().error("[DAWIYAT] FAILED to create the staging row in ["
                    + Constants.DawiyatConstants.STAGING_RECORD_DEF + "] for case ["
                    + update.getCase_number() + "]: " + e);
            return DawiyatResponse.error("Could not store the update: "
                    + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /** Field lengths are fixed; a longer value would fail the whole write. */
    private static String truncate(String value, int limit) {
        if (value == null || value.length() <= limit) {
            return value;
        }
        ServiceLocator.getLogger().warn("[DAWIYAT] Value of " + value.length()
                + " chars truncated to " + limit);
        return value.substring(0, limit);
    }
}