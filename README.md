# Postcode.io Gateway

Spring Boot boundary for postcode/outcode lookup. LIVE mode calls
`api.postcodes.io`; FIXTURE mode calls system-data-service for deterministic
test data.

> Beta status: not beta-ready. Fixture mode has an unapproved generated binary
> dependency and live-provider resilience/error mapping is incomplete. See
> [the audit](docs/BETA_READINESS_AUDIT.md).

## Requirements and configuration

- Java 17 and Maven 3.9

| Variable | Local default | Purpose |
|---|---|---|
| `SERVER_PORT` | `8082` | HTTP port |
| `EXTERNAL_PROVIDER_MODE` | `LIVE` | `LIVE` or `FIXTURE` |
| `SYSTEM_DATA_SERVICE_URL` | `http://localhost:8103` | Fixture dependency |
| `FIXTURE_DATASET_ID` / `FIXTURE_DATASET_VERSION` / `FIXTURE_SCENARIO` | demo values | Deterministic fixture selection |

No provider credential is currently required by postcodes.io. Do not add one
to source.

## API, health and build

- `GET /api/postcodes/{postcode}`
- `GET /internal/provider-mode`
- `/v3/api-docs`, `/swagger-ui/index.html`, `/actuator/health`

```bash
mvn -B verify
mvn spring-boot:run
docker build -t postcode-io-gateway .
```

The clean build fails until POSTCODE-01 removes the local system-data client
JAR. Do not create an additional repository without approval.

## Branch workflow and troubleshooting

Use `feature/* → develop`; `main` will be introduced later. Confirm the active
provider mode before diagnosing lookups. Production must never use fixture mode
and tests should not make uncontrolled live calls.

## Licence

Copyright © 2026 Bernard McGeever. All rights reserved.

This repository contains proprietary software belonging to Bernard McGeever.
It may not be used, copied, modified or distributed without express written
permission. See [LICENSE](./LICENSE).
