package com.cmak.integrations.bundle.sms.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.bmc.arsys.rx.application.common.ServiceLocator;
import com.cmak.integrations.bundle.config.Constants;
import com.cmak.integrations.bundle.sms.config.SmsConfig;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;




/**
 * Replaces com.pmmsoapmessenger.Messenger / MessengerSoap.
 *
 * The JAX-WS generated stubs are deliberately NOT ported into the bundle:
 * JAX-WS is gone from the JDK used by the Helix container and dragging the
 * RI + JAXB into an OSGi bundle causes classloader grief. The two operations
 * this integration needs (Authenticate, SendSms) are simple enough to build
 * as raw envelopes over HttpURLConnection, which needs no extra imports.
 *
 * Everything version-specific (namespace, operation names, enum wire values)
 *
 */
public class PmmSoapClient {


    private final SmsConfig config;

    public PmmSoapClient(SmsConfig config) {
        this.config = config;
    }

    /**
     * Mirrors: port.authenticate(soapUser)
     */
    public SoapCallResult authenticate(String language) {
        String body = "<" + Constants.SmsConstants.OP_AUTHENTICATE + " xmlns=\"" + config.getSoapNamespace() + "\">"
                + buildSoapUser(language)
                + "</" + Constants.SmsConstants.OP_AUTHENTICATE + ">";

        ServiceLocator.getLogger().info("Authentication started - URL: "+config.getSmsWsUrl() +"| User: "+ config.getSmsUser());
        return invoke(Constants.SmsConstants.OP_AUTHENTICATE, body);
    }

    /**
     * Mirrors: port.sendSms(user, originator, smsText, recipientPhone,
     *                       messageType, defDate, blink, flash, private)
     *
     * blink / flash / private were always false in the legacy code, and
     * defDate was always an empty string — kept identical here.
     */
    public SoapCallResult sendSms(String language,
                                  String originator,
                                  String smsText,
                                  String recipientPhone,
                                  String messageType) {

        StringBuilder body = new StringBuilder(512);
        body.append("<").append(Constants.SmsConstants.OP_SEND_SMS)
                .append(" xmlns=\"").append(config.getSoapNamespace()).append("\">");
        body.append(buildSoapUser(language));
        body.append("<originator>").append(escapeXml(originator)).append("</originator>");
        body.append("<smsText>").append(escapeXml(smsText)).append("</smsText>");
        body.append("<recipientPhone>").append(escapeXml(recipientPhone)).append("</recipientPhone>");
        body.append("<messageType>").append(escapeXml(messageType)).append("</messageType>");
        body.append("<defDate></defDate>");
        body.append("<blink>false</blink>");
        body.append("<flash>false</flash>");
        body.append("<Private>false</Private>");
        body.append("</").append(Constants.SmsConstants.OP_SEND_SMS).append(">");

        ServiceLocator.getLogger().info("Sending SMS - Phone: "+recipientPhone+" | Originator: "+originator +"| MessageType: "
                + messageType);

        return invoke(Constants.SmsConstants.OP_SEND_SMS, body.toString());
    }

    /* ------------------------------------------------------------------ */

    /** Mirrors the SoapUser bean: CustomerID / Name  / Language / Password . */
    private String buildSoapUser(String language) {


        return "<user>"
                + "<CustomerID>" + config.getCustomerId() + "</CustomerID>"
                + "<Name>" + escapeXml(config.getSmsUser()) + "</Name>"
                + "<Language>" + escapeXml(language) + "</Language>"
                + "<Password>" + escapeXml(config.getSmsPassword()) + "</Password>"
                + "</user>";
    }

    /**  1- Wraps the body in a SOAP envelope
     *   2- opens an HttpURLConnection     */
    private SoapCallResult invoke(String operation, String bodyXml) {
        String envelope = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                + "<soap:Body>" + bodyXml + "</soap:Body>"
                + "</soap:Envelope>";

        HttpURLConnection connection = null;
        try {
            URL url = new URL(config.getSmsWsUrl());
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(config.getConnectTimeoutMs());
            connection.setReadTimeout(config.getReadTimeoutMs());
            connection.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
            connection.setRequestProperty("SOAPAction", "\"" + config.getSoapNamespace() + operation + "\"");

            byte[] payload = envelope.getBytes(StandardCharsets.UTF_8);
            connection.setRequestProperty("Content-Length", String.valueOf(payload.length));

            try (OutputStream out = connection.getOutputStream()) {
                out.write(payload);
                out.flush();
            }

            int status = connection.getResponseCode();
            String response = readStream(status >= 400 ? connection.getErrorStream()
                    : connection.getInputStream());

            if (status >= 400) {
                String fault = extractElementText(response, "faultstring");
                String message = "HTTP " + status + " from SMS web service"
                        + (fault == null ? "" : " - fault: " + fault);
                ServiceLocator.getLogger().error(message+" | operation: " +operation+ " | URL: "+ config.getSmsWsUrl());
                return SoapCallResult.failure(message);
            }

            String result = extractElementText(response, "Result");
            String transactionId = extractElementText(response, "TransactionID");

            if (result == null) {
                String fault = extractElementText(response, "faultstring");
                String message = fault != null
                        ? "SOAP fault: " + fault
                        : "No <Result> element in the " + operation + " response";
                ServiceLocator.getLogger().error(message+" | URL: "+ config.getSmsWsUrl());
                return SoapCallResult.failure(message);
            }

            ServiceLocator.getLogger().info(operation+" completed - Result: "+result+" | TransactionID: "+ transactionId);
            return SoapCallResult.success(result, transactionId);

        } catch (Exception e) {
            ServiceLocator.getLogger().error("Exception during " +operation+ " call to ["+config.getSmsWsUrl()+"]"+ e);
            return SoapCallResult.failure(e.getClass().getSimpleName() + ": " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String readStream(InputStream in) throws Exception {
        if (in == null) {
            return "";
        }
        try (InputStream stream = in) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int read;
            while ((read = stream.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Namespace-agnostic lookup by local name, so a namespace change in the
     * WSDL does not silently break parsing.
     */
    private static String extractElementText(String xml, String localName) {
        if (xml == null || xml.isEmpty()) {
            return null;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setExpandEntityReferences(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(
                    new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

            NodeList nodes = document.getElementsByTagName("*");
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (localName.equalsIgnoreCase(node.getLocalName())
                        || localName.equalsIgnoreCase(node.getNodeName())) {
                    String text = node.getTextContent();
                    return text == null ? null : text.trim();
                }
            }
        } catch (Exception e) {
            ServiceLocator.getLogger().warn("Failed to parse SOAP response looking for <"+localName+">" + e);
        }
        return null;
    }

    private static String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '&':  out.append("&amp;");  break;
                case '<':  out.append("&lt;");   break;
                case '>':  out.append("&gt;");   break;
                case '"':  out.append("&quot;"); break;
                case '\'': out.append("&apos;"); break;
                default:   out.append(c);
            }
        }
        return out.toString();
    }
}