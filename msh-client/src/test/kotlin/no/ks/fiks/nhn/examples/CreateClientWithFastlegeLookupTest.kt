package no.ks.fiks.nhn.examples

import io.kotest.core.spec.style.StringSpec
import no.ks.fiks.helseid.Environment
import no.ks.fiks.nhn.msh.*

class CreateClientWithFastlegeLookupTest : StringSpec({
    "! Create client with fastlege lookup using factory" {
        ClientFactory.createClientWithFastlegeLookup(
            ConfigurationWithFastlegeLookup(
                helseId = HelseIdConfiguration(
                    environment = Environment(
                        issuer = "<HelseID issuer/base URL>",
                        audience = "<HelseID audience>",
                    ),
                    clientId = "<HelseID client id>",
                    jwk = "<HelseID JWK string>",
                ),
                adresseregister = AdresseregisterConfiguration(
                    url = "<Adresseregister URL>",
                    credentials = Credentials(
                        username = "<AR username>",
                        password = "<AR password>",
                    ),
                ),
                fastlegeregister = FastlegeregisterConfiguration(
                    url = "<Fastlegeregister URL>",
                    credentials = Credentials(
                        username = "<FLR username>",
                        password = "<FLR password>",
                    ),
                ),
                mshBaseUrl = "<MSH base URL>",
                sourceSystem = "<Name describing the system using the client>",
            )
        )
    }
})