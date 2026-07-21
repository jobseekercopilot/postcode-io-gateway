# Beta-readiness audit: Postcode.io gateway

Audit date: 18 July 2026

Status: **Not beta-ready.** POSTCODE-01 and POSTCODE-08 are remediated: the
service builds without a local generated binary and CI now blocks unaccepted
Critical/High runtime dependency findings. Live-provider resilience/error
handling and the other findings below remain incomplete.

## Findings

| ID | Finding | Evidence | Risk and severity | Recommended solution and acceptance criteria | Dependencies | Beta blocker | Effort |
|---|---|---|---|---|---|---|---|
| [POSTCODE-01](https://github.com/jobseekercopilot/postcode-io-gateway/issues/1) | Remove the out-of-scope `system-data-service` binary dependency | **Remediated:** Maven, Docker and source no longer reference the generated client or `libs`; FIXTURE mode uses a minimal repository-owned HTTP response contract with focused mapping/failure tests. | The Critical clean-build and undeclared binary dependency risk is resolved; fixture environment controls remain tracked by POSTCODE-05. | Keep the documented fixture response fields compatible and retain clean-clone/contract verification. | No external repository or package was introduced. | No | L |
| [POSTCODE-02](https://github.com/jobseekercopilot/postcode-io-gateway/issues/2) | Add provider timeout, retry, circuit and cache policy | `PostcodeIoApiClient` creates a default WebClient and calls `retrieve()` with no deadline/backoff/circuit/cache. | **High / P1 reliability:** provider latency/outage can consume resources and cascade. | Set deadlines, bounded jittered retry for safe failures, circuit break, cache/negative-cache and metrics; test outage/429/timeout/recovery. | Provider SLO and monitoring. | Yes | L |
| [POSTCODE-03](https://github.com/jobseekercopilot/postcode-io-gateway/issues/3) | Validate requests and preserve meaningful status codes | `postcode.replaceAll` assumes non-null; no postcode format/length validation; controller converts every error to empty 500. | **High / P1 API:** invalid/not-found/rate-limit/outage are indistinguishable and clients cannot recover correctly. | Canonicalise with a documented validator; map 400/404/429/502/503/504 to stable redacted errors; test full/outcode/invalid/empty/partial. | Location error contract. | Yes | M |
| [POSTCODE-04](https://github.com/jobseekercopilot/postcode-io-gateway/issues/4) | Build provider URIs safely and monitor API compatibility | Paths are concatenated strings and response DTOs assume current provider shapes; no compatibility alert/test exists. | **Medium / P2 reliability/security:** unexpected encoded input or provider schema change causes failures. | Use URI builder/path segments, validate before construction, pin/document provider version assumptions and run contract fixtures. | POSTCODE-03. | No | S |
| [POSTCODE-05](https://github.com/jobseekercopilot/postcode-io-gateway/issues/5) | Fail closed for fixture/live mode | LIVE is default; fixture is rejected only when profile is exactly `prod`/`production`; provider-mode endpoint reveals mode. | **High / P1 configuration:** a misnamed environment can run fixture data in production or make live calls in tests. | Require explicit mode, validate an explicit environment class, restrict internal endpoint, fail startup on ambiguity and test prod-like configurations. | Deployment config convention. | Yes | M |
| [POSTCODE-06](https://github.com/jobseekercopilot/postcode-io-gateway/issues/6) | Expand provider and security testing | Eight tests cover controller/service behaviour and fixture HTTP mapping/encoding/failure; no real LIVE-provider stub, timeout, 429, malformed payload, cache, mode-safety or correlation propagation test exists. | **High / P1 testing:** live-provider boundary failure modes remain unproven. | Add WireMock-equivalent integration/contract tests and include full location/browser journey. | POSTCODE-02–05. | Yes | M |
| [POSTCODE-07](https://github.com/jobseekercopilot/postcode-io-gateway/issues/7) | Harden container, health, telemetry and docs | Docker skips tests and runs root/mutable tags; health has no provider readiness; README is skeletal. | **Medium / P1 operational/docs:** no deployable/diagnosable beta baseline. | Pin/non-root/scan image, run verify, expose safe readiness, metrics/alerts/graceful shutdown and complete operational/proprietary docs. | POSTCODE-01/02. | Yes | M |
| [POSTCODE-08](https://github.com/jobseekercopilot/postcode-io-gateway/issues/8) | Establish reliable dependency vulnerability scanning | **Remediated:** CI scans Maven's resolved runtime libraries with pinned Trivy/action revisions, retains JSON evidence and applies a tested fail-closed policy. | The High dependency-gate risk is resolved; one Medium finding remains outside the Critical/High gate and advisory lag remains residual risk. | Keep scanner/action versions pinned and current; review reports and remove expired exceptions; retain policy negative tests. | Shared platform policy is documented in `docs/DEPENDENCY_SECURITY.md`. | No | M |

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

This resolves POSTCODE-01 and POSTCODE-08 only. POSTCODE-02 through POSTCODE-07
remain open, so the service is still **not beta-ready**.
