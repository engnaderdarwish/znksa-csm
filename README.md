# Zain KSA CSM Integrations

A **BMC Helix Innovation Studio (RX) bundle** that connects **BMC Helix CSM** to the external systems used by **Zain KSA**: an SMS gateway, a location service (LBS), the **Dawiyat** FTTH partner and the **SALAM** partner.

This bundle is a **modernization of legacy code** (standalone Java JARs called from AR System filters, plus separate web services) into native, configurable RX Actions and REST endpoints that run inside Helix and can be used directly from Helix processes.

## What changed compared to the legacy implementation

| Legacy | This bundle |
|---|---|
| Standalone JARs launched from AR filters, results read back from `stdout` (`1` = success, `2` = failure) | RX Actions that return **structured results** (success flag, transaction ID, error message) |
| Endpoints, users, passwords and ports in `.properties` files or hard-coded | **One centralized configuration form in Helix**, one row per integration, editable at runtime |
| Separate inbound web service | A native RX **REST resource** that writes incoming updates to a staging record |

## Integrations

| Integration | Direction | Exposed as | Purpose |
|---|---|---|---|
| **SMS** | Outbound | Action `sendZainSms` | Sends English or Arabic SMS through the Zain SMS SOAP web service (authenticate, then send). |
| **LBS** | Outbound | Action `locateSubscriber` | Locates a subscriber by phone number using the MLP (XML over HTTP) location service. Returns latitude / longitude. |
| **Dawiyat (FTTH)** | Inbound | REST `PUT dawiyat/{partner}/tickets/{troubleTicketId}/notification` | Receives ticket updates from the Dawiyat partner and stores them in a staging record for Helix workflows to process. |
| **SALAM** | Outbound | Actions `salamCreateWorkInfo`, `salamUpdateStatusInProgress`, `salamResolveTicket` | Reads a ticket from the staging form and pushes a work log / status update / resolution to SALAM over SOAP. |

### SMS: `sendZainSms`

| Parameter | Notes |
|---|---|
| `recipientPhone` | Required |
| `messageText` | Required. The token `#INTG#` is converted to a new line (kept from the legacy filter contract). |
| `language` | Required. `English` or `Arabic` (selects the SOAP message type). |
| `senderName` | Optional. Only `Zain` or `1555` can override the configured default originator. |

### LBS: `locateSubscriber`

| Parameter | Notes |
|---|---|
| `phoneNumber` | Required. Normalized to a KSA MSISDN (handles `05...`, `00966...`, `966...`). |
| `maxLocationAgeSeconds` | Optional. Falls back to the configured value. |

Returns latitude, longitude (decimal and DMS), cell information, result code and the raw reply for diagnosis.

### Dawiyat: inbound REST endpoint

```
PUT /dawiyat/{partner}/tickets/{troubleTicketId}/notification
Content-Type: application/json

{
  "u_trouble_ticket_number": "...",   // required
  "case_number": "...",               // required
  "state": "...",
  "comments": "..."
}
```

The update is stored in the Dawiyat staging record with processing status `New`, so a Helix process can pick it up. The response always has the same JSON shape: `errCode`, `errDisc`, `stagingId`. HTTP status is `200` on success, `400` for an invalid payload and `500` for any other error. Long comments are truncated instead of rejected.

### SALAM actions

All three take `salamGuid` and `timeoutMs`. Each action:

1. reads the ticket from the generic staging form (`INTG:CS:Generic-StagingForm`) by SALAM GUID, using the AR System Java API;
2. builds the SOAP envelope for the matching SALAM web service (`AddLog`, `StatusUpdate`, `StatusUpdate Resolved-Reassign`);
3. sends it using the server certificate configured in Helix and returns SALAM's result (SOAP faults raise an error).

## Architecture

```
 Helix process / CSM filter ──> RX Action / REST resource
                                      │
                    ┌─────────────────┼──────────────────────┐
                    ▼                 ▼                      ▼
            SmsService          LbsService          DawiyatService      SALAM services
            (SOAP client)       (MLP client)        (staging record)    (AR API + SOAP)
                    └────────┬────────┴───────────────┬──────────────────────┘
                             ▼                        ▼
                 IntegrationConfigService     Helix records / external systems
                 (reads the central config
                  form, 5-minute cache)
```

