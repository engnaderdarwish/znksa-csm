package com.cmak.integrations.bundle.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bmc.arsys.rx.application.common.ServiceLocator;
import com.bmc.arsys.rx.services.common.DataPage;
import com.bmc.arsys.rx.services.common.DataPageQueryParameters;
import com.bmc.arsys.rx.services.common.Service;
import com.bmc.arsys.rx.services.record.RecordService;

/**
 * Reads one row out of the centralized configuration record, looked up by
 * Integration Name.
 *
 * Uses the same datapage query pattern as MOEHelpers in the MOE bundle, which
 * is the supported way to query by a field value:
 *
 *   recordService.getRecordInstancesByIdDataPage(new DataPageQueryParameters(map))
 *
 * IMPORTANT: this returns rows as Map&lt;String, Object&gt; keyed by the field id
 * AS A STRING - not RecordInstance. So every read goes through asString/asInt
 * below, which do the Integer -&gt; String key conversion.
 */
public class IntegrationConfigService implements Service {

    /** dataPageType required by the query datapage. */
    private static final String DATA_PAGE_TYPE =
            "com.bmc.arsys.rx.services.record.datapage.query";

    private final Map<String, CachedRow> cache = new HashMap<>();

    /**
     * @param integrationName e.g. "LBS" or "SMS"
     * @return the row keyed by field-id-as-String, or null when not found
     */
    public Map<String, Object> getRow(String integrationName) {
        long now = System.currentTimeMillis();

        synchronized (cache) {
            CachedRow cached = cache.get(integrationName);
            if (cached != null && (now - cached.loadedAtMs) < Constants.CACHE_TTL_MS) {
                ServiceLocator.getLogger().info("[CONFIG] Using cached configuration for ["
                        + integrationName + "], age " + ((now - cached.loadedAtMs) / 1000) + "s");
                return cached.row;
            }

            Map<String, Object> row = loadRow(integrationName);

            if (row == null) {
                // Never cache a failure - it would hide the diagnostics and
                // make every later call fail without retrying.
                cache.remove(integrationName);
                return null;
            }

            cache.put(integrationName, new CachedRow(row, System.currentTimeMillis()));
            return row;
        }
    }

    /** Call after editing the configuration record so changes apply at once. */
    public void invalidate() {
        synchronized (cache) {
            cache.clear();
        }
        ServiceLocator.getLogger().info("[CONFIG] Configuration cache cleared");
    }

    /* ------------------------------------------------------------------ */

