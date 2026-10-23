package com.cmak.integrations.bundle.lbs.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.bmc.arsys.rx.application.common.ServiceLocator;
import com.cmak.integrations.bundle.config.Constants;
import com.cmak.integrations.bundle.lbs.config.LbsConfig;

import com.cmak.integrations.bundle.lbs.domain.LbsLocationResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Replaces the Apache DefaultHttpClient block in the legacy LBS jar.
 *
 * This is NOT SOAP. The body is a plain MLP 3.2.0 document with a DOCTYPE.
 * We keep the request byte-for-byte compatible with what production sends,
 * because that request is known to work; only the plumbing around it changed.
 *
 * Differences from the old code, all deliberate:
 *   - HttpURLConnection instead of DefaultHttpClient (deprecated, and one
 *     less OSGi dependency)
 *   - real connect and read timeouts (the old code had none and could hang)
 *   - values XML-escaped (the old code did not escape anything)
 *   - reply parsed with a real XML parser, then indexOf only as a fallback
 *   - the raw reply is always returned so the format can finally be seen
 */
public class MlpClient {

    private final LbsConfig config;

    public MlpClient(LbsConfig config) {
        this.config = config;
    }

    /**
     * Mirrors the whole body of the legacy main(): build the slir request,
     * post it, and pull the coordinates out of the answer.
     *
     * @param msisdn full number including country code, e.g. 966512345678
     * @param maxLocAgeSeconds how old a cached fix may be
     */
    public LbsLocationResult locate(String msisdn, int maxLocAgeSeconds) {

        String request = buildSlirRequest(msisdn, maxLocAgeSeconds);

        ServiceLocator.getLogger().info("LBS locate started - MSISDN: " + msisdn
                + " | maxLocAge: " + maxLocAgeSeconds + " | URL: " + config.getLbsUrl());

        HttpURLConnection connection = null;
        String raw = null;
        try {
            URL url = new URL(config.getLbsUrl());
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(config.getConnectTimeoutMs());
            connection.setReadTimeout(config.getReadTimeoutMs());
            connection.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
            connection.setRequestProperty("Accept", "text/plain");

            byte[] payload = request.getBytes(StandardCharsets.UTF_8);
            connection.setRequestProperty("Content-Length", String.valueOf(payload.length));

            try (OutputStream out = connection.getOutputStream()) {
                out.write(payload);
                out.flush();
            }

            int status = connection.getResponseCode();
            raw = readStream(status >= 400 ? connection.getErrorStream()
                    : connection.getInputStream());

            // The project has no sample MLP reply. Log it in full so the real
            // format is captured on the very first call.
            ServiceLocator.getLogger().info("LBS raw reply (HTTP " + status + "): " + raw);

            if (status >= 400) {
                return LbsLocationResult.failed("HTTP " + status + " from the LBS service",
                        truncate(raw));
            }

            return parseReply(raw);

        } catch (Exception e) {
            ServiceLocator.getLogger().error("Exception during LBS locate for [" + msisdn
                    + "] at [" + config.getLbsUrl() + "]" + e);
            return LbsLocationResult.failed(
                    e.getClass().getSimpleName() + ": " + e.getMessage(), truncate(raw));
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /* ------------------------------------------------------------------ */

    /**
     * Byte-for-byte the same document the legacy jar sent, including the
     * DOCTYPE and the absence of an XML declaration.
     */
    private String buildSlirRequest(String msisdn, int maxLocAgeSeconds) {
        return Constants.LbsConstants.MLP_DOCTYPE + "<svc_init>\n"
                + "<hdr ver=\"" + Constants.LbsConstants.MLP_VERSION + "\">\n"
                + "<client>\n"
                + "<id>" + escapeXml(config.getClientId()) + "</id>\n"
                + "<pwd>" + escapeXml(config.getLbsPassword()) + "</pwd>\n"
                + "</client>\n"
                + "</hdr>\n"
                + "<slir res_type=\"SYNC\" ver=\"" + Constants.LbsConstants.MLP_VERSION + "\">\n"
                + "<msids>\n"
                + "<msid type=\"MSISDN\">" + escapeXml(msisdn) + "</msid>\n"
                + "</msids>\n"
                + "<eqop>\n"
                + "<max_loc_age>" + maxLocAgeSeconds + "</max_loc_age>\n"
                + "</eqop>\n"
                + "<loc_type type=\"CURRENT\"/>\n"
                + "</slir>\n"
                + "</svc_init>";
    }

    /**
     * The old code used indexOf on the raw string. That breaks on any
     * formatting change, so a real parser goes first and indexOf is only
     * the safety net.
     */
    private LbsLocationResult parseReply(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return LbsLocationResult.failed("The LBS service returned an empty reply", null);
        }

        Document document = parse(raw);

        // ---- success path: <pos><pd><shape><EllipticalArea><coord><X>/<Y> ----
        String xDms = elementText(document, raw, "X");
        String yDms = elementText(document, raw, "Y");

        if (xDms != null && yDms != null) {
            LbsLocationResult result = LbsLocationResult.located(
                    xDms, yDms,
                    dmsToDecimal(xDms), dmsToDecimal(yDms),
                    truncate(raw));

            // Extras the legacy code fetched and then threw away.
            result.setMsid(elementText(document, raw, "msid"));
            result.setAccuracyMeters(elementText(document, raw, "semiMajor"));
            result.setMcc(elementText(document, raw, "mcc"));
            result.setMnc(elementText(document, raw, "mnc"));
            result.setLac(elementText(document, raw, "lac"));
            result.setCellId(elementText(document, raw, "cellid"));
            return result;
        }

        // ---- failure path: <poserr><result resid="N">TEXT</result> ----
        String resid = attributeOf(document, "result", "resid");
        String resultText = elementText(document, raw, "result");

        if (resid == null && resultText == null) {
            // Last resort, the way the old jar did it.
            resultText = between(raw, "\">", "</result>");
            resid = between(raw, "<result resid=\"", "\">");
        }

        if (resid != null || resultText != null) {
            LbsLocationResult result = LbsLocationResult.notLocated(
                    resid, resultText,
                    elementText(document, raw, "add_info"),
                    truncate(raw));
            result.setMsid(elementText(document, raw, "msid"));
            return result;
        }

        return LbsLocationResult.failed(
                "Could not find coordinates or a result code in the LBS reply",
                truncate(raw));
    }

    /**
     * MLP sends coordinates as degrees minutes seconds with a hemisphere
     * letter, for example "24 45 03.7N" and "46 39 06.1E". Nothing can put
     * that on a map, so convert to decimal degrees.
     *
     * The legacy jar never did this - it passed the raw dms string straight
     * through as if it were a number.
     *
     * @return decimal degrees as a string, or null if the format is unexpected
     */
    static String dmsToDecimal(String dms) {
        if (dms == null) {
            return null;
        }
        String value = dms.trim().toUpperCase();
        if (value.isEmpty()) {
            return null;
        }

        boolean negative = value.endsWith("S") || value.endsWith("W");
        boolean hasHemisphere = negative || value.endsWith("N") || value.endsWith("E");
        if (hasHemisphere) {
            value = value.substring(0, value.length() - 1).trim();
        }

        try {
            String[] parts = value.split("[\\s:]+");
            double degrees;
            if (parts.length >= 3) {
                degrees = Double.parseDouble(parts[0])
                        + Double.parseDouble(parts[1]) / 60.0
                        + Double.parseDouble(parts[2]) / 3600.0;
            } else if (parts.length == 2) {
                degrees = Double.parseDouble(parts[0])
                        + Double.parseDouble(parts[1]) / 60.0;
            } else {
                // Already decimal.
                degrees = Double.parseDouble(parts[0]);
            }
            if (negative) {
                degrees = -degrees;
            }
            return String.format("%.6f", degrees);
        } catch (Exception e) {
            ServiceLocator.getLogger().warn("Could not convert coordinate [" + dms
                    + "] to decimal degrees: " + e);
            return null;
        }
    }

    /* ------------------------------------------------------------------ */

    private static String elementText(Document document, String xml, String localName) {
        if (document == null) {
            return between(xml, "<" + localName + ">", "</" + localName + ">");
        }
        NodeList nodes = document.getElementsByTagName("*");
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (localName.equalsIgnoreCase(node.getLocalName())
                    || localName.equalsIgnoreCase(node.getNodeName())) {
                String text = node.getTextContent();
                if (text != null && !text.trim().isEmpty()) {
                    return text.trim();
                }
            }
        }
        return null;
    }

