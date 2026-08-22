# Place-name search contract

The postcode gateway owns the external Postcodes.io place-search boundary used
by location-gateway. The repository endpoint is:

```text
GET /api/places?q={place-name}&limit={1..10}
```

`q` is trimmed and internal whitespace is collapsed. It must contain 2–80
Unicode letters/marks, spaces, apostrophes, hyphens or periods. Control
characters, URI delimiters and other punctuation are rejected. `limit`
defaults to 10 and cannot exceed 10. Invalid input returns the normal redacted
`400 INVALID_PLACE_SEARCH` error before any provider request.

In LIVE mode the gateway calls the provider's documented
`GET /places?q=...&limit=...` endpoint. Query construction uses Spring's URI
builder, so user input remains an encoded query value. Automated tests use a
local exchange fixture and never contact the external provider.

Successful responses are arrays containing only:

- persistent place `id`;
- primary display `name`;
- representative outward `postcode`;
- `region` and optional `adminDistrict`;
- `latitude` and `longitude`.

An empty provider result is a successful empty array. Required identity,
region or coordinate fields that are missing, a non-200 response envelope, or
more results than requested are incompatible provider responses and fail
closed. Additive fields are ignored.

Search shares the LIVE provider's bounded per-attempt/total deadlines, retry
policy, circuit and low-cardinality outcome metrics. Query values and provider
bodies are never logged or used as metric labels. FIXTURE mode intentionally
returns `503 CAPABILITY_UNAVAILABLE`: its separately owned system-data-service
contract contains postcode records but no approved place dataset. Deterministic
cross-service search tests use LOCAL/LIVE mode with a loopback provider stub.

The external behavior is based on the provider's official place-query
documentation: <https://postcodes.io/docs/place/query/>.
