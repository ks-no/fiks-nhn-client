# Adresseregisteret REST Client

This module exposes a REST-based client for the NHN Adresseregisteret API.
It is intentionally kept separate from the existing SOAP-based `adresseregisteret-client` module.

## Maven dependency

```xml
<dependency>
    <groupId>no.ks.fiks</groupId>
    <artifactId>adresseregister-rest-client</artifactId>
    <version>${project.version}</version>
</dependency>
```

## Example usage

```kotlin
import no.ks.fiks.nhn.ar.rest.AdresseregisteretClient
import no.ks.fiks.nhn.ar.rest.AdresseregisteretService
import no.ks.fiks.nhn.ar.rest.Credentials

val client = AdresseregisteretClient(
    service = AdresseregisteretService(
        url = "https://cpapi.test.grunndata.nhn.no",
        credentials = Credentials(
            username = username,
            password = password,
        ),
    ),
)

val party = client.lookupHerId(12345)
val postalAddress = client.lookupPostalAddress(12345)
```

## Notes

- The REST client is generated from `adresseregister-spec.json`.
- The public Kotlin API is intentionally close to the existing SOAP client to minimize the migration effort.
- Use the generated `AdresseregisteretRestService`/`AdresseregisteretRestClient` directly if you want to work closer to the generated OpenAPI types.
