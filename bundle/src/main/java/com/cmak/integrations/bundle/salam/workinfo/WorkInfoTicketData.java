package com.cmak.integrations.bundle.salam.workinfo;


public class WorkInfoTicketData {

    private String secureWorkLog;       // field 908302202
    private String viewAccess;          // field 908302203
    private String communicationSource; // field 922092801
    private String workLogType;         // field 908302204
    private String incidentNumber;      // field 908302201 (SalamIncidentNumber)
    private String workLogSubmitter;    // field 908302205
    private String detailedDescription; // field 922083007
    private String description;         // field 922092802

    public String getSecureWorkLog() { return secureWorkLog; }
    public void setSecureWorkLog(String v) { this.secureWorkLog = v; }

    public String getViewAccess() { return viewAccess; }
    public void setViewAccess(String v) { this.viewAccess = v; }

    public String getCommunicationSource() { return communicationSource; }
    public void setCommunicationSource(String v) { this.communicationSource = v; }

    public String getWorkLogType() { return workLogType; }
    public void setWorkLogType(String v) { this.workLogType = v; }

    public String getIncidentNumber() { return incidentNumber; }
    public void setIncidentNumber(String v) { this.incidentNumber = v; }

    public String getWorkLogSubmitter() { return workLogSubmitter; }
    public void setWorkLogSubmitter(String v) { this.workLogSubmitter = v; }

    public String getDetailedDescription() { return detailedDescription; }
    public void setDetailedDescription(String v) { this.detailedDescription = v; }

    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }


    public static String orEmpty(String v) {
        return v == null ? "" : v;
    }
}
