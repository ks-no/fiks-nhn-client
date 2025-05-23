package no.ks.fiks.nhn.ar

import com.github.tomakehurst.wiremock.client.WireMock.*
import io.kotest.assertions.asClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.beNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import io.kotest.matchers.types.shouldBeInstanceOf
import org.wiremock.integrations.testcontainers.WireMockContainer
import java.nio.charset.Charset
import java.util.*

class LookupHerIdHttpTest : StringSpec() {

    private val wireMock = WireMockContainer("wiremock/wiremock:3.12.1")
        .also {
            it.start()
            configureFor(it.port)
            stubFor(
                post("/")
                    .withRequestBody(or(containing("<herId>1</herId>"), containing("<herId>3</herId>")))
                    .willReturn(
                        aResponse()
                            .withHeader("Content-Type", "application/soap+xml; charset=utf-8")
                            .withBody(ClassLoader.getSystemResource("get-communication-party-details-no-parent-response.xml").readBytes())
                    )
            )
            stubFor(
                post("/")
                    .withRequestBody(containing("<herId>2</herId>"))
                    .willReturn(
                        aResponse()
                            .withHeader("Content-Type", "application/soap+xml; charset=utf-8")
                            .withBody(ClassLoader.getSystemResource("get-communication-party-details-with-parent-response.xml").readBytes())
                    )
            )
        }

    init {
        "Test lookup without parent" {
            val username = UUID.randomUUID().toString()
            val password = UUID.randomUUID().toString()
            val environment = Environment(wireMock.baseUrl)

            AdresseregisteretClient(environment, Credentials(username, password))
                .lookupHerId(1)
                .asClue {
                    it.shouldBeInstanceOf<OrganizationCommunicationParty>()
                    it.herId shouldBe 12345
                    it.parent should beNull()
                    it.physicalAddresses shouldHaveSize 1
                    with(it.physicalAddresses.single()) {
                        type shouldBe Adressetetype.POSTADRESSE
                        streetAddress shouldBe "Testveien 123"
                        postbox should beNull()
                        postalCode shouldBe "5020"
                        city shouldBe "BERGEN"
                        country should beNull()
                    }
                    it.organizationNumber shouldBe "123456789"
                }
        }

        "Test lookup with parent" {
            val username = UUID.randomUUID().toString()
            val password = UUID.randomUUID().toString()
            val environment = Environment(wireMock.baseUrl)

            AdresseregisteretClient(environment, Credentials(username, password))
                .lookupHerId(2)
                .asClue {
                    it.shouldBeInstanceOf<OrganizationCommunicationParty>()
                    it.herId shouldBe 55555
                    it.parent shouldNot beNull()
                    it.parent!!.herId shouldBe 987654
                    it.parent!!.name shouldBe "Overordnet Organisasjon"
                    it.physicalAddresses shouldHaveSize 1
                    with(it.physicalAddresses.single()) {
                        type shouldBe Adressetetype.POSTADRESSE
                        streetAddress shouldBe "Rådhusplassen 1"
                        postbox should beNull()
                        postalCode shouldBe "0037"
                        city shouldBe "OSLO"
                        country shouldBe "Norge"
                    }
                    it.organizationNumber shouldBe "888333444"
                }
        }

        "Test that configuration is applied correctly" {
            val username = UUID.randomUUID().toString()
            val password = UUID.randomUUID().toString()
            val environment = Environment(wireMock.baseUrl)

            AdresseregisteretClient(environment, Credentials(username, password))
                .lookupHerId(3)

            val requests = findAll(
                postRequestedFor(urlEqualTo("/"))
                    .withRequestBody(containing("<herId>3</herId>"))
            )
            requests shouldHaveSize 1
            requests.single().asClue { request ->
                request.absoluteUrl shouldBe "${environment.url}/"
                val usernamePassword = Base64.getDecoder()
                    .decode(
                        request.header("Authorization").firstValue()
                            .removePrefix("Basic ")
                    )
                    .toString(Charset.defaultCharset())
                usernamePassword shouldBe "$username:$password"
            }
        }

    }

}
