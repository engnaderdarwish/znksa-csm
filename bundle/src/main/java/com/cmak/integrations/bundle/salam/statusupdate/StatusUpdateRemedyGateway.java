package com.cmak.integrations.bundle.salam.statusupdate;

import com.bmc.arsys.api.ARServerUser;
import com.bmc.arsys.api.Entry;
import com.bmc.arsys.api.EntryListInfo;
import com.bmc.arsys.api.QualifierInfo;

import java.util.List;


class StatusUpdateRemedyGateway {

    private static final String STAGING_FORM = "INTG:CS:Generic-StagingForm";
    private static final String GUID_QUALIFIER = "'INTG_SALAMGUID' = \"%s\"";

    private static final int FIELD_SALAM_TT_NO = 908302201;
    private static final int FIELD_STATUS = 922083102;
    private static final int FIELD_STATUS_REASON = 922083103;
    private static final int FIELD_STATUS_NOTE = 922101003;
    private static final int FIELD_ASSIGNED_GROUP = 922101001;
    private static final int FIELD_ASSIGNEE = 922101002;
    private static final int FIELD_LAST_RESOLVED_DATE = 922181001;

    StatusUpdateTicketData fetchByGuid(String salamGuid, StatusUpdateConfigProvider config) {
        ARServerUser ct = new ARServerUser();
        try {
            ct.setServer(config.getArServer());
            ct.setUser(config.getArUser());
            ct.setPassword(config.getArPassword());
            ct.setPort(config.getArPort());
            ct.verifyUser();

            String queryString = String.format(GUID_QUALIFIER, salamGuid);
            QualifierInfo qual = ct.parseQualification(STAGING_FORM, queryString);
            List<EntryListInfo> entries = ct.getListEntry(STAGING_FORM, qual, 0, 0, null, null, false, null);

            if (entries.isEmpty()) {
                throw new IllegalStateException("No staging record found for SALAM GUID " + salamGuid);
            }
            if (entries.size() > 1) {

                throw new IllegalStateException("Multiple staging records found for SALAM GUID " + salamGuid);
            }

            Entry record = ct.getEntry(STAGING_FORM, entries.get(0).getEntryID(), null);

            StatusUpdateTicketData data = new StatusUpdateTicketData();
            data.setSalamTicketNo(str(record, FIELD_SALAM_TT_NO));
            data.setStatus(str(record, FIELD_STATUS));
            data.setStatusReason(str(record, FIELD_STATUS_REASON));
            data.setStatusNote(str(record, FIELD_STATUS_NOTE));
            data.setAssignedGroup(str(record, FIELD_ASSIGNED_GROUP));
            data.setAssignee(str(record, FIELD_ASSIGNEE));
            data.setLastResolvedDate(str(record, FIELD_LAST_RESOLVED_DATE));
            return data;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to query staging form for GUID " + salamGuid + ": " + e.getMessage(), e);
        } finally {
            try { ct.logout(); } catch (Exception ignored) { }
        }
    }

    private String str(Entry record, int fieldId) {
        Object v = record.get(fieldId);
        return v == null ? null : v.toString();
    }
}
