# Fastlegeregisteret Client

Uses the Fastlegeregisteret web service provided by NHN:
- https://register-web.test.nhn.no/docs/arkitektur/register/flr.html
- https://www.nhn.no/tjenester/fastlegeregisteret

Test:
- https://ws-web.test.nhn.no/v2/flr
Prod:
- https://ws.nhn.no/v2/flr

### Import using Maven
```xml
<dependency>
    <groupId>no.ks.fiks</groupId>
    <artifactId>fastlegeregisteret-client</artifactId>
    <version>X.X.X</version>
</dependency>
```

### Setting up the client
```kotlin
val client = FastlegeregisteretClient(
    url = Environment.TEST,
    credentials = Credentials(
        username = username,
        password = password,
    ),
)
```

### Getting GP for patient by fødselsnummer
```kotlin
val fnr = "12345678910"
val gp = client.getPatientGP(fnr)
```
