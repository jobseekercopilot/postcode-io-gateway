# Live provider resilience and service objective

## Request budget

The gateway owns this initial operational objective; it is not a guarantee from
postcodes.io. A logical LIVE lookup has a four-second total deadline. Each
network attempt has a 500 ms connection deadline and a one-second response
deadline. At most two retries are allowed, with 100–500 ms exponential backoff
and 50% jitter, so a single caller cannot retry indefinitely or synchronize a
retry storm with other callers.

Retries are limited to failures that can reasonably be transient and are safe
for GET: connection/response timeout, transport failure, unusable response,
HTTP 408, 425, 429 and 5xx. A 404 or another client error is not retried.
`PROVIDER_TOTAL_TIMEOUT` remains the hard outer limit even when attempts and
backoff would otherwise take longer.

All duration, retry, circuit and cache settings are configurable through the
environment variables in the README. Startup rejects non-positive or excessive
durations, more than five retries, jitter outside 0–1, a total deadline shorter
than the response deadline, circuit thresholds outside 1–100, or cache bounds
outside 1–100,000 entries.

## Circuit and cache policy

Five consecutive failed logical calls open the in-process circuit for 30
seconds. Calls fail immediately while open. After the open period, one
half-open probe is allowed; success closes the circuit and failure reopens it.
The circuit counts only transient/provider-availability failures, not valid 404
responses or other caller errors.

Successful responses are cached for 15 minutes and 404 responses for one
minute. The cache is local to each instance, uses normalized postcode/outcode
keys, is capped at 1,000 entries and evicts least-recently-used entries. Valid
cached responses are served before the circuit check. Cache contents are not
shared or persisted, and expired values are never served as stale data.

## Metrics and alert starting points

Micrometer registers only bounded-cardinality metrics; postcode values, URLs,
response bodies and credentials are never tags:

| Metric | Tags/values | Meaning |
|---|---|---|
| `postcode.provider.requests` | `outcome=success|not_found|failure|circuit_open` | Logical calls and their terminal outcome |
| `postcode.provider.latency` | same outcome tag | End-to-end logical-call duration |
| `postcode.provider.retries` | none | Individual retry attempts |
| `postcode.provider.cache.accesses` | `result=hit|negative_hit|miss` | Cache effectiveness |
| `postcode.provider.circuit.opens` | none | Transitions to open |
| `postcode.provider.circuit.state` | `0` closed, `1` half-open, `2` open | Current in-process state |

Attach the approved metrics registry before production rollout. For local
inspection only, add `metrics` to `MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE`;
deployment owners must keep `/actuator/**` off public ingress.

Use these initial alert thresholds until traffic establishes a better baseline:

- page if provider `failure` plus `circuit_open` exceeds 5% for 10 minutes with
  at least 20 logical calls;
- page if the circuit remains open for two minutes;
- warn if successful-call p95 exceeds two seconds for 10 minutes;
- warn if retries exceed 10% of logical calls for 15 minutes.

The service owner owns the request budget, alert tuning and capacity review.
The platform/deployment owner owns registry export, dashboards and routing.
Residual risks are per-instance cache/circuit state, upstream advisory or
latency changes, and callers still receiving generic 500 responses until
POSTCODE-03 implements the public error contract.
