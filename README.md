# Postcode.io Gateway

Spring Boot boundary for postcode/outcode lookup. LIVE mode calls
`api.postcodes.io`; FIXTURE mode calls system-data-service for deterministic
test data.

> Beta status: not beta-ready. The fixture client is now an in-repository HTTP
> boundary, but live-provider resilience/error mapping is incomplete. See
> [the audit](docs/BETA_READINESS_AUDIT.md).

## Requirements and configuration

- Java 17 and Maven 3.9

| Variable | Local default | Purpose |
|---|---|---|
| `SERVER_PORT` | `8082` | HTTP port |
| `EXTERNAL_PROVIDER_MODE` | `LIVE` | `LIVE` or `FIXTURE` |
| `SYSTEM_DATA_SERVICE_URL` | `http://localhost:8103` | Fixture dependency |
| `FIXTURE_DATASET_ID` / `FIXTURE_DATASET_VERSION` / `FIXTURE_SCENARIO` | demo values | Fixture safety metadata exposed by the existing internal mode endpoint |

No provider credential is currently required by postcodes.io. Do not add one
to source.

## API, health and build

- `GET /api/postcodes/{postcode}`
- `GET /internal/provider-mode`
- `/v3/api-docs`, `/swagger-ui/index.html`, `/actuator/health`

```bash
mvn -B clean verify
./scripts/test-dependency-report-policy.sh
mvn spring-boot:run
docker build -t postcode-io-gateway .
```

CI scans the resolved runtime dependency set with pinned Trivy releases,
publishes the JSON report, and rejects unaccepted Critical or High findings.
See [dependency security](docs/DEPENDENCY_SECURITY.md) for local reproduction,
scanner scope, and the time-bounded exception process.

The build has no local JAR dependency and succeeds from a clean clone. FIXTURE
mode uses the small HTTP contract documented in
[`docs/FIXTURE_POSTCODE_CONTRACT.md`](docs/FIXTURE_POSTCODE_CONTRACT.md); do not
restore generated binaries or create another repository for it.

## Branch workflow and troubleshooting

Use `feature/* → develop`; `main` will be introduced later. Confirm the active
provider mode before diagnosing lookups. Production must never use fixture mode
and tests should not make uncontrolled live calls.

## Licence

Copyright © 2026 Bernard McGeever. All rights reserved.

This repository contains proprietary software belonging to Bernard McGeever.
It may not be used, copied, modified or distributed without express written
permission. See [LICENSE](./LICENSE).
