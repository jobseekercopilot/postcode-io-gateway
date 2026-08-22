# Provider-mode safety

## Explicit deployment contract

Every runtime must set both `DEPLOYMENT_ENVIRONMENT_CLASS` and
`EXTERNAL_PROVIDER_MODE`. Neither has a default. Missing, blank, unknown, or
unsafe values stop application startup before it accepts traffic.

| Environment class | Allowed provider mode | Intended use |
|---|---|---|
| `LOCAL` | `FIXTURE` or `LIVE` | Explicit developer choice |
| `TEST` | `FIXTURE` only | Automated tests without uncontrolled external calls |
| `DEMO` | `FIXTURE` only | Deterministic demonstrations |
| `STAGING` | `LIVE` only | Production-like provider integration |
| `PRODUCTION` | `LIVE` only | Production provider integration |

Spring profile names do not determine this policy. This avoids aliases or
misspellings such as `prod-like` silently bypassing a profile-name check.
Deployment owners must classify the environment explicitly and treat a startup
failure as a configuration error, not work around it by changing the class.

## Local and Compose examples

For deterministic local development:

```yaml
environment:
  DEPLOYMENT_ENVIRONMENT_CLASS: LOCAL
  EXTERNAL_PROVIDER_MODE: FIXTURE
  SYSTEM_DATA_SERVICE_URL: http://system-data-service:8103
```

For an explicit local call to postcodes.io, use `LOCAL` with `LIVE`. Automated
test and demo manifests must use `TEST`/`DEMO` with `FIXTURE`; staging and
production manifests must use `STAGING`/`PRODUCTION` with `LIVE`.

## Diagnostic endpoint

`/internal/provider-mode` is absent by default. Set
`PROVIDER_MODE_ENDPOINT_ENABLED=true` only for a local, test, or demo diagnostic
session. Startup fails if it is enabled for `STAGING` or `PRODUCTION`. Do not
route `/internal/**` through a public ingress even in an allowed environment.

When enabled, the endpoint reports the environment class, provider mode,
fixture identifiers and whether external calls are active. It returns no
credentials or provider response data.

## Rollout and ownership

The repository owner owns the allowed matrix. Deployment owners must set the two
required variables in each manifest and verify a deliberate invalid combination
fails before rollout. Changing the matrix requires tests, documentation, and a
security review because it controls whether deterministic fixture data or an
external provider is used.
