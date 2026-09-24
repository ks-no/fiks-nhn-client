# Adresseregisteret REST Client

This module exposes a REST-based client for the NHN Adresseregisteret API.
It is intentionally kept separate from the existing SOAP-based `adresseregisteret-client` module.

## Maven dependency

```xml
<dependency>
    <groupId>no.ks.fiks</groupId>
    <artifactId>adresseregisteret-rest-client</artifactId>
    <version>${project.version}</version>
</dependency>
```

## Example usage

```kotlin
import no.ks.fiks.helseid.Configuration as HelseIdConfiguration
import no.ks.fiks.helseid.Environment
import no.ks.fiks.helseid.HelseIdClient
import no.ks.fiks.helseid.dpop.ProofBuilder
import no.ks.fiks.nhn.ar.rest.AdresseregisteretRestClient
import no.ks.fiks.nhn.ar.rest.AdresseregisteretRestService

val jwk = "<JWK string>"
val client = AdresseregisteretRestClient(
    service = AdresseregisteretRestService(
        url = "https://cpapi.test.grunndata.nhn.no",
        helseIdClient = HelseIdClient(
            HelseIdConfiguration(
                clientId = clientId,
                jwk = jwk,
                environment = Environment(
                    issuer = issuer,
                    audience = audience,
                ),
            ),
        ),
        proofBuilder = ProofBuilder(jwk),
    ),
)

val party = client.lookupHerId(12345)
val postalAddress = client.lookupPostalAddress(12345)
```

## Notes

- The REST client is generated from `adresseregister-spec.json`.
- The public Kotlin API is intentionally direct: use `AdresseregisteretRestClient` for lookups and `AdresseregisteretRestService` for transport/auth wiring.
