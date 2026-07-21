# Observability and readiness contract

## Health and readiness

`GET /actuator/health` and `GET /actuator/health/readiness` return aggregate
status only. Component names, provider URLs, query/postcode values, response
content and exception details are never returned. Readiness combines the
application readiness state with a passive provider signal: LIVE mode is
`OUT_OF_SERVICE` while the existing provider circuit is open and returns to a
probe-ready state when the circuit becomes half-open. The check never calls
postcodes.io or the fixture service, so probes cannot create provider traffic,
consume rate limits or amplify an outage.

FIXTURE readiness confirms that the safely validated fixture-mode client is
installed; its downstream availability is observed through request outcomes.
Provider failure does not change liveness.

## Metrics and privacy

The bounded provider meters and tags are defined in
[the resilience policy](PROVIDER_RESILIENCE.md). Postcode/outcode/query values,
URLs, bodies, coordinates, credentials, tokens, correlation IDs and exception
text are forbidden as labels. `/actuator/metrics` is intentionally not exposed
by default. Platform owners must connect the registry to an approved private
exporter rather than adding a public metrics route.

## Dashboard and alerts

The beta dashboard must show logical request outcomes and rate, latency
percentiles, retry/cache behavior, circuit state/transitions, aggregate
readiness, HTTP status rates, JVM/process health and container restarts. It
must include release annotations and links to this contract and the operations
runbook. No panel may contain request values or provider payloads.

Initial alerts are:

| Signal | Initial trigger | Response |
|---|---|---|
| Readiness | non-UP for 2 minutes | Page owner; inspect circuit and provider reachability |
| Provider failures | failure plus circuit-open outcomes above 5% for 10 minutes, minimum 20 calls | Page owner and check upstream status |
| Circuit | open for 2 minutes | Page owner; do not restart every replica |
| Latency | p95 above 2 seconds for 10 minutes, minimum 20 calls | Notify owner; inspect upstream latency/retries |
| Telemetry silence | no samples for 5 minutes while traffic is expected | Notify operations; inspect process/exporter |

Platform owners must deploy the private exporter, dashboard, alert routing and
prove firing, acknowledgement and recovery in the target environment. This
repository specifies the required signals but does not claim those external
controls are deployed.
