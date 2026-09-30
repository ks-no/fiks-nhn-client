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
import no.ks.fiks.ar.rest.model.CommunicationParty
import no.ks.fiks.ar.rest.model.PostalAddress
import no.ks.fiks.nhn.ar.rest.AdresseregisteretClient
import no.ks.fiks.nhn.ar.rest.AdresseregisteretService

val jwk = "<JWK string>"
val client = AdresseregisteretClient(
    service = AdresseregisteretService(
        url = "https://cpapi.test.grunndata.nhn.no",
        helseIdConfiguration = HelseIdConfiguration(
            clientId = clientId,
            jwk = jwk,
            environment = Environment(
                issuer = issuer,
                audience = audience,
            ),
        ),
    ),
)

val party: CommunicationParty? = client.lookupHerId(12345)
val postalAddress: PostalAddress = client.lookupPostalAddress(12345)
```

## Notes

- The REST client is generated from `openapi/adresseregister-spec.json`.
- `lookupHerId` and `lookupPostalAddress` return the generated OpenAPI models directly.
