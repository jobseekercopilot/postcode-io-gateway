# Beta-readiness audit: Postcode.io gateway

Audit date: 18 July 2026

Status: **Not beta-ready.** POSTCODE-01 through POSTCODE-05, POSTCODE-07 and POSTCODE-08
are remediated: the service builds without a local generated binary, LIVE calls
are bounded by explicit resilience policy, provider mode fails closed, and CI
blocks unaccepted Critical/High runtime dependency findings, the public
validation/error contract is stable, and provider compatibility is monitored.
Full journey testing and the other findings below remain incomplete.

## Findings

| ID | Finding | Evidence | Risk and severity | Recommended solution and acceptance criteria | Dependencies | Beta blocker | Effort |
|---|---|---|---|---|---|---|---|
| [POSTCODE-01](https://github.com/jobseekercopilot/postcode-io-gateway/issues/1) | Remove the out-of-scope `system-data-service` binary dependency | **Remediated:** Maven, Docker and source no longer reference the generated client or `libs`; FIXTURE mode uses a minimal repository-owned HTTP response contract with focused mapping/failure tests. | The Critical clean-build and undeclared binary dependency risk is resolved; fixture environment controls remain tracked by POSTCODE-05. | Keep the documented fixture response fields compatible and retain clean-clone/contract verification. | No external repository or package was introduced. | No | L |
| [POSTCODE-02](https://github.com/jobseekercopilot/postcode-io-gateway/issues/2) | Add provider timeout, retry, circuit and cache policy | **Remediated:** LIVE calls use connect/response/total deadlines, bounded jittered retry, a half-open circuit, capped positive/negative caches and Micrometer signals. | The High cascading-outage risk is bounded; per-instance state and external provider behavior remain documented residual risks. | Retain the tested budgets and tune them only with observed metrics and documented ownership. | Service-owned objective and alert starting points in `docs/PROVIDER_RESILIENCE.md`. | No | L |
| [POSTCODE-03](https://github.com/jobseekercopilot/postcode-io-gateway/issues/3) | Validate requests and preserve meaningful status codes | **Remediated:** a bounded UK postcode/outcode validator canonicalises input before provider access; stable redacted responses distinguish 400/404/429/502/503/504 and include a safe correlation ID. | The High ambiguous-error/input-handling risk is resolved; end-to-end consumer behavior remains tracked by POSTCODE-06. | Retain the documented contract and focused validator/controller/security tests. | Gateway-owned location error contract in `docs/POSTCODE_API_CONTRACT.md`. | No | M |
| [POSTCODE-04](https://github.com/jobseekercopilot/postcode-io-gateway/issues/4) | Build provider URIs safely and monitor API compatibility | **Remediated:** validated values use URI-builder path segments; focused fixtures pin the minimum postcode/outcode response identity and additive-field behavior; incompatible responses increment a bounded metric. | The path-construction and silent schema-drift risks are resolved; an unversioned upstream can still break before operators react. | Retain fixtures, reviewed provider links, compatibility signal and documented change process. | Completed POSTCODE-03 and repository-owned compatibility contract. | No | S |
| [POSTCODE-05](https://github.com/jobseekercopilot/postcode-io-gateway/issues/5) | Fail closed for fixture/live mode | **Remediated:** provider mode and environment class are explicit; a startup policy enforces safe combinations; diagnostics are opt-in and forbidden in production-like classes. | The High configuration risk is resolved; deployment manifests must continue to set both explicit values. | Retain the documented matrix, startup/context tests and restricted diagnostics default. | Repository-owned convention in `docs/PROVIDER_MODE_SECURITY.md`. | No | M |
| [POSTCODE-06](https://github.com/jobseekercopilot/postcode-io-gateway/issues/6) | Expand provider and security testing | Unit-level LIVE resilience tests and MVC tests now cover validation, stable error mapping, redaction, and safe correlation IDs; no real LIVE HTTP stub or full location/browser failure journey exists. | **High / P1 testing:** the real HTTP boundary and complete consumer path remain incompletely proven. | Add WireMock-equivalent integration/contract tests and include the full location/browser journey. | POSTCODE-04 plus completed POSTCODE-02/03/05. | Yes | M |
| [POSTCODE-07](https://github.com/jobseekercopilot/postcode-io-gateway/issues/7) | Harden container, health, telemetry and docs | **Remediated:** aggregate passive provider/circuit readiness, operational signal/alert contracts, graceful shutdown and a test-enforcing digest-pinned non-root image with blocking image scan are present. | The repository operational baseline is complete; private exporter and target-environment alert delivery remain platform validation. | Retain runtime/readiness/privacy tests and prove alert delivery in the controlled beta environment. | POSTCODE-01/02 complete. | Yes | M |
| [POSTCODE-08](https://github.com/jobseekercopilot/postcode-io-gateway/issues/8) | Establish reliable dependency vulnerability scanning | **Remediated:** CI scans Maven's resolved runtime libraries with pinned Trivy/action revisions, retains JSON evidence and applies a tested fail-closed policy. | The High dependency-gate risk is resolved; one Medium finding remains outside the Critical/High gate and advisory lag remains residual risk. | Keep scanner/action versions pinned and current; review reports and remove expired exceptions; retain policy negative tests. | Shared platform policy is documented in `docs/DEPENDENCY_SECURITY.md`. | No | M |

## POSTCODE-07 remediation evidence

- General health and provider-circuit readiness are aggregate-only and never
  perform an external lookup or expose provider/request/error details.
- Existing bounded request, latency, retry, cache, circuit and compatibility
  metrics now have an explicit private-export, dashboard and alert contract.
- Release validation runs the complete Maven suite before the image copies the
  verified JAR. The runtime base is digest-pinned, runs as `10001:10001`, is
  tested read-only through provider failure and graceful stop, and receives a
  blocking Critical/High OS and library image scan.
- README and operations documentation record the real API, configuration,
  develop-only workflow, proprietary licence, ownership and residual risks.

## POSTCODE-01 remediation evidence

- `pom.xml` has no `systemPath`, generated-client or local JAR dependency, and
  the runtime JAR no longer includes system-scoped artifacts.
- The Docker build no longer copies a `libs` directory.
- The FIXTURE client owns only the response fields it consumes and calls
  `/internal/fixtures/postcodes/{postcode}` with an encoded path segment.
- Focused tests verify response mapping, forward compatibility with unknown
  fields and propagation of fixture-service failures.
- Clean-clone Maven, container, paired-service and secret/dependency checks are
  recorded on the completing pull request and issue.

## POSTCODE-08 remediation evidence

- The verified pre-remediation runtime set contained 82 Java packages and 47
  Critical/High findings (4 Critical and 43 High).
- Spring Boot was upgraded from 3.2.0 to 4.1.0, springdoc-openapi to 3.0.3 and
  Lombok to 1.18.46. The Spring Boot 4 REST client/MVC test modules and package
  migrations were adopted.
- The post-remediation scan covered 110 packages with zero Critical or High
  findings. One Medium finding remains outside the approved gate.
- CI uses pinned Trivy and action revisions, caches advisory data, scans only
  Maven's resolved runtime dependency directory, uploads the JSON report, and
  applies a fail-closed policy after report generation.
- Policy tests reject Critical findings, malformed or uncovered reports, and
  missing, invalid or expired risk-exception metadata. Full details are in
  `docs/DEPENDENCY_SECURITY.md`.

At completion of POSTCODE-08, POSTCODE-01 and POSTCODE-08 were resolved while
POSTCODE-02 through POSTCODE-07 remained open.

## POSTCODE-05 remediation evidence

- `EXTERNAL_PROVIDER_MODE` no longer defaults to LIVE and
  `DEPLOYMENT_ENVIRONMENT_CLASS` has no default; missing, blank or unknown values
  fail configuration/startup.
- The documented matrix allows either mode only for explicit LOCAL use, requires
  FIXTURE for TEST/DEMO, and requires LIVE for STAGING/PRODUCTION.
- The internal provider-mode endpoint is absent by default, requires an explicit
  opt-in, and cannot be enabled in STAGING/PRODUCTION.
- Focused tests cover the allowed matrix, missing configuration, test-like live
  calls, production-like fixture use, production diagnostics, context startup,
  and endpoint opt-in.

At completion of POSTCODE-05, POSTCODE-01, POSTCODE-05 and POSTCODE-08 were
resolved while POSTCODE-02 through POSTCODE-04, POSTCODE-06 and POSTCODE-07
remained open.

## POSTCODE-02 remediation evidence

- Netty enforces a connection deadline and response deadline; Reactor enforces
  per-attempt and hard total deadlines around the complete logical call.
- Retry is capped and jittered, and applies only to safe transient GET failures
  such as timeouts, transport errors, 408/425/429, 5xx and unusable responses.
- A thread-safe circuit opens after consecutive failed logical calls, rejects
  during the open window and permits one half-open recovery probe.
- Bounded LRU positive and 404 caches use distinct TTLs and are checked before
  the circuit so unexpired known results remain available during an outage.
- Bounded-cardinality request, latency, retry, cache and circuit metrics have
  documented alert starting points and ownership.
- Focused tests cover 429 retry, timeout exhaustion, outage/open rejection,
  recovery, positive/negative cache expiry, metrics and unsafe configuration.

## POSTCODE-03 remediation evidence

- A single bounded validator accepts UK full postcodes and outcodes, removes
  whitespace, uppercases safely, and sends only the canonical form downstream.
- Empty, oversized, partial, non-ASCII, and structurally malformed inputs fail
  with a stable `400` before provider access.
- Provider absence, terminal rate limiting, transport/response failure, open
  circuit, and timeout map to stable redacted `404`, `429`, `502`, `503`, and
  `504` responses respectively.
- MVC and focused unit tests cover successful responses, full postcode/outcode
  canonicalisation, invalid and empty input, every public failure class,
  fixture `found=false`, correlation IDs, and response-detail redaction.
- Submitted postcodes are absent from request logs and error bodies; unsafe
  correlation IDs are replaced before use in logs or response headers.
- The location-consumer contract and ownership boundary are documented in
  `docs/POSTCODE_API_CONTRACT.md`.

## POSTCODE-04 remediation evidence

- LIVE postcode and outcode URLs use URI-builder path segments rather than
  concatenated strings; POSTCODE-03 validation remains before provider access.
- Successful responses must include the documented `200` envelope status and
  the matching `result.postcode` or `result.outcode` identity.
- Additive fields remain forward-compatible, while missing/mismatched identity
  increments `postcode.provider.compatibility.failures` and follows the existing
  bounded bad-response policy without logging provider payloads.
- Focused contract fixtures cover both official endpoint shapes, additive
  fields, schema drift, identity mismatch and exact provider paths.
- `docs/PROVIDER_COMPATIBILITY.md` records the reviewed official documentation,
  consumed contract, alert signal, change process, ownership and residual risk.

This resolves POSTCODE-01 through POSTCODE-05 and POSTCODE-08 only. POSTCODE-06
and POSTCODE-07 remain open, so the
service is still **not beta-ready**.

## LOC-02 provider support evidence

- The provider gateway exposes a bounded `/api/places` contract in LIVE mode
  and builds the official `/places?q=...&limit=...` URI with isolated encoded
  query parameters.
- Query/limit validation runs before provider access. Required output fields,
  result count, text lengths and coordinate ranges are checked; additive
  provider fields remain compatible and drift fails closed.
- Search shares provider deadlines, bounded transient retry, circuit and
  low-cardinality telemetry without logging or tagging query values.
- FIXTURE mode returns a stable `503` because its approved external fixture
  contract has no place dataset; deterministic tests use a loopback LIVE stub.
- Provider, controller and validator tests cover multiple/empty results,
  normalization, encoded queries, 429 retry, incompatible responses, redacted
  errors and fail-closed fixture behavior.

This supporting change does not close POSTCODE-06 or POSTCODE-07 and does not
make the service beta-ready.
