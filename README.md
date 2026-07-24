# Postcode.io Gateway

Spring Boot boundary for postcode/outcode lookup and bounded place-name search. LIVE mode calls
`api.postcodes.io`; FIXTURE mode calls system-data-service for deterministic
test data.

> Beta status: not beta-ready. The fixture client is an in-repository HTTP
> boundary, live calls are bounded, and the consumed provider contract is
> monitored, but full consumer-journey coverage remains incomplete.
> See [the audit](docs/BETA_READINESS_AUDIT.md).

Postcode acquisition is an upstream profile concern, not part of Job Search
provider fan-out. That boundary is defined in the Infrastructure
[Job Search architecture ADR](https://github.com/jobseekercopilot/infrastructure/blob/develop/docs/adr/0001-job-search-architecture-and-ownership.md).

## Requirements and configuration

- Java 17 and Maven 3.9

| Variable | Local default | Purpose |
|---|---|---|
| `SERVER_PORT` | `8082` | HTTP port |
| `DEPLOYMENT_ENVIRONMENT_CLASS` | none (required) | `LOCAL`, `TEST`, `DEMO`, `STAGING`, or `PRODUCTION` |
| `EXTERNAL_PROVIDER_MODE` | none (required) | `LIVE` or `FIXTURE`; must match the environment-class policy |
| `EXTERNAL_PROVIDER_BASE_URL` | `https://api.postcodes.io` | Trusted LIVE provider origin; override only for an approved local test stub |
| `PROVIDER_CONNECT_TIMEOUT` / `PROVIDER_RESPONSE_TIMEOUT` / `PROVIDER_TOTAL_TIMEOUT` | `500ms` / `1s` / `4s` | Per-connect, per-attempt and hard logical-call deadlines |
| `PROVIDER_MAX_RETRIES` / `PROVIDER_INITIAL_BACKOFF` / `PROVIDER_MAX_BACKOFF` / `PROVIDER_RETRY_JITTER` | `2` / `100ms` / `500ms` / `0.5` | Bounded transient-failure retry policy |
| `PROVIDER_CIRCUIT_FAILURE_THRESHOLD` / `PROVIDER_CIRCUIT_OPEN_DURATION` | `5` / `30s` | Consecutive logical failures before open and half-open delay |
| `PROVIDER_SUCCESS_CACHE_TTL` / `PROVIDER_NEGATIVE_CACHE_TTL` / `PROVIDER_CACHE_MAXIMUM_ENTRIES` | `15m` / `1m` / `1000` | Bounded per-instance positive/404 cache |
| `MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE` | `health,info` | Actuator web endpoints; expose `metrics` only on an approved operational network |
| `PROVIDER_MODE_ENDPOINT_ENABLED` | `false` | Opt in to `/internal/provider-mode`; forbidden in `STAGING`/`PRODUCTION` |
| `SYSTEM_DATA_SERVICE_URL` | `http://localhost:8103` | Fixture dependency |
| `FIXTURE_DATASET_ID` / `FIXTURE_DATASET_VERSION` / `FIXTURE_SCENARIO` | demo values | Fixture safety metadata exposed by the existing internal mode endpoint |

No provider credential is currently required by postcodes.io. Do not add one
to source.

## API, health and build

- `GET /api/postcodes/{postcode}`
- `GET /api/places?q={place-name}&limit={1..10}` (LIVE mode)
- `GET /internal/provider-mode` (only when explicitly enabled in a local/test/demo environment)
- `/v3/api-docs`, `/swagger-ui/index.html`, `/actuator/health`
- `/actuator/health/readiness` (aggregate application and provider-circuit readiness)

```bash
mvn -B clean verify
./scripts/test-dependency-report-policy.sh
./scripts/verify-container.sh
DEPLOYMENT_ENVIRONMENT_CLASS=LOCAL EXTERNAL_PROVIDER_MODE=FIXTURE mvn spring-boot:run
```

The release-shaped container workflow runs the complete verification before
building from the verified JAR. The digest-pinned image runs read-only as fixed
UID/GID `10001:10001`, exposes a redacted readiness check, supports graceful
shutdown and is scanned for Critical/High OS and library findings in CI. See
[service operations](docs/OPERATIONS.md) and the
[observability contract](docs/OBSERVABILITY.md).

CI scans the resolved runtime dependency set with pinned Trivy releases,
publishes the JSON report, and rejects unaccepted Critical or High findings.
See [dependency security](docs/DEPENDENCY_SECURITY.md) for local reproduction,
scanner scope, and the time-bounded exception process.

The build has no local JAR dependency and succeeds from a clean clone. FIXTURE
mode uses the small HTTP contract documented in
[`docs/FIXTURE_POSTCODE_CONTRACT.md`](docs/FIXTURE_POSTCODE_CONTRACT.md); do not
restore generated binaries or create another repository for it.

Startup fails when either environment class or provider mode is missing, blank,
unknown, or unsafe. See the [provider-mode safety policy](docs/PROVIDER_MODE_SECURITY.md)
for the allowed matrix, Compose examples, diagnostics restriction, and rollout
requirements.

LIVE lookups have explicit deadlines, safe jittered retries, a circuit breaker,
separate positive/negative cache TTLs and bounded-cardinality metrics. See the
[provider resilience policy](docs/PROVIDER_RESILIENCE.md) for the retryable
failure set, request budget, metrics, alert starting points, ownership and
residual risks.

Requests accept syntactically valid UK full postcodes and outcodes, canonicalise
them before provider access, and expose stable redacted `400`, `404`, `429`,
`502`, `503`, and `504` error responses. See the
[public postcode API contract](docs/POSTCODE_API_CONTRACT.md) for accepted
forms, error codes, correlation-ID rules, and consumer ownership.

Place search accepts a restricted 2–80 character name and at most ten results.
It uses the same LIVE provider deadlines, retry/circuit and safe telemetry as
postcode lookup. See the [place-search contract](docs/PLACE_SEARCH_CONTRACT.md)
for fields, privacy, fixture-mode behavior and deterministic testing rules.

LIVE provider paths are constructed from isolated path segments. The gateway
accepts additive provider fields but rejects missing or mismatched response
identity and increments a bounded compatibility-failure metric. See the
[Postcodes.io compatibility contract](docs/PROVIDER_COMPATIBILITY.md) for the
reviewed upstream assumptions, fixtures, alert signal, change process, and
residual risk.

## Branch workflow and troubleshooting

Use `feature/* → develop`; `main` is not used for application delivery. Confirm the active
provider mode before diagnosing lookups. Production must never use fixture mode
and tests should not make uncontrolled live calls.

## Licence

Copyright © 2026 Bernard McGeever. All rights reserved.

This repository contains proprietary software belonging to Bernard McGeever.
It may not be used, copied, modified or distributed without express written
permission. See [LICENSE](./LICENSE).
