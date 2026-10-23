package com.cmak.integrations.bundle.salam.resolve;


public class ResolveTicketData {

    private String salamTicketNo;      // field 908302201
    private String zainTicketNo;       // field 536870980
    private String status;             // field 922083102
    private String statusReason;       // field 922083103
    private String statusNote;         // field 922101003
    private String resolutionNote;     // field 922083104
    private String assignedGroup;      // field 922101001
    private String assignee;           // field 922101002
    private String lastResolvedDate;   // field 922181001
    private String supportCompany;     // field 922122001
    private String supportOrganization;// field 922122002
    private String supportGroupId;     // field 922122003

    public String getSalamTicketNo() { return salamTicketNo; }
    public void setSalamTicketNo(String v) { this.salamTicketNo = v; }

    public String getZainTicketNo() { return zainTicketNo; }
    public void setZainTicketNo(String v) { this.zainTicketNo = v; }

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }

    public String getStatusReason() { return statusReason; }
    public void setStatusReason(String v) { this.statusReason = v; }

    public String getStatusNote() { return statusNote; }
    public void setStatusNote(String v) { this.statusNote = v; }

    public String getResolutionNote() { return resolutionNote; }
    public void setResolutionNote(String v) { this.resolutionNote = v; }

    public String getAssignedGroup() { return assignedGroup; }
    public void setAssignedGroup(String v) { this.assignedGroup = v; }

    public String getAssignee() { return assignee; }
    public void setAssignee(String v) { this.assignee = v; }

    public String getLastResolvedDate() { return lastResolvedDate; }
    public void setLastResolvedDate(String v) { this.lastResolvedDate = v; }

    public String getSupportCompany() { return supportCompany; }
    public void setSupportCompany(String v) { this.supportCompany = v; }

    public String getSupportOrganization() { return supportOrganization; }
    public void setSupportOrganization(String v) { this.supportOrganization = v; }

    public String getSupportGroupId() { return supportGroupId; }
    public void setSupportGroupId(String v) { this.supportGroupId = v; }


    public static String orEmpty(String v) {
        return v == null ? "" : v;
    }
}
