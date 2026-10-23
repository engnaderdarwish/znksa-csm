package com.cmak.integrations.bundle.salam.statusupdate;

import com.bmc.arsys.rx.services.action.domain.Action;
import com.bmc.arsys.rx.services.action.domain.ActionParameter;
import com.bmc.arsys.rx.services.common.Service;
import com.bmc.arsys.rx.services.common.domain.Scope;
import org.hibernate.validator.constraints.NotBlank;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static com.cmak.integrations.bundle.salam.statusupdate.StatusUpdateTicketData.orEmpty;


public class StatusUpdateService implements Service {

    private final StatusUpdateRemedyGateway remedyGateway = new StatusUpdateRemedyGateway();
    private final StatusUpdateConfigProvider config = new StatusUpdateConfigProviderImpl();

    @Action(name = "salamUpdateStatusInProgress", scope = Scope.PUBLIC)
    public String salamUpdateStatusInProgress(
            @ActionParameter(name = "salamGuid") @NotBlank String salamGuid,
            @ActionParameter(name = "timeoutMs") int timeoutMs) {
        try {
            applyTrustStore();
            StatusUpdateTicketData data = remedyGateway.fetchByGuid(salamGuid, config);
            String envelope = buildEnvelope(data);
            return sendToSalam(envelope, timeoutMs);
        } catch (Exception e) {
            throw new RuntimeException("SALAM In-Progress update failed for GUID " + salamGuid, e);
        }
    }


    private void applyTrustStore() {
        System.setProperty("javax.net.ssl.trustStore", config.getSalamCertPath());
        System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
    }

    private String buildEnvelope(StatusUpdateTicketData data) {
        return "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                "xmlns:urn=\"urn:_ITC_HPD_StatusUpdate_Zain\">\n" +
                "  <soapenv:Header>\n" +
                "    <urn:AuthenticationInfo>\n" +
                "      <urn:userName>" + escapeXml(config.getSalamUser()) + "</urn:userName>\n" +
                "      <urn:password>" + escapeXml(config.getSalamPassword()) + "</urn:password>\n" +
                "    </urn:AuthenticationInfo>\n" +
                "  </soapenv:Header>\n" +
                "  <soapenv:Body>\n" +
                "    <urn:UpdateStatus>\n" +
                "      <urn:Salam_Ticket_No>" + escapeXml(data.getSalamTicketNo()) + "</urn:Salam_Ticket_No>\n" +
                "      <urn:Status>" + escapeXml(data.getStatus()) + "</urn:Status>\n" +
                "      <urn:Status_Note>" + escapeXml(orEmpty(data.getStatusNote())) + "</urn:Status_Note>\n" +
                "      <urn:AssignedGroup>" + escapeXml(orEmpty(data.getAssignedGroup())) + "</urn:AssignedGroup>\n" +
                "      <urn:Assignee>" + escapeXml(orEmpty(data.getAssignee())) + "</urn:Assignee>\n" +
                "    </urn:UpdateStatus>\n" +
                "  </soapenv:Body>\n" +
                "</soapenv:Envelope>";
    }

    private String sendToSalam(String envelope, int timeoutMs) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getStatusUpdateApiUrl()))
                .timeout(Duration.ofMillis(timeoutMs))
                .header("Accept", "application/soap+xml")
                .header("SOAPAction", "_ITC_HPD_StatusUpdate_Zain/Update")
                .header("Content-Type", "application/xml")
                .POST(HttpRequest.BodyPublishers.ofString(envelope, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return parseResult(response.body());
    }

    private String parseResult(String rawResponse) throws Exception {
        if (rawResponse == null || rawResponse.isBlank()) {
            throw new IllegalStateException("SALAM returned an empty response");
        }

        int faultStart = rawResponse.indexOf("<faultstring>");
        if (faultStart != -1) {
            int contentStart = faultStart + "<faultstring>".length();
            int end = rawResponse.indexOf("</faultstring>", contentStart);
            String faultString = end != -1 ? rawResponse.substring(contentStart, end) : "unknown fault";
            throw new IllegalStateException("SALAM fault: " + faultString);
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(rawResponse.getBytes(StandardCharsets.UTF_8)));

        NodeList list = doc.getElementsByTagName("urn:UpdateStatusResponse");
        if (list.getLength() == 0) {
            throw new IllegalStateException("Unexpected SALAM response shape");
        }
        Element e = (Element) list.item(0);
        NodeList resultNodes = e.getElementsByTagName("Result");
        return resultNodes.getLength() > 0 ? resultNodes.item(0).getTextContent() : null;
    }

    private static String escapeXml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }
}
