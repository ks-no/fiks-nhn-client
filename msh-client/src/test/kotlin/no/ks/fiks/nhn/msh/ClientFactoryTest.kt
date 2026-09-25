package no.ks.fiks.nhn.msh

import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.throwable.shouldHaveMessage
import no.ks.fiks.helseid.Environment
import java.text.ParseException
import java.util.*

class ClientFactoryTest : StringSpec() {

    init {
        "Create default client" {
            ClientFactory.createClient(
                Configuration(
                    helseId = HelseIdConfiguration(
                        environment = Environment(
                            issuer = "http://helseid:8080",
                            audience = UUID.randomUUID().toString(),
                        ),
                        clientId = UUID.randomUUID().toString(),
                        jwk = RSAKeyGenerator(2048).generate().toRSAKey().toString(),
                    ),
                    mshBaseUrl = "http://msh:8080",
                    sourceSystem = UUID.randomUUID().toString(),
                ),
            )
        }

        "JWK should be validated" {
            shouldThrow<ParseException> {
                ClientFactory.createClient(
                    Configuration(
                        helseId = HelseIdConfiguration(
                            environment = Environment(
                                issuer = "http://helseid:8080",
                                audience = UUID.randomUUID().toString(),
                            ),
                            clientId = UUID.randomUUID().toString(),
                            jwk = UUID.randomUUID().toString(),
                        ),
                        mshBaseUrl = "http://msh:8080",
                        sourceSystem = UUID.randomUUID().toString(),
                    ),
                )
            } shouldHaveMessage "Invalid JSON object"
        }

        "Create client with fastlege lookup" {
            ClientFactory.createClientWithFastlegeLookup(
                ConfigurationWithFastlegeLookup(
                    helseId = HelseIdConfiguration(
                        environment = Environment(
                            issuer = "http://helseid:8080",
                            audience = UUID.randomUUID().toString(),
                        ),
                        clientId = UUID.randomUUID().toString(),
                        jwk = RSAKeyGenerator(2048).generate().toRSAKey().toString(),
                    ),
                    adresseregister = AdresseregisterConfiguration(
                        url = "http://adresseregister:8080",
                        credentials = Credentials(
                            username = UUID.randomUUID().toString(),
                            password = UUID.randomUUID().toString(),
                        ),
                    ),
                    fastlegeregister = FastlegeregisterConfiguration(
                        url = "http://fastlegeregister:8080",
                        credentials = Credentials(
                            username = UUID.randomUUID().toString(),
                            password = UUID.randomUUID().toString(),
                        ),
                    ),
                    mshBaseUrl = "http://msh:8080",
                    sourceSystem = UUID.randomUUID().toString(),
                ),
            )

        }

    }

}
