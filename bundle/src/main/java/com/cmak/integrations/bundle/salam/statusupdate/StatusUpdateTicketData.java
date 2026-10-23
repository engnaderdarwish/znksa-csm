package com.cmak.integrations.bundle.salam.statusupdate;


public class StatusUpdateTicketData {

    private String salamTicketNo;   // field 908302201
    private String status;          // field 922083102
    private String statusReason;    // field 922083103 (fetched, but not sent - matches legacy)
    private String statusNote;      // field 922101003
    private String assignedGroup;   // field 922101001
    private String assignee;        // field 922101002
    private String lastResolvedDate;// field 922181001 (fetched, but not sent - matches legacy)

    public String getSalamTicketNo() { return salamTicketNo; }
    public void setSalamTicketNo(String v) { this.salamTicketNo = v; }

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }

    public String getStatusReason() { return statusReason; }
    public void setStatusReason(String v) { this.statusReason = v; }

    public String getStatusNote() { return statusNote; }
    public void setStatusNote(String v) { this.statusNote = v; }

    public String getAssignedGroup() { return assignedGroup; }
    public void setAssignedGroup(String v) { this.assignedGroup = v; }

    public String getAssignee() { return assignee; }
    public void setAssignee(String v) { this.assignee = v; }

    public String getLastResolvedDate() { return lastResolvedDate; }
    public void setLastResolvedDate(String v) { this.lastResolvedDate = v; }

    public static String orEmpty(String v) {
        return v == null ? "" : v;
    }
}
