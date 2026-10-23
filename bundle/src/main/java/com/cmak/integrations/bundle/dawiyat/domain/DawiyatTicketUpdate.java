package com.cmak.integrations.bundle.dawiyat.domain;

/**
 * The JSON body Dawiyat PUTs to the notification endpoint.
 *
 * Field names match the legacy contract EXACTLY - the old service read them
 * straight off a JSONObject, so renaming any of them breaks Dawiyat's caller:
 *
 *   { "u_trouble_ticket_number": "...", "state": "...",
 *     "comments": "...", "case_number": "..." }
 */
public class DawiyatTicketUpdate {

    private String u_trouble_ticket_number;
    private String state;
    private String comments;
    private String case_number;

    public String getU_trouble_ticket_number() {
        return u_trouble_ticket_number;
    }

    public void setU_trouble_ticket_number(String u_trouble_ticket_number) {
        this.u_trouble_ticket_number = u_trouble_ticket_number;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getCase_number() {
        return case_number;
    }

    public void setCase_number(String case_number) {
        this.case_number = case_number;
    }

    /**
     * The legacy service crashed with a JSONException when a key was missing
     * and returned a raw Java stack message. This returns a readable reason
     * instead, or null when the payload is usable.
     */
    public String validate() {
        if (isBlank(case_number)) {
            return "case_number is required";
        }
        if (isBlank(u_trouble_ticket_number)) {
            return "u_trouble_ticket_number is required";
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Rebuilt JSON for the Raw Request field.
     *
     * NOTE: this reflects the four fields we bind, not the literal bytes
     * Dawiyat sent. If they ever add fields we do not map, those will not
     * appear here - see the note in the handover doc.
     */
    public String toJson() {
        return "{"
                + "\"u_trouble_ticket_number\":" + quote(u_trouble_ticket_number) + ","
                + "\"state\":" + quote(state) + ","
                + "\"comments\":" + quote(comments) + ","
                + "\"case_number\":" + quote(case_number)
                + "}";
    }

    private static String quote(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder out = new StringBuilder(value.length() + 8).append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':  out.append("\\\""); break;
                case '\\': out.append("\\\\"); break;
                case '\n': out.append("\\n");  break;
                case '\r': out.append("\\r");  break;
                case '\t': out.append("\\t");  break;
                default:
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
            }
        }
        return out.append('"').toString();
    }

    @Override
    public String toString() {
        return "DawiyatTicketUpdate{ticket=" + u_trouble_ticket_number
                + ", case=" + case_number
                + ", state=" + state
                + ", commentsLength=" + (comments == null ? 0 : comments.length()) + "}";
    }
}