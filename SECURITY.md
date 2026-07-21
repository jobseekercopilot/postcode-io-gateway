# Security policy

This repository is private. Report suspected vulnerabilities privately to the
repository owner. Do not open a public issue or include credentials, tokens,
personal data, exploit details, or production logs in an issue.

Do not commit secrets. Use runtime environment variables or the approved
secret-management mechanism. If a credential may have been exposed, stop its
use, report the type and affected location without reproducing its value, and
arrange rotation with the owner.

The current code is a beta-readiness baseline, not a security certification.
Known risks and beta blockers are tracked in `docs/BETA_READINESS_AUDIT.md`.

CI scans resolved runtime dependencies and fails when its machine-readable
report contains an unaccepted Critical or High vulnerability. Exceptions must
be linked to a private repository issue, expire within 30 days, and be removed
when the finding is remediated. See `docs/DEPENDENCY_SECURITY.md` for the full
policy and safe local reproduction steps.
