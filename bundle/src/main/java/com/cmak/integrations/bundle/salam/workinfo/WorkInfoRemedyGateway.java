package com.cmak.integrations.bundle.salam.workinfo;

import com.bmc.arsys.api.ARServerUser;
import com.bmc.arsys.api.Entry;
import com.bmc.arsys.api.EntryListInfo;
import com.bmc.arsys.api.QualifierInfo;

import java.util.List;


class WorkInfoRemedyGateway {

    private static final String STAGING_FORM = "INTG:CS:Generic-StagingForm";
    private static final String GUID_QUALIFIER = "'INTG_SALAMGUID' = \"%s\"";

    private static final int FIELD_SECURE_WORKLOG = 908302202;
    private static final int FIELD_VIEW_ACCESS = 908302203;
    private static final int FIELD_COMM_SOURCE = 922092801;
    private static final int FIELD_WORKLOG_TYPE = 908302204;
    private static final int FIELD_INCIDENT_NUMBER = 908302201;
    private static final int FIELD_WORKLOG_SUBMITTER = 908302205;
    private static final int FIELD_DETAILED_DESCRIPTION = 922083007;
    private static final int FIELD_DESCRIPTION = 922092802;

    WorkInfoTicketData fetchByGuid(String salamGuid, WorkInfoConfigProvider config) {
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

            WorkInfoTicketData data = new WorkInfoTicketData();
            data.setSecureWorkLog(str(record, FIELD_SECURE_WORKLOG));
            data.setViewAccess(str(record, FIELD_VIEW_ACCESS));
            data.setCommunicationSource(str(record, FIELD_COMM_SOURCE));
            data.setWorkLogType(str(record, FIELD_WORKLOG_TYPE));
            data.setIncidentNumber(str(record, FIELD_INCIDENT_NUMBER));
            data.setWorkLogSubmitter(str(record, FIELD_WORKLOG_SUBMITTER));
            data.setDetailedDescription(str(record, FIELD_DETAILED_DESCRIPTION));
            data.setDescription(str(record, FIELD_DESCRIPTION));
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
