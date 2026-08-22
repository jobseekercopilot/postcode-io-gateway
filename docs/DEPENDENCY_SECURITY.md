# Dependency security

## Decision and scope

The service uses Trivy's open-source vulnerability scanner for Maven runtime
dependencies. Running this CI tool is compatible with the repository's
proprietary licence; it does not become part of the distributed service or
change the application's licence.

CI verifies the application, then asks Maven to materialise the resolved
runtime dependency set in `target/dependency-scan`. A pinned Trivy release
scans only that directory for library vulnerabilities. The pinned action caches
the advisory database and retains the complete JSON report for 30 days. The
policy validates the report schema and Java package coverage before rejecting
any unaccepted Critical or High finding.

This covers resolved Java runtime libraries, including transitive dependencies
used by the live and fixture provider clients. It does not scan the container
base image, operating system packages, build-only Maven plugins, source code,
infrastructure, or a deployed environment; those require separate controls.

## Remediation evidence

The initial Spring Boot 3.2.0 runtime scan covered 82 Java packages and found 4
Critical and 43 High findings. The service was upgraded to Spring Boot 4.1.0,
springdoc-openapi 3.0.3 and Lombok 1.18.46. This required the Spring Boot 4 REST
client and MVC test modules and package migrations.

The equivalent post-upgrade scan covered 110 packages with zero Critical and
zero High findings. One Medium finding remained; it is outside the approved
Critical/High gate and remains subject to routine dependency maintenance.
There are no active risk exceptions.

Spring Boot 4.1.0 manages Netty 4.2.15.Final by default. The beta-stack image
scan subsequently identified five fixed High findings in that release:
CVE-2026-59901, CVE-2026-55831, CVE-2026-55833, CVE-2026-56745 and
CVE-2026-56816. `pom.xml` therefore overrides Boot's `netty.version` property
to 4.2.16.Final, the fixed compatible 4.2.x release. Keep the override until a
future Spring Boot dependency BOM manages the same or a later fixed version;
verify the resolved `io.netty` dependency tree before removing it.

## Local verification

```bash
mvn -B clean verify
mvn -B dependency:tree -Dincludes=io.netty
./scripts/test-dependency-report-policy.sh

mvn -B dependency:copy-dependencies \
  -DincludeScope=runtime \
  -DoutputDirectory=target/dependency-scan

scan_dir="$(mktemp -d)"
docker run --rm \
  -v "$PWD/target/dependency-scan:/scan:ro" \
  -v "$PWD/config/trivy/.trivyignore:/.trivyignore:ro" \
  -v "$scan_dir:/output" \
  aquasec/trivy:0.72.0 rootfs \
  --scanners vuln --vuln-type library --list-all-pkgs \
  --severity UNKNOWN,LOW,MEDIUM,HIGH,CRITICAL \
  --format json --output /output/dependencies.json \
  --ignorefile /.trivyignore /scan

./scripts/verify-dependency-report.sh \
  "$scan_dir/dependencies.json" config/trivy/.trivyignore
```

Do not mount the repository or Docker socket into the scanner. Do not commit
reports or advisory caches: reports expose exact component versions and belong
in the private CI artifact store or a local temporary directory.

## Time-bounded risk exceptions

Remediation is the default. If a Critical or High finding cannot be fixed
immediately, the repository owner must assess it in a private
`jobseekercopilot/postcode-io-gateway` issue. Place its URL immediately before
one finding ID in `config/trivy/.trivyignore`, with an expiry no more than 30
days away:

```text
# Tracking: https://github.com/jobseekercopilot/postcode-io-gateway/issues/123
CVE-2099-12345 exp:2099-01-30
```

The issue must record affected versions, exploitability, compensating controls,
owner, remediation plan, and review date. CI rejects untracked, malformed,
expired, or overlong exceptions. Renewal requires a new assessment; remove the
entry as soon as remediation lands.

## Ownership and residual risk

The repository owner owns scanner/action/dependency upgrades, advisory review,
exception approval, and remediation. A clean report is not proof of absence:
advisory feeds can lag disclosure and the scan is limited to resolved Java
runtime dependencies. Dependency update review, container and secret scanning,
code review, and deployment controls remain necessary complementary safeguards.
