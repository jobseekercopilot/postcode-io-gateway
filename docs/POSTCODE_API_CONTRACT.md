# Public postcode API contract

The postcode gateway owns the validation and error contract consumed by the
location gateway. The endpoint is:

```text
GET /api/postcodes/{postcode}
```

## Accepted input and canonical form

`postcode` must be a syntactically valid UK full postcode or outward code
(outcode). Input is limited to 16 characters before normalisation. The gateway
removes whitespace, converts ASCII letters to uppercase, and inserts one space
before the inward-code portion of a full postcode.

Examples:

| Submitted | Canonical provider value | Kind |
|---|---|---|
| `sw1a1aa` | `SW1A 1AA` | full postcode |
| ` LS1  1UR ` | `LS1 1UR` | full postcode |
| `ec1a` | `EC1A` | outcode |
| `GIR 0AA` | `GIR 0AA` | special full postcode |

Null, empty, partial, oversized, non-ASCII, or structurally invalid values are
rejected before any provider call. Validation proves syntax, not that a
postcode exists; a syntactically valid but unknown postcode receives `404`.

## Responses

A successful lookup returns `200` and the existing `PostcodeLocation` JSON
shape. A failure returns JSON with stable `status`, `code`, `message`, and
`correlationId` fields:

```json
{
  "status": 404,
  "code": "POSTCODE_NOT_FOUND",
  "message": "No location was found for the supplied postcode.",
  "correlationId": "request-123"
}
```

| HTTP | Code | Meaning and consumer action |
|---|---|---|
| `400` | `INVALID_POSTCODE` | Correct the submitted postcode/outcode before retrying. |
| `404` | `POSTCODE_NOT_FOUND` | The valid input has no provider result; do not retry unchanged. |
| `429` | `PROVIDER_RATE_LIMITED` | The provider remained throttled after the gateway's bounded retry policy; retry later. |
| `502` | `PROVIDER_BAD_RESPONSE` | A transport, decoding, or unusable upstream response ended the lookup; retry according to the caller's budget. |
| `503` | `PROVIDER_UNAVAILABLE` | The gateway's provider circuit is open; retry later. |
| `504` | `PROVIDER_TIMEOUT` | The bounded provider deadline expired; retry according to the caller's budget. |
| `500` | `INTERNAL_ERROR` | An unexpected internal failure occurred; use the correlation ID for support. |

Messages are deliberately redacted and are part of the public contract. They
do not echo the submitted postcode, upstream URI/body, or exception detail.
Logs likewise record only the redacted route, outcome, error class, and
correlation ID—not postcode values or provider payloads.

## Correlation IDs and ownership

Clients may send `X-Correlation-Id` using 1–128 characters from
`A-Z`, `a-z`, `0-9`, `.`, `_`, `:`, and `-`. Missing or unsafe values are
replaced with a generated UUID. The effective value is returned in the header
and failure body.

The postcode gateway owns validation, canonicalisation, provider-failure
translation, and these stable public codes. Location and browser consumers own
their presentation and retry decisions and must branch on `status`/`code`, not
message text. The provider URI/schema boundary and monitoring policy are
documented in [the compatibility contract](PROVIDER_COMPATIBILITY.md).
POSTCODE-06 remains responsible for the full HTTP-stub and browser/location
journey.
