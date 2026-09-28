package no.ks.fiks.nhn.ar.rest

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Duration
import no.nhn.register.communicationparty.rest.model.AdministrativeCode
import no.nhn.register.communicationparty.rest.model.CommunicationParty
import no.nhn.register.communicationparty.rest.model.CommunicationPartyType
import no.nhn.register.communicationparty.rest.model.OrganizationDetails
import no.nhn.register.communicationparty.rest.model.ParentOrganization
import no.nhn.register.communicationparty.rest.model.PersonDetails
import no.nhn.register.communicationparty.rest.model.PostalAddress
import no.nhn.register.communicationparty.rest.model.ServiceDetails

class AdresseregisteretClientTest : FreeSpec({
    "lookupHerId" - {
        "maps organization responses to domain model" {
            val service = mockk<AdresseregisteretService>()
            every { service.getCommunicationPartyDetails(123) } returns CommunicationParty()
                .herId(123)
                .name("Test Organisasjon")
                .type(CommunicationPartyType.ORGANIZATION)
                .organizationDetails(
                    OrganizationDetails()
                        .organizationNumber("123456789")
                )
                .postalAddress(postalAddress())

            val client = AdresseregisteretClient(service)
            val result = client.lookupHerId(123).shouldBeInstanceOf<OrganizationCommunicationParty>()

            result.name shouldBe "Test Organisasjon"
            result.organizationNumber shouldBe "123456789"
            result.physicalAddresses.single().streetAddress shouldBe "Testgata 1"
            result.physicalAddresses.single().postbox shouldBe "Postboks 2"
            result.physicalAddresses.single().postalCode shouldBe "0123"
            result.physicalAddresses.single().city shouldBe "Oslo"
        }

        "maps person responses to domain model" {
            val service = mockk<AdresseregisteretService>()
            every { service.getCommunicationPartyDetails(456) } returns CommunicationParty()
                .herId(456)
                .name("Ada Maria Lovelace")
                .type(CommunicationPartyType.PERSON)
                .personDetails(
                    PersonDetails()
                        .hprNumber(78910)
                        .parentOrganization(parentOrganization())
                )
                .postalAddress(postalAddress())

            val client = AdresseregisteretClient(service)
            val result = client.lookupHerId(456).shouldBeInstanceOf<PersonCommunicationParty>()
            val parent = result.parent!!

            result.herId shouldBe 456
            result.name shouldBe "Ada Maria Lovelace"
            parent.herId shouldBe 321
            parent.name shouldBe "Parent Organization"
            parent.organizationNumber shouldBe "987654321"
            result.physicalAddresses.single().postalCode shouldBe "0123"
        }

        "maps service responses to domain model" {
            val service = mockk<AdresseregisteretService>()
            every { service.getCommunicationPartyDetails(789) } returns CommunicationParty()
                .herId(789)
                .name("Laboratorietjeneste")
                .type(CommunicationPartyType.SERVICE)
                .serviceDetails(
                    ServiceDetails()
                        .serviceType(AdministrativeCode().value("LAB").name("Laboratory"))
                        .parentOrganization(parentOrganization())
                )
                .postalAddress(postalAddress())

            val client = AdresseregisteretClient(service)
            val result = client.lookupHerId(789).shouldBeInstanceOf<ServiceCommunicationParty>()
            val parent = result.parent!!

            result.herId shouldBe 789
            result.name shouldBe "Laboratorietjeneste"
            parent.herId shouldBe 321
            parent.name shouldBe "Parent Organization"
            parent.organizationNumber shouldBe "987654321"
            result.physicalAddresses.single().streetAddress shouldBe "Testgata 1"
        }

        "throws AddressNotFoundException when API returns not found" {
            val service = mockk<AdresseregisteretService>()
            every { service.getCommunicationPartyDetails(404) } throws mockk<feign.FeignException.NotFound>()

            val client = AdresseregisteretClient(service)

            val exception = shouldThrow<AddressNotFoundException> {
                client.lookupHerId(404)
            }

            exception.message shouldBe "Could not find any communication party related to herId"
        }

        "throws AdresseregisteretApiException with statusCode on API error" {
            val service = mockk<AdresseregisteretService>()
            val feignException = mockk<feign.FeignException>(relaxed = true)
            every { feignException.status() } returns 500
            every { feignException.message } returns "Internal Server Error"
            every { service.getCommunicationPartyDetails(123) } throws feignException

            val client = AdresseregisteretClient(service)

            val exception = shouldThrow<AdresseregisteretApiException> {
                client.lookupHerId(123)
            }

            exception.statusCode shouldBe 500
            exception.message shouldBe "Error from Adresseregisteret REST API: Internal Server Error"
            exception.cause shouldBe feignException
        }

        "wraps unknown exceptions in AdresseregisteretException" {
            val service = mockk<AdresseregisteretService>()
            val cause = IllegalStateException("boom")
            every { service.getCommunicationPartyDetails(500) } throws cause

            val client = AdresseregisteretClient(service)

            val exception = shouldThrow<AdresseregisteretException> {
                client.lookupHerId(500)
            }

            exception.message shouldBe "Unknown error from Adresseregisteret REST API"
            exception.cause shouldBe cause
        }

        "caches successful lookups when cache is enabled" {
            val service = mockk<AdresseregisteretService>()
            every { service.getCommunicationPartyDetails(777) } returns organizationResponse(777)

            val client = AdresseregisteretClient(
                service = service,
                cacheConfig = CacheConfig(cacheTtl = Duration.ofMinutes(1)),
            )

            client.lookupHerId(777)
            client.lookupHerId(777)

            verify(exactly = 1) { service.getCommunicationPartyDetails(777) }
        }

        "does not cache when cache is disabled" {
            val service = mockk<AdresseregisteretService>()
            every { service.getCommunicationPartyDetails(888) } returns organizationResponse(888)

            val client = AdresseregisteretClient(service)

            client.lookupHerId(888)
            client.lookupHerId(888)

            verify(exactly = 2) { service.getCommunicationPartyDetails(888) }
        }

        "does not cache missing lookups when cache loader returns null" {
            val service = mockk<AdresseregisteretService>()
            every { service.getCommunicationPartyDetails(999) } returns null

            val client = AdresseregisteretClient(
                service = service,
                cacheConfig = CacheConfig(cacheTtl = Duration.ofMinutes(1)),
            )

            client.lookupHerId(999) shouldBe null
            client.lookupHerId(999) shouldBe null

            verify(exactly = 2) { service.getCommunicationPartyDetails(999) }
        }
    }

    "lookupPostalAddress" - {
        "returns postal address on happy path" {
            val service = mockk<AdresseregisteretService>()
            every { service.getCommunicationPartyDetails(123) } returns organizationResponse(123)

            val client = AdresseregisteretClient(service)
            val result = client.lookupPostalAddress(123)

            result.name shouldBe "Test Organisasjon"
            result.streetAddress shouldBe "Testgata 1"
            result.postbox shouldBe "Postboks 2"
            result.postalCode shouldBe "0123"
            result.city shouldBe "Oslo"
            result.country shouldBe null
        }

        "throws AddressNotFoundException when no communication party exists" {
            val service = mockk<AdresseregisteretService>()
            every { service.getCommunicationPartyDetails(321) } returns null

            val client = AdresseregisteretClient(service)

            val exception = shouldThrow<AddressNotFoundException> {
                client.lookupPostalAddress(321)
            }

            exception.message shouldBe "Did not find any communication party related to herId"
        }

        "throws AddressNotFoundException when communication party has no addresses" {
            val service = mockk<AdresseregisteretService>()
            every { service.getCommunicationPartyDetails(654) } returns organizationResponse(654)
                .postalAddress(null)

            val client = AdresseregisteretClient(service)

            val exception = shouldThrow<AddressNotFoundException> {
                client.lookupPostalAddress(654)
            }

            exception.message shouldBe "Could not find any physical addresses related to herId"
        }
    }
})

private fun organizationResponse(herId: Int) = CommunicationParty()
    .herId(herId)
    .name("Test Organisasjon")
    .type(CommunicationPartyType.ORGANIZATION)
    .organizationDetails(
        OrganizationDetails()
            .organizationNumber("123456789")
    )
    .postalAddress(postalAddress())

private fun postalAddress() = PostalAddress()
    .address("Testgata 1")
    .postalBox("Postboks 2")
    .postalCode("0123")
    .city("Oslo")

private fun parentOrganization() = ParentOrganization()
    .herId(321)
    .name("Parent Organization")
    .organizationNumber("987654321")

