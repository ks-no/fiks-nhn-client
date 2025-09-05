package no.ks.fiks.nhn.ar

import com.github.tomakehurst.wiremock.client.WireMock
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
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*
import kotlin.random.Random.Default.nextInt

class LookupHerIdHttpTest : StringSpec() {

    private val wireMock = WireMockContainer("wiremock/wiremock:3.12.1")
        .also { it.start() }
    private val wireMockClient = WireMock(wireMock.host, wireMock.port)

    init {

        "Test lookup organzation" {
            val herId = nextInt(1, 100000)
            stubResponse(herId, "get-communication-party-details-organzation-response.xml")

            AdresseregisteretClient(AdresseregisteretService(wireMock.baseUrl, Credentials(UUID.randomUUID().toString(), UUID.randomUUID().toString())))
                .lookupHerId(herId)
                .asClue { party ->
                    party.shouldBeInstanceOf<OrganizationCommunicationParty>()
                    party.herId shouldBe 8142987
                    party.name shouldBe "KS-DIGITALE FELLESTJENESTER AS"
                    party.parent should beNull()
                    party.physicalAddresses shouldHaveSize 2
                    with(party.physicalAddresses.single { it.type == PostalAddressType.POSTADRESSE }) {
                        type shouldBe PostalAddressType.POSTADRESSE
                        streetAddress shouldBe "Haakon VIIs gate 9"
                        postbox should beNull()
                        postalCode shouldBe "0161"
                        city shouldBe "OSLO"
                        country should beNull()
                    }
                    with(party.physicalAddresses.single { it.type == PostalAddressType.BESOKSADRESSE }) {
                        type shouldBe PostalAddressType.BESOKSADRESSE
                        streetAddress shouldBe "Haakon VIIs gate 9"
                        postbox should beNull()
                        postalCode shouldBe "0161"
                        city shouldBe "OSLO"
                        country should beNull()
                    }
                    party.electronicAddresses shouldHaveSize 2
                    with(party.electronicAddresses[0]) {
                        type shouldBe AddressComponent.EDI
                        address shouldBe "meldingstjener-api@testedi.nhn.no"
                        lastChanged shouldBe OffsetDateTime.of(2025, 4, 30, 14, 13, 19, 10 * 1000000, ZoneOffset.ofHours(2))
                    }
                    with(party.electronicAddresses[1]) {
                        type shouldBe AddressComponent.DIGITALT_SERTIFIKAT
                        address!!.trim() shouldBe "ldap://ldap.test4.buypass.no/dc=Buypass,dc=no,CN=Buypass%20Class%203%20Test4%20CA%20G2?usercertificate;binary?sub?(|(certificateSerialNumber=1978614801669301597418371)(certificateSerialNumber=1978620698775221943443289))"
                        lastChanged shouldBe OffsetDateTime.of(2025, 4, 30, 14, 13, 19, 10 * 1000000, ZoneOffset.ofHours(2))
                    }
                }
        }

        "Test lookup person" {
            val herId = nextInt(1, 100000)
            stubResponse(herId, "get-communication-party-details-person-response.xml")

            AdresseregisteretClient(AdresseregisteretService(wireMock.baseUrl, Credentials(UUID.randomUUID().toString(), UUID.randomUUID().toString())))
                .lookupHerId(herId)
                .asClue { party ->
                    party.shouldBeInstanceOf<PersonCommunicationParty>()
                    party.herId shouldBe 115522
                    party.name shouldBe "Anne-Marie Bach"
                    party.parent shouldNot beNull()
                    party.parent!!.herId shouldBe 84556
                    party.parent.name shouldBe "Tysnes Helsesenter"
                    party.physicalAddresses shouldHaveSize 1
                    with(party.physicalAddresses.single()) {
                        type shouldBe PostalAddressType.POSTADRESSE
                        streetAddress shouldBe "uggdalsvegen 301"
                        postbox should beNull()
                        postalCode shouldBe "5685"
                        city shouldBe "UGGDAL"
                        country should beNull()
                    }
                    party.electronicAddresses shouldHaveSize 6
                    with(party.electronicAddresses[0]) {
                        type shouldBe AddressComponent.EPOST
                        address shouldBe "awibej@hotmail.com"
                        lastChanged shouldBe OffsetDateTime.of(2018, 2, 14, 10, 25, 2, 450 * 1000000, ZoneOffset.ofHours(1))
                    }
                    with(party.electronicAddresses[1]) {
                        type shouldBe AddressComponent.TELEFONNUMMER
                        address shouldBe "53437080"
                        lastChanged shouldBe OffsetDateTime.of(2018, 1, 4, 16, 24, 19, 880 * 1000000, ZoneOffset.ofHours(1))
                    }
                    with(party.electronicAddresses[2]) {
                        type shouldBe AddressComponent.SENTRALBORDNUMMER
                        address shouldBe "41763738"
                        lastChanged shouldBe OffsetDateTime.of(2020, 1, 27, 10, 31, 15, 370 * 1000000, ZoneOffset.ofHours(1))
                    }
                    with(party.electronicAddresses[3]) {
                        type shouldBe AddressComponent.HJEMMESIDE
                        address shouldBe "http://www.tysnes.kommune.no/tysnes-legekontor.5561952-329522.html"
                        lastChanged shouldBe OffsetDateTime.of(2016, 8, 4, 9, 28, 31, 613 * 1000000, ZoneOffset.ofHours(2))
                    }
                    with(party.electronicAddresses[4]) {
                        type shouldBe AddressComponent.EDI
                        address shouldBe "tysnes-kommune@edi.nhn.no"
                        lastChanged shouldBe OffsetDateTime.of(2016, 8, 4, 9, 28, 31, 613 * 1000000, ZoneOffset.ofHours(2))
                    }
                    with(party.electronicAddresses[5]) {
                        type shouldBe AddressComponent.FAXNUMMER
                        address shouldBe "53 43 70 81"
                        lastChanged shouldBe OffsetDateTime.of(2016, 8, 4, 9, 28, 31, 613 * 1000000, ZoneOffset.ofHours(2))
                    }
                }
        }

        "Test lookup service" {
            val herId = nextInt(1, 100000)
            stubResponse(herId, "get-communication-party-details-service-response.xml")

            AdresseregisteretClient(AdresseregisteretService(wireMock.baseUrl, Credentials(UUID.randomUUID().toString(), UUID.randomUUID().toString())))
                .lookupHerId(herId)
                .asClue { party ->
                    party.shouldBeInstanceOf<ServiceCommunicationParty>()
                    party.herId shouldBe 8094866
                    party.name shouldBe "Meldingsvalidering"
                    party.parent shouldNot beNull()
                    party.parent!!.herId shouldBe 112374
                    party.parent.name shouldBe "Norsk Helsenett SF"
                    party.physicalAddresses shouldHaveSize 2
                    with(party.physicalAddresses.single { it.type == PostalAddressType.POSTADRESSE }) {
                        type shouldBe PostalAddressType.POSTADRESSE
                        streetAddress shouldBe "Postboks 6123"
                        postbox should beNull()
                        postalCode shouldBe "7435"
                        city shouldBe "TRONDHEIM"
                        country shouldBe "Norge"
                    }
                    with(party.physicalAddresses.single { it.type == PostalAddressType.BESOKSADRESSE }) {
                        type shouldBe PostalAddressType.BESOKSADRESSE
                        streetAddress shouldBe "Abels gate 9"
                        postbox should beNull()
                        postalCode shouldBe "7030"
                        city shouldBe "TRONDHEIM"
                        country shouldBe "Norge"
                    }
                    party.electronicAddresses shouldHaveSize 7
                    with(party.electronicAddresses[0]) {
                        type shouldBe AddressComponent.EDI
                        address shouldBe "meldingsvalidator@samsvar.nhn.no"
                        lastChanged shouldBe OffsetDateTime.of(2016, 11, 14, 9, 27, 45, 130 * 1000000, ZoneOffset.ofHours(1))
                    }
                    with(party.electronicAddresses[1]) {
                        type shouldBe AddressComponent.DIGITALT_SERTIFIKAT
                        address!!.trim() shouldBe "ldap://ldap.buypass.no/dc=Buypass,dc=no,CN=Buypass%20Class%203%20CA?usercertificate;binary?sub?(|(certificateSerialNumber=429310064944711009829792)(certificateSerialNumber=429314915671661112195379))"
                        lastChanged shouldBe OffsetDateTime.of(2025, 2, 27, 15, 43, 36, 560 * 1000000, ZoneOffset.ofHours(1))
                    }
                    with(party.electronicAddresses[2]) {
                        type shouldBe AddressComponent.FAXNUMMER
                        address shouldBe "77286287"
                        lastChanged shouldBe OffsetDateTime.of(2020, 8, 17, 10, 25, 41, 330 * 1000000, ZoneOffset.ofHours(2))
                    }
                    with(party.electronicAddresses[3]) {
                        type shouldBe AddressComponent.EPOST
                        address shouldBe "kundesenter@nhn.no"
                        lastChanged shouldBe OffsetDateTime.of(2020, 8, 17, 10, 25, 41 , 330 * 1000000, ZoneOffset.ofHours(2))
                    }
                    with(party.electronicAddresses[4]) {
                        type shouldBe AddressComponent.TELEFONNUMMER
                        address shouldBe "24200000"
                        lastChanged shouldBe OffsetDateTime.of(2020, 8, 17, 10, 25, 41, 330 * 1000000, ZoneOffset.ofHours(2))
                    }
                    with(party.electronicAddresses[5]) {
                        type shouldBe AddressComponent.HJEMMESIDE
                        address shouldBe "http://www.nhn.no"
                        lastChanged shouldBe OffsetDateTime.of(2014, 12, 3, 9, 47, 6, 630 * 1000000, ZoneOffset.ofHours(1))
                    }
                    with(party.electronicAddresses[6]) {
                        type shouldBe AddressComponent.FHIR_ENDEPUNKT
                        address shouldBe "dette-er-en-fhir-adresse"
                        lastChanged shouldBe OffsetDateTime.of(2022, 2, 7, 14, 19, 58, 30 * 1000000, ZoneOffset.ofHours(1))
                    }
                }
        }

        "Test lookup without parent" {
            val herId = nextInt(1, 100000)
            stubResponse(herId, "get-communication-party-details-no-parent-response.xml")

            AdresseregisteretClient(AdresseregisteretService(wireMock.baseUrl, Credentials(UUID.randomUUID().toString(), UUID.randomUUID().toString())))
                .lookupHerId(herId)
                .asClue {
                    it.shouldBeInstanceOf<OrganizationCommunicationParty>()
                    it.herId shouldBe 12345
                    it.name shouldBe "ET LEGEKONTOR"
                    it.parent should beNull()
                    it.physicalAddresses shouldHaveSize 1
                    with(it.physicalAddresses.single()) {
                        type shouldBe PostalAddressType.POSTADRESSE
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

            AdresseregisteretClient(AdresseregisteretService(wireMock.baseUrl, Credentials(UUID.randomUUID().toString(), UUID.randomUUID().toString())))
                .lookupHerId(herId)
                .asClue {
                    it.shouldBeInstanceOf<OrganizationCommunicationParty>()
                    it.herId shouldBe 55555
                    it.name shouldBe "ET LEGEKONTOR"
                    it.parent shouldNot beNull()
                    it.parent!!.herId shouldBe 987654
                    it.parent.name shouldBe "Overordnet Organisasjon"
                    it.physicalAddresses shouldHaveSize 1
                    with(it.physicalAddresses.single()) {
                        type shouldBe PostalAddressType.POSTADRESSE
                        streetAddress shouldBe "Rådhusplassen 1"
                        postbox should beNull()
                        postalCode shouldBe "0037"
                        city shouldBe "OSLO"
                        country shouldBe "Norge"
                    }
                    it.organizationNumber shouldBe "888333444"
                }
        }

        "Test that error response is translated into AdresseregisteretException" {
            val herId = nextInt(1, 100000)
            stubResponse(herId, "get-communication-party-details-not-found-response.xml")

            shouldThrow<AdresseregisteretApiException> {
                AdresseregisteretClient(AdresseregisteretService(wireMock.baseUrl, Credentials(UUID.randomUUID().toString(), UUID.randomUUID().toString())))
                    .lookupHerId(herId)
            }.asClue {
                it.message shouldBe "Kommunikasjonspart med oppgitt HER-id eksisterer ikke. (reason)"
                it.faultMessage shouldBe "Kommunikasjonspart med oppgitt HER-id eksisterer ikke."
                it.errorCode shouldBe "InvalidHerIdSupplied"
            }
        }

    }

    private fun stubResponse(herId: Int, fileName: String) {
        wireMockClient.register(
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
