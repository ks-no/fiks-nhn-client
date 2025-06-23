# Adresseregisteret Client

Uses the Adresseregisteret web service provided by NHN:
- https://register-web.test.nhn.no/docs/arkitektur/register/ar.html
- https://www.nhn.no/tjenester/adresseregisteret

Test: https://ws-web.test.nhn.no/v1/Ar  
Prod: https://ws.nhn.no/v1/Ar

### Import using Maven
```xml
<dependency>
    <groupId>no.ks.fiks</groupId>
    <artifactId>adresseregisteret-client</artifactId>
    <version>X.X.X</version>
</dependency>
```

### Setting up the client
```kotlin
val client = AdresseregisteretClient(
    url = "https://ws-web.test.nhn.no/v1/Ar",
    credentials = Credentials(
        username = username,
        password = password,
    ),
)
```

### Getting details concerning communication party by HER-id
```kotlin
val herId = "12345"
val party = client.lookupHerId(herId)
```
