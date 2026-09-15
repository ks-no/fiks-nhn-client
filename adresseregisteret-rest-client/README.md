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
import no.ks.fiks.nhn.ar.rest.AdresseregisteretClient
import no.ks.fiks.nhn.ar.rest.AdresseregisteretService

val jwk = "<JWK string>"
val client = AdresseregisteretClient(
    service = AdresseregisteretService(
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
- The public Kotlin API is intentionally close to the existing SOAP client to minimize the migration effort.
- Use the generated `AdresseregisteretRestService`/`AdresseregisteretRestClient` directly if you want to work closer to the generated OpenAPI types.
