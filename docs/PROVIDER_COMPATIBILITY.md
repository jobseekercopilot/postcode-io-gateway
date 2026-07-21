# Postcodes.io compatibility contract

Reviewed against the official Postcodes.io documentation on 21 July 2026:

- [postcode lookup](https://postcodes.io/docs/postcode/lookup/)
- [outcode lookup](https://postcodes.io/docs/outcode/lookup/)

Postcodes.io does not expose a versioned URL for these operations. This gateway
therefore pins the small response contract it consumes instead of treating the
complete provider response as stable.

## Consumed contract

LIVE mode sends only validated, canonical values using URI-builder path
segments:

| Lookup | Request path | Minimum successful response |
|---|---|---|
| Full postcode | `/postcodes/{postcode}` | top-level `status` is `200`; `result.postcode` is present and identifies the requested postcode |
| Outcode | `/outcodes/{outcode}` | top-level `status` is `200`; `result.outcode` is present and identifies the requested outcode |

Unknown response properties are ignored so additive provider changes remain
compatible. A missing envelope status, missing identity, mismatched identity,
empty body, decoding failure, or other unusable response is a provider contract
failure. It follows the bounded retry/circuit policy and is exposed to callers
as the existing redacted `PROVIDER_BAD_RESPONSE` response.

The gateway increments `postcode.provider.compatibility.failures` for every
decoded response that violates the consumed contract. Alert on any sustained
non-zero rate and compare it with provider release/status information before
changing DTOs. This metric has no postcode, URI, or response-body label.

## Verification and change process

Focused contract fixtures cover both documented successful shapes, additive
unknown fields, missing identity, mismatched identity, and safe provider path
construction. Run them with:

```bash
mvn -B -Dtest=PostcodeIoApiClientTest test
```

Before accepting a provider schema change:

1. Review the official endpoint and schema documentation.
2. Add a redacted fixture that demonstrates the new shape and a negative test
   for the previous failure mode.
3. Preserve the public postcode gateway contract or coordinate a separately
   approved consumer change.
4. Run clean verification and dependency/security policy checks.

Residual risk: Postcodes.io can change without a versioned endpoint or advance
notice. Contract fixtures and the compatibility metric detect this; they do not
prevent an upstream breaking change.
