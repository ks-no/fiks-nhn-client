package no.ks.fiks.nhn.ar

import com.github.tomakehurst.wiremock.client.WireMock.*
import io.kotest.assertions.asClue
import io.kotest.assertions.throwables.shouldThrow
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
import kotlin.random.Random.Default.nextInt

class LookupHerIdHttpTest : StringSpec() {

    private val wireMock = WireMockContainer("wiremock/wiremock:3.12.1")
        .also {
            it.start()
            configureFor(it.port)
        }

    init {

        "Test lookup organzation" {
            val herId = nextInt(1, 100000)
            stubResponse(herId, "get-communication-party-details-organzation-response.xml")

            AdresseregisteretClient(Environment(wireMock.baseUrl), Credentials(UUID.randomUUID().toString(), UUID.randomUUID().toString()))
                .lookupHerId(herId)
                .asClue { party ->
                    party.shouldBeInstanceOf<OrganizationCommunicationParty>()
                    party.herId shouldBe 8142987
                    party.name shouldBe "KS-DIGITALE FELLESTJENESTER AS"
                    party.parent should beNull()
                    party.physicalAddresses shouldHaveSize 2
                    with(party.physicalAddresses.single { it.type == AddressType.POSTADRESSE }) {
                        type shouldBe AddressType.POSTADRESSE
                        streetAddress shouldBe "Haakon VIIs gate 9"
                        postbox should beNull()
                        postalCode shouldBe "0161"
                        city shouldBe "OSLO"
                        country should beNull()
                    }
                    with(party.physicalAddresses.single { it.type == AddressType.BESOKSADRESSE }) {
                        type shouldBe AddressType.BESOKSADRESSE
                        streetAddress shouldBe "Haakon VIIs gate 9"
                        postbox should beNull()
                        postalCode shouldBe "0161"
                        city shouldBe "OSLO"
                        country should beNull()
                    }
                }
        }

        "Test lookup person" {
            val herId = nextInt(1, 100000)
            stubResponse(herId, "get-communication-party-details-person-response.xml")

            AdresseregisteretClient(Environment(wireMock.baseUrl), Credentials(UUID.randomUUID().toString(), UUID.randomUUID().toString()))
                .lookupHerId(herId)
                .asClue { party ->
                    party.shouldBeInstanceOf<PersonCommunicationParty>()
                    party.herId shouldBe 115522
                    party.name shouldBe "Anne-Marie Bach"
                    party.parent shouldNot beNull()
                    party.parent!!.herId shouldBe 84556
                    party.parent!!.name shouldBe "Tysnes Helsesenter"
                    party.physicalAddresses shouldHaveSize 1
                    with(party.physicalAddresses.single()) {
                        type shouldBe AddressType.POSTADRESSE
                        streetAddress shouldBe "uggdalsvegen 301"
                        postbox should beNull()
                        postalCode shouldBe "5685"
                        city shouldBe "UGGDAL"
                        country should beNull()
                    }
                }
        }

        "Test lookup service" {
            val herId = nextInt(1, 100000)
            stubResponse(herId, "get-communication-party-details-service-response.xml")

            AdresseregisteretClient(Environment(wireMock.baseUrl), Credentials(UUID.randomUUID().toString(), UUID.randomUUID().toString()))
                .lookupHerId(herId)
                .asClue { party ->
                    party.shouldBeInstanceOf<ServiceCommunicationParty>()
                    party.herId shouldBe 8094866
                    party.name shouldBe "Meldingsvalidering"
                    party.parent shouldNot beNull()
                    party.parent!!.herId shouldBe 112374
                    party.parent!!.name shouldBe "Norsk Helsenett SF"
                    party.physicalAddresses shouldHaveSize 2
                    with(party.physicalAddresses.single { it.type == AddressType.POSTADRESSE }) {
                        type shouldBe AddressType.POSTADRESSE
                        streetAddress shouldBe "Postboks 6123"
                        postbox should beNull()
                        postalCode shouldBe "7435"
                        city shouldBe "TRONDHEIM"
                        country shouldBe "Norge"
                    }
                    with(party.physicalAddresses.single { it.type == AddressType.BESOKSADRESSE }) {
                        type shouldBe AddressType.BESOKSADRESSE
                        streetAddress shouldBe "Abels gate 9"
                        postbox should beNull()
                        postalCode shouldBe "7030"
                        city shouldBe "TRONDHEIM"
                        country shouldBe "Norge"
                    }
                }
        }

        "Test lookup without parent" {
            val herId = nextInt(1, 100000)
            stubResponse(herId, "get-communication-party-details-no-parent-response.xml")

            AdresseregisteretClient(Environment(wireMock.baseUrl), Credentials(UUID.randomUUID().toString(), UUID.randomUUID().toString()))
                .lookupHerId(herId)
                .asClue {
                    it.shouldBeInstanceOf<OrganizationCommunicationParty>()
                    it.herId shouldBe 12345
                    it.name shouldBe "ET LEGEKONTOR"
                    it.parent should beNull()
                    it.physicalAddresses shouldHaveSize 1
                    with(it.physicalAddresses.single()) {
                        type shouldBe AddressType.POSTADRESSE
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
            val herId = nextInt(1, 100000)
            stubResponse(herId, "get-communication-party-details-with-parent-response.xml")

            AdresseregisteretClient(Environment(wireMock.baseUrl), Credentials(UUID.randomUUID().toString(), UUID.randomUUID().toString()))
                .lookupHerId(herId)
                .asClue {
                    it.shouldBeInstanceOf<OrganizationCommunicationParty>()
                    it.herId shouldBe 55555
                    it.name shouldBe "ET LEGEKONTOR"
                    it.parent shouldNot beNull()
                    it.parent!!.herId shouldBe 987654
                    it.parent!!.name shouldBe "Overordnet Organisasjon"
                    it.physicalAddresses shouldHaveSize 1
                    with(it.physicalAddresses.single()) {
                        type shouldBe AddressType.POSTADRESSE
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

            val herId = nextInt(1, 100000)
            stubResponse(herId, "get-communication-party-details-no-parent-response.xml")

            AdresseregisteretClient(environment, Credentials(username, password))
                .lookupHerId(herId)

            val requests = findAll(
                postRequestedFor(urlEqualTo("/"))
                    .withRequestBody(containing("<herId>$herId</herId>"))
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

        "Test that configuration is applied correctly when using builder" {
            val username = UUID.randomUUID().toString()
            val password = UUID.randomUUID().toString()
            val environment = Environment(wireMock.baseUrl)

            val herId = nextInt(1, 100000)
            stubResponse(herId, "get-communication-party-details-no-parent-response.xml")

            AdresseregisteretClientBuilder()
                .environment(environment)
                .credentials(Credentials(username, password))
                .build()
                .lookupHerId(herId)

            val requests = findAll(
                postRequestedFor(urlEqualTo("/"))
                    .withRequestBody(containing("<herId>$herId</herId>"))
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

        "Test that configuration is applied correctly when using builder, using url instead of environment" {
            val username = UUID.randomUUID().toString()
            val password = UUID.randomUUID().toString()

            val herId = nextInt(1, 100000)
            stubResponse(herId, "get-communication-party-details-no-parent-response.xml")

            AdresseregisteretClientBuilder()
                .url(wireMock.baseUrl)
                .credentials(Credentials(username, password))
                .build()
                .lookupHerId(herId)

            val requests = findAll(
                postRequestedFor(urlEqualTo("/"))
                    .withRequestBody(containing("<herId>$herId</herId>"))
            )
            requests shouldHaveSize 1
            requests.single().asClue { request ->
                request.absoluteUrl shouldBe "${wireMock.baseUrl}/"
                val usernamePassword = Base64.getDecoder()
                    .decode(
                        request.header("Authorization").firstValue()
                            .removePrefix("Basic ")
                    )
                    .toString(Charset.defaultCharset())
                usernamePassword shouldBe "$username:$password"
            }
        }

        "Test that error response is translated into AdresseregisteretException" {
            val herId = nextInt(1, 100000)
            stubResponse(herId, "get-communication-party-details-not-found-response.xml")

            shouldThrow<AdresseregisteretApiException> {
                AdresseregisteretClient(Environment(wireMock.baseUrl), Credentials(UUID.randomUUID().toString(), UUID.randomUUID().toString()))
                    .lookupHerId(herId)
            }.asClue {
                it.message shouldBe "Kommunikasjonspart med oppgitt HER-id eksisterer ikke. (reason)"
                it.faultMessage shouldBe "Kommunikasjonspart med oppgitt HER-id eksisterer ikke."
                it.errorCode shouldBe "InvalidHerIdSupplied"
            }
        }

    }

    private fun stubResponse(herId: Int, fileName: String) {
        stubFor(
            post("/")
                .withRequestBody(containing("<herId>$herId</herId>"))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/soap+xml; charset=utf-8")
                        .withBody(ClassLoader.getSystemResource(fileName).readBytes())
                )
        )
    }

}
