# NHN Client

Uses the Meldingstjener REST API provided by NHN:
- [Documentation](https://utviklerportal.nhn.no/informasjonstjenester/meldingstjener/edi-20/edi-20-ekstern-docs/docs/meldingstjener-rest-apimd)
- [API spec](https://api.tjener.test.melding.nhn.no/swagger/index.html)

Test: https://api.tjener.test.melding.nhn.no  
Prod: TODO: Missing

Usage examples can be found in tests defined [here](src/test/kotlin/no/ks/fiks/nhn/examples).

The easiest way to create a client is using the `ClientFactory`, see examples.  
Four different clients are provided:
- `Client` - Standard client. Receiver HER-id has to be provided when sending messages. All functions are suspendable.
- `ClientWithFastlegeLookup` - In addition to everything `Client` can do, this can also send messages to the GP of a person identified by fødselsnummer, using Adresseregisteret and Fastlegeregisteret for lookup.
- `BlockingClient` - Provides the same functionality as `Client`, but all functions are blocking
- `BlockingClientWithFastlegeLookup` - Provides the same functionality as `ClientWithFastlegeLookup`, but all functions are blocking

## Notes

- The generated `msh-api-v2` client models are sourced from `../fiks-nhn-specs/specs/msh-api-v2.json`.
- Initialize/update git submodules before regenerating sources or building from a fresh clone.

The clients support using access tokens with organization numbers for use with single-/multi-tenancy as discussed [here](https://utviklerportal.nhn.no/informasjonstjenester/helseid/bruksmoenstre-og-eksempelkode/bruk-av-helseid/docs/tekniske-mekanismer/organisasjonsnumre_enmd).
These organization numbers can be configured at the client level, but can also be provided per request by using the `RequestParameters` parameter.


### Import using Maven
```xml
<dependency>
    <groupId>no.ks.fiks</groupId>
    <artifactId>msh-client</artifactId>
    <version>X.X.X</version>
</dependency>
```