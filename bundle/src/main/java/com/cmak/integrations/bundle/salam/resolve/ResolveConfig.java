package com.cmak.integrations.bundle.salam.resolve;

import com.bmc.arsys.rx.application.common.ServiceLocator;
import com.bmc.arsys.rx.services.record.RecordService;
import com.bmc.arsys.rx.services.record.domain.RecordInstance;


interface ResolveConfigProvider {
    String getArServer();
    String getArUser();
    String getArPassword();
    int getArPort();

    String getSalamUser();
    String getSalamPassword();
    String getSalamCertPath();

    String getStatusResolveApiUrl();
}

class ResolveConfigProviderImpl implements ResolveConfigProvider {


    private static final String SETTINGS_RECORD_DEFINITION = "com.cmak.csm:CS KSAZN INTG - Centralized Configuration Form";

    // GUID of the single SALAM settings row.
    private static final String SETTINGS_INSTANCE_ID = "AGGC6BBKIWVLYATKZTGHTKZTGH2EGQ";

    private static final int FIELD_AR_SERVER = 536870931;
    private static final int FIELD_AR_USER = 536870932;
    private static final int FIELD_AR_PASSWORD = 536870933;
    private static final int FIELD_AR_PORT = 536870934;
    private static final int FIELD_SALAM_USER = 536870935;
    private static final int FIELD_SALAM_PASSWORD = 536870936;
    private static final int FIELD_SALAM_CERT_PATH = 536870937;
    private static final int FIELD_STATUS_RESOLVE_API_URL = 536870940;

    @Override public String getArServer() { return fetchField(FIELD_AR_SERVER); }
    @Override public String getArUser() { return fetchField(FIELD_AR_USER); }
    @Override public String getArPassword() { return fetchField(FIELD_AR_PASSWORD); }

    @Override
    public int getArPort() {
        String value = fetchField(FIELD_AR_PORT);
        return value == null || value.isBlank() ? 6000 : Integer.parseInt(value);
    }

    @Override public String getSalamUser() { return fetchField(FIELD_SALAM_USER); }
    @Override public String getSalamPassword() { return fetchField(FIELD_SALAM_PASSWORD); }
    @Override public String getSalamCertPath() { return fetchField(FIELD_SALAM_CERT_PATH); }
    @Override public String getStatusResolveApiUrl() { return fetchField(FIELD_STATUS_RESOLVE_API_URL); }

    private String fetchField(int fieldId) {
        RecordService recordService = ServiceLocator.getRecordService();
        RecordInstance settings = recordService.getRecordInstance(SETTINGS_RECORD_DEFINITION, SETTINGS_INSTANCE_ID);
        return settings.getFieldValue(fieldId);
    }
}