    private static String attributeOf(Document document, String localName, String attribute) {
        if (document == null) {
            return null;
        }
        NodeList nodes = document.getElementsByTagName("*");
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (localName.equalsIgnoreCase(node.getLocalName())
                    || localName.equalsIgnoreCase(node.getNodeName())) {
                if (node instanceof Element) {
                    String value = ((Element) node).getAttribute(attribute);
                    if (value != null && !value.trim().isEmpty()) {
                        return value.trim();
                    }
                }
            }
        }
        return null;
    }

    /**
     * MLP replies carry their own DOCTYPE, so unlike the SOAP client we must
     * ALLOW the doctype declaration - but we must never let the parser go and
     * fetch MLP_SVC_RESULT_320.DTD over the network, which would hang.
     */
    private static Document parse(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setValidating(false);
            factory.setExpandEntityReferences(false);
            trySetFeature(factory, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            trySetFeature(factory, "http://xml.org/sax/features/external-general-entities", false);
            trySetFeature(factory, "http://xml.org/sax/features/external-parameter-entities", false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            // Hand back an empty DTD instead of downloading one.
            builder.setEntityResolver((publicId, systemId) ->
                    new InputSource(new StringReader("")));

            return builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            ServiceLocator.getLogger().warn("Could not parse the LBS reply as XML, "
                    + "falling back to plain text search: " + e);
            return null;
        }
    }

    private static void trySetFeature(DocumentBuilderFactory factory, String feature, boolean value) {
        try {
            factory.setFeature(feature, value);
        } catch (Exception ignored) {
            // Not all parsers know every feature; safe to continue.
        }
    }

    /** Last-resort extraction, the way the old jar did it. */
    private static String between(String text, String open, String close) {
        if (text == null) {
            return null;
        }
        int start = text.indexOf(open);
        if (start < 0) {
            return null;
        }
        int from = start + open.length();
        int end = text.indexOf(close, from);
        if (end < 0) {
            return null;
        }
        String value = text.substring(from, end).trim();
        return value.isEmpty() ? null : value;
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

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= Constants.LbsConstants.RAW_RESPONSE_LIMIT
                ? value
                : value.substring(0, Constants.LbsConstants.RAW_RESPONSE_LIMIT) + "...[truncated]";
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