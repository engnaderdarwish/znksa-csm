package com.cmak.integrations.bundle.salam.workinfo;

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

import static com.cmak.integrations.bundle.salam.workinfo.WorkInfoTicketData.orEmpty;


public class WorkInfoService implements Service {

    private final WorkInfoRemedyGateway remedyGateway = new WorkInfoRemedyGateway();
    private final WorkInfoConfigProvider config = new WorkInfoConfigProviderImpl();

    @Action(name = "salamCreateWorkInfo", scope = Scope.PUBLIC)
    public String salamCreateWorkInfo(
            @ActionParameter(name = "salamGuid") @NotBlank String salamGuid,
            @ActionParameter(name = "timeoutMs") int timeoutMs) {
        try {
            applyTrustStore();
            WorkInfoTicketData data = remedyGateway.fetchByGuid(salamGuid, config);
            String envelope = buildEnvelope(data);
            return sendToSalam(envelope, timeoutMs);
        } catch (Exception e) {
            throw new RuntimeException("SALAM WorkInfo creation failed for GUID " + salamGuid, e);
        }
    }

    private void applyTrustStore() {
        System.setProperty("javax.net.ssl.trustStore", config.getSalamCertPath());
        System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
    }

    private String buildEnvelope(WorkInfoTicketData data) {
        return "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                "xmlns:urn=\"urn:_ITC_HPD_AddLog_Zain\">\n" +
                "  <soapenv:Header>\n" +
                "    <urn:AuthenticationInfo>\n" +
                "      <urn:userName>" + escapeXml(config.getSalamUser()) + "</urn:userName>\n" +
                "      <urn:password>" + escapeXml(config.getSalamPassword()) + "</urn:password>\n" +
                "    </urn:AuthenticationInfo>\n" +
                "  </soapenv:Header>\n" +
                "  <soapenv:Body>\n" +
                "    <urn:Create>\n" +
                "      <urn:SecureWorkLog>" + escapeXml(orEmpty(data.getSecureWorkLog())) + "</urn:SecureWorkLog>\n" +
                "      <urn:ViewAccess>" + escapeXml(orEmpty(data.getViewAccess())) + "</urn:ViewAccess>\n" +
                "      <urn:CommunicationSource>" + escapeXml(orEmpty(data.getCommunicationSource())) + "</urn:CommunicationSource>\n" +
                "      <urn:WorkLogType>" + escapeXml(orEmpty(data.getWorkLogType())) + "</urn:WorkLogType>\n" +
                "      <urn:IncidentNumber>" + escapeXml(orEmpty(data.getIncidentNumber())) + "</urn:IncidentNumber>\n" +
                "      <urn:WorkLogSubmitter>" + escapeXml(orEmpty(data.getWorkLogSubmitter())) + "</urn:WorkLogSubmitter>\n" +
                "      <urn:DetailedDescription>" + escapeXml(orEmpty(data.getDetailedDescription())) + "</urn:DetailedDescription>\n" +
                "      <urn:Description>" + escapeXml(orEmpty(data.getDescription())) + "</urn:Description>\n" +
                "    </urn:Create>\n" +
                "  </soapenv:Body>\n" +
                "</soapenv:Envelope>";
    }

    private String sendToSalam(String envelope, int timeoutMs) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getWorkInfoApiUrl()))
                .timeout(Duration.ofMillis(timeoutMs))
                .header("Accept", "application/soap+xml")
                .header("SOAPAction", "_ITC_HPD_AddLog_Zain/Create")
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

        NodeList list = doc.getElementsByTagName("urn:CreateResponse");
        if (list.getLength() == 0) {
            throw new IllegalStateException("Unexpected SALAM response shape");
        }
        Element e = (Element) list.item(0);
        NodeList resultNodes = e.getElementsByTagName("Work_Log_ID");
        return resultNodes.getLength() > 0 ? resultNodes.item(0).getTextContent() : null;
    }

    private static String escapeXml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }
}