    private Map<String, Object> loadRow(String integrationName) {
        RecordService recordService = ServiceLocator.getRecordService();

        String qualification = "'" + Constants.FIELD_ID_INTEGRATION_NAME
                + "' = \"" + escape(integrationName) + "\"";

        ServiceLocator.getLogger().info("[CONFIG] Loading configuration for ["
                + integrationName + "] from [" + Constants.CONFIG_RECORD_DEF
                + "] with qualification " + qualification);

        Map<String, List<String>> params = new HashMap<>();
        params.put("dataPageType", List.of(DATA_PAGE_TYPE));
        params.put("recorddefinition", List.of(Constants.CONFIG_RECORD_DEF));
        params.put("queryExpression", List.of(qualification));
        params.put("startIndex", List.of("0"));
        params.put("pageSize", List.of("2"));   // 2 so a duplicate name is visible
        params.put("propertySelection", allFieldIdsAsStrings());

        try {
            DataPage page = recordService.getRecordInstancesByIdDataPage(
                    new DataPageQueryParameters(params));

            if (page == null || page.getData() == null || page.getData().isEmpty()) {
                ServiceLocator.getLogger().error("[CONFIG] No configuration row found with "
                        + "Integration Name [" + integrationName + "] in ["
                        + Constants.CONFIG_RECORD_DEF + "]");
                return null;
            }

            if (page.getData().size() > 1) {
                ServiceLocator.getLogger().warn("[CONFIG] " + page.getData().size()
                        + " rows are named [" + integrationName
                        + "]. Using the first. Integration Name must be unique.");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> row = (Map<String, Object>) page.getData().get(0);

            logLoadedFields(integrationName, row);
            return row;

        } catch (Exception e) {
            ServiceLocator.getLogger().error("[CONFIG] Query failed for ["
                    + integrationName + "]: " + e);
            return null;
        }
    }

    /** Every field the mappers read, as the query wants them: field id strings. */
    private static List<String> allFieldIdsAsStrings() {
        List<String> ids = new ArrayList<>();
        ids.add(Integer.toString(Constants.FIELD_ID_INSTANCE_ID));
        ids.add(Integer.toString(Constants.FIELD_ID_CUSTOMER_ID));
        ids.add(Integer.toString(Constants.FIELD_ID_PASSWORD));
        ids.add(Integer.toString(Constants.FIELD_ID_HOSTNAME_IP));
        ids.add(Integer.toString(Constants.FIELD_ID_CLIENT_ID));
        ids.add(Integer.toString(Constants.FIELD_ID_CLIENT_SECRET));
        ids.add(Integer.toString(Constants.FIELD_ID_USERNAME));
        ids.add(Integer.toString(Constants.FIELD_ID_INTEGRATION_NAME));
        ids.add(Integer.toString(Constants.FIELD_ID_READ_TIMEOUT));
        ids.add(Integer.toString(Constants.FIELD_ID_CONNECTION_TIMEOUT));
        ids.add(Integer.toString(Constants.FIELD_ID_MAX_LOCATION_AGE));
        ids.add(Integer.toString(Constants.FIELD_ID_INTEGRATION_STATUS));
        ids.add(Integer.toString(Constants.FIELD_ID_DEFAULT_ORIGINATOR));
        ids.add(Integer.toString(Constants.FIELD_ID_SOAP_NAMESPACE));
        return ids;
    }

    /**
     * Escapes a value for a double-quoted AR qualification literal, so a stray
     * quote cannot break out and inject extra clauses. Same helper as MOEHelpers.q().
     */
    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /** Proves the field ids are right, without ever printing the password. */
    private static void logLoadedFields(String integrationName, Map<String, Object> row) {
        String password = asString(row, Constants.FIELD_ID_PASSWORD);

        ServiceLocator.getLogger().info("[CONFIG] Row loaded for [" + integrationName + "]"
                + " | InstanceId=" + asString(row, Constants.FIELD_ID_INSTANCE_ID)
                + " | IntegrationName=" + asString(row, Constants.FIELD_ID_INTEGRATION_NAME)
                + " | Hostname_IP=" + asString(row, Constants.FIELD_ID_HOSTNAME_IP)
                + " | Username=" + asString(row, Constants.FIELD_ID_USERNAME)
                + " | ClientID=" + asString(row, Constants.FIELD_ID_CLIENT_ID)
                + " | CustomerID=" + asString(row, Constants.FIELD_ID_CUSTOMER_ID)
                + " | Password=" + (password == null || password.isEmpty()
                ? "MISSING" : "present(" + password.length() + " chars)")
                + " | DefaultOriginator=" + asString(row, Constants.FIELD_ID_DEFAULT_ORIGINATOR)
                + " | SoapNamespace=" + asString(row, Constants.FIELD_ID_SOAP_NAMESPACE)
                + " | ConnectionTimeout=" + asString(row, Constants.FIELD_ID_CONNECTION_TIMEOUT)
                + " | ReadTimeout=" + asString(row, Constants.FIELD_ID_READ_TIMEOUT)
                + " | MaxLocationAge=" + asString(row, Constants.FIELD_ID_MAX_LOCATION_AGE)
                + " | IntegrationStatus=" + asString(row, Constants.FIELD_ID_INTEGRATION_STATUS));

        String actualName = asString(row, Constants.FIELD_ID_INTEGRATION_NAME);
        if (actualName != null && !actualName.trim().equalsIgnoreCase(integrationName)) {
            ServiceLocator.getLogger().error("[CONFIG] WRONG ROW: asked for ["
                    + integrationName + "] but got [" + actualName + "]");
        }
    }

    private static final class CachedRow {
        private final Map<String, Object> row;
        private final long loadedAtMs;

        private CachedRow(Map<String, Object> row, long loadedAtMs) {
            this.row = row;
            this.loadedAtMs = loadedAtMs;
        }
    }

    /* ------------------------------------------------------------------
     * Shared field readers - the row is keyed by field id AS A STRING
     * ------------------------------------------------------------------ */

    public static String asString(Map<String, Object> row, int fieldId) {
        if (row == null) {
            return null;
        }
        Object value = row.get(Integer.toString(fieldId));
        return value == null ? null : String.valueOf(value);
    }

    public static int asInt(Map<String, Object> row, int fieldId, int fallback) {
        String value = asString(row, fieldId);
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            ServiceLocator.getLogger().warn("[CONFIG] Field " + fieldId
                    + " is not numeric ('" + value + "'), using " + fallback);
            return fallback;
        }
    }

    /** A blank Integration Status counts as enabled. */
    public static boolean isEnabled(Map<String, Object> row) {
        String status = asString(row, Constants.FIELD_ID_INTEGRATION_STATUS);
        if (status == null || status.trim().isEmpty()) {
            return true;
        }
        for (String disabled : Constants.DISABLED_STATUS_VALUES) {
            if (disabled.equalsIgnoreCase(status.trim())) {
                ServiceLocator.getLogger().warn("[CONFIG] Integration Status is ["
                        + status + "] - treating the integration as DISABLED");
                return false;
            }
        }
        return true;
    }
}