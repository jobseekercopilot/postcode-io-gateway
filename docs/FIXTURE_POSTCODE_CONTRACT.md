# Fixture postcode HTTP contract

The deterministic postcode fixture remains an optional local/test dependency,
but this repository owns its narrow HTTP boundary. It does not consume a
generated system-data client, a local JAR or an external contract package.

## Request

When `DEPLOYMENT_ENVIRONMENT_CLASS` and `EXTERNAL_PROVIDER_MODE=FIXTURE` satisfy
the [provider-mode safety policy](PROVIDER_MODE_SECURITY.md), the gateway sends:

```text
GET {SYSTEM_DATA_SERVICE_URL}/internal/fixtures/postcodes/{postcode}
Accept: application/json
```

The postcode is decoded once at the gateway boundary and then inserted as an
encoded URI path segment. Query text and path separators therefore cannot
escape the postcode segment. The existing correlation-ID client customizer
adds `X-Correlation-Id` when a request correlation ID is available.

## Response fields

The gateway consumes this JSON shape:

| Field | Type | Use |
|---|---|---|
| `postcode` | string | Canonical/fallback postcode |
| `country` | string | Country |
| `region` | string | Region |
| `adminDistrict` | string | Administrative district |
| `latitude` | number | Latitude; missing values map to `0.0` for compatibility |
| `longitude` | number | Longitude; missing values map to `0.0` for compatibility |
| `found` | boolean | Safe lookup outcome used only for operational logging |

Unknown response fields are ignored so additive fixture-service changes remain
compatible. Non-success HTTP responses are propagated to the existing gateway
error path. Error payloads and postcode response contents are not logged, and
request logs use `/api/postcodes/{postcode}` rather than the submitted value.

Changing the path, field names or field types requires a coordinated contract
test with the fixture service. Never restore `libs/*.jar` or add an unapproved
repository/package to distribute this contract.
