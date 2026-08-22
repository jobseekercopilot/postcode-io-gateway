# Service operations

## Verification

From a clean clone with Java 17, Maven 3.9 and Docker:

```bash
mvn -B clean verify
./scripts/test-dependency-report-policy.sh
./scripts/verify-container.sh
```

The container validator runs all tests, builds only from the verified JAR,
starts LIVE mode with a disposable unreachable provider, confirms non-root
read-only operation, opens the circuit through a bounded synthetic request,
proves readiness fails closed, and proves graceful SIGTERM exit. CI also scans
the rebuilt image for unaccepted Critical/High OS and library findings.

## Startup and shutdown

Set `DEPLOYMENT_ENVIRONMENT_CLASS` and `EXTERNAL_PROVIDER_MODE` explicitly and
follow the approved matrix in [provider-mode security](PROVIDER_MODE_SECURITY.md).
The process allows 20 seconds for graceful lifecycle shutdown; orchestrators
must allow at least 25 seconds before forcible termination.

```bash
curl --fail http://localhost:8082/actuator/health
curl --fail http://localhost:8082/actuator/health/readiness
```

Both responses are deliberately redacted. Readiness is passive and never
generates an external lookup.

## Troubleshooting

- Readiness non-UP: inspect circuit metrics, bounded provider outcomes,
  upstream reachability, DNS/TLS and timeouts. Wait for the half-open probe;
  do not restart all replicas or log request values.
- Startup failure: check the environment/mode matrix and bounded resilience
  configuration. Never switch production-like environments to FIXTURE.
- `429`, `502`, `503`, `504`: use the stable public code and correlation ID;
  inspect aggregate signals without copying provider bodies or URLs to logs.
- Missing telemetry: check the environment-owned private exporter and alert
  routing. `/actuator/metrics` is intentionally not public.

Service owners maintain application signals, resilience policy and runbooks.
Platform owners maintain private export, resource limits, network policy,
dashboard/alert delivery and image/dependency scan operations. POSTCODE-06
remains the separate full HTTP-boundary and browser-journey testing blocker.