- **`IntegrationConfigService`** loads the row whose *Integration Name* is `SMS`, `LBS` or `DAWIYAT` from the centralized configuration form, caches it for 5 minutes and never caches a failure. A blank *Integration Status* counts as enabled; `Inactive`, `Disabled`, `Off` or `Stopped` switch the integration off.
- Each integration has its own `*ConfigService` that maps generic fields (hostname, username, password, timeouts, namespace, originator) to a typed config object and validates it before any network call.
- Secrets are never logged: the password is reported only as present or missing.

## Configuration

No credentials or endpoints are stored in this repository. At runtime everything comes from the Helix configuration record `com.cmak.csm:CS KSAZN INTG - Centralized Configuration Form`.

| Integration Name | Used fields |
|---|---|
| `SMS` | Hostname/IP (web service URL), Username, Password, Customer ID, Default Originator, SOAP Namespace, timeouts, status |
| `LBS` | Hostname/IP (service URL), Username or Client ID, Password, Max Location Age, timeouts, status |
| `DAWIYAT` | Status |
| SALAM settings row | AR server, port, user, password, SALAM user, SALAM password, certificate path, status-resolve API URL |

Defaults used only when a field is left blank are in `Constants.java`. For the SMS Customer ID and the LBS client ID they are placeholders; set the real values in the configuration record.

### Deployment credentials

The Maven `deploy` profile needs a developer account. The values in the root `pom.xml` are placeholders. Pass real values on the command line so they never enter version control:

```bash
mvn clean install -Pdeploy \
  -DdeveloperUserName=<your_dev_account> \
  -DdeveloperPassword=<your_dev_password> \
  -DwebUrl=https://<your-helix-server>
```

## Prerequisites

- Java 17
- Maven 3.x
- BMC RX SDK `26.2.00-SNAPSHOT` and `rx-sdk-maven-plugin` in your local `.m2` repository (BMC proprietary, not included)
- BMC AR System Java API `com.bmc.arsys:arAPI:91.9` (used by the SALAM integration)
- A BMC Helix Innovation Studio environment with the `com.cmak.csm` application and the configuration, staging and generic staging forms referenced above

## Build and deploy

```bash
mvn clean install                 # build
mvn clean install -Pdeploy        # build and deploy to the Helix server
mvn clean install -Pexport        # export definitions from the server
mvn clean install -Plocalization  # localization
```

## Project structure

```
znksa-csm/
├── pom.xml                              # Parent POM (RX SDK properties, deploy settings)
├── bundle/                              # Java code of the bundle
│   └── src/main/java/com/cmak/integrations/bundle/
│       ├── MyApplication.java               # Registers every service and the REST resource
│       ├── config/                          # Constants + IntegrationConfigService (central config)
│       ├── sms/    (client, config, domain, service)   # Zain SMS SOAP integration
│       ├── lbs/    (client, config, domain, service)   # MLP location integration
│       ├── dawiyat/ (config, domain, resource, service) # Inbound FTTH REST endpoint
│       └── salam/  (resolve, statusupdate, workinfo)    # SALAM SOAP integration
└── package/                             # Deployment package
    └── src/main/definitions/db/             # Process, record and view definitions
```

## Tech stack

Java 17 · Maven · BMC Helix Innovation Studio RX SDK · AR System Java API · Jersey / JAX-RS · Jackson · SOAP and MLP (XML over HTTP) clients · OSGi bundle

## Possible improvements

- The SALAM settings are read field by field from a record identified by a fixed ID. Moving them to the shared, cached `IntegrationConfigService` (one row per integration) would match the other integrations and remove the hard-coded ID.
- The SALAM actions set `javax.net.ssl.trustStore` as a JVM-wide property. A dedicated `SSLContext` per client would avoid side effects on other bundles.
- The "Test Connection" actions for SMS and LBS are commented out in the source while the matching Helix processes still exist; restore or remove them together.
- Add unit tests for MSISDN normalization, SOAP / MLP parsing and the Dawiyat payload validation.
