package no.ks.fiks.nhn.ar.rest

import io.kotest.core.spec.style.StringSpec
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

class AdresseregisteretRestClientTest : StringSpec({
    "lookupHerId maps organization responses to domain model" {
        val service = mockk<AdresseregisteretRestService>()
        every { service.getCommunicationPartyDetails(123) } returns CommunicationParty()
            .herId(123)
            .name("Test Organisasjon")
            .type(CommunicationPartyType.ORGANIZATION)
            .organizationDetails(
                OrganizationDetails()
                    .organizationNumber("123456789")
            )
            .postalAddress(postalAddress())

        val client = AdresseregisteretRestClient(service)
        val result = client.lookupHerId(123).shouldBeInstanceOf<OrganizationCommunicationParty>()

        result.name shouldBe "Test Organisasjon"
        result.organizationNumber shouldBe "123456789"
        result.physicalAddresses.single().streetAddress shouldBe "Testgata 1"
        result.physicalAddresses.single().postbox shouldBe "Postboks 2"
        result.physicalAddresses.single().postalCode shouldBe "0123"
        result.physicalAddresses.single().city shouldBe "Oslo"
    }

    "lookupHerId maps person responses to domain model" {
        val service = mockk<AdresseregisteretRestService>()
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

        val client = AdresseregisteretRestClient(service)
        val result = client.lookupHerId(456).shouldBeInstanceOf<PersonCommunicationParty>()
        val parent = result.parent!!

        result.herId shouldBe 456
        result.name shouldBe "Ada Maria Lovelace"
        parent.herId shouldBe 321
        parent.name shouldBe "Parent Organization"
        parent.organizationNumber shouldBe "987654321"
        result.physicalAddresses.single().postalCode shouldBe "0123"
    }

    "lookupHerId maps service responses to domain model" {
        val service = mockk<AdresseregisteretRestService>()
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

        val client = AdresseregisteretRestClient(service)
        val result = client.lookupHerId(789).shouldBeInstanceOf<ServiceCommunicationParty>()
        val parent = result.parent!!

        result.herId shouldBe 789
        result.name shouldBe "Laboratorietjeneste"
        parent.herId shouldBe 321
        parent.name shouldBe "Parent Organization"
        parent.organizationNumber shouldBe "987654321"
        result.physicalAddresses.single().streetAddress shouldBe "Testgata 1"
    }

    "lookupHerId throws AddressNotFoundException when API returns not found" {
        val service = mockk<AdresseregisteretRestService>()
        every { service.getCommunicationPartyDetails(404) } throws mockk<feign.FeignException.NotFound>()

        val client = AdresseregisteretRestClient(service)

        val exception = shouldThrow<AddressNotFoundException> {
            client.lookupHerId(404)
        }

        exception.message shouldBe "Could not find any communication party related to herId"
    }

    "lookupHerId wraps unknown exceptions in AdresseregisteretException" {
        val service = mockk<AdresseregisteretRestService>()
        val cause = IllegalStateException("boom")
        every { service.getCommunicationPartyDetails(500) } throws cause

        val client = AdresseregisteretRestClient(service)

        val exception = shouldThrow<AdresseregisteretException> {
            client.lookupHerId(500)
        }

        exception.message shouldBe "Unknown error from Adresseregisteret REST API"
        exception.cause shouldBe cause
    }

    "lookupPostalAddress returns postal address on happy path" {
        val service = mockk<AdresseregisteretRestService>()
        every { service.getCommunicationPartyDetails(123) } returns organizationResponse(123)

        val client = AdresseregisteretRestClient(service)
        val result = client.lookupPostalAddress(123)

        result.name shouldBe "Test Organisasjon"
        result.streetAddress shouldBe "Testgata 1"
        result.postbox shouldBe "Postboks 2"
        result.postalCode shouldBe "0123"
        result.city shouldBe "Oslo"
        result.country shouldBe null
    }

    "lookupPostalAddress throws AddressNotFoundException when no communication party exists" {
        val service = mockk<AdresseregisteretRestService>()
        every { service.getCommunicationPartyDetails(321) } returns null

        val client = AdresseregisteretRestClient(service)

        val exception = shouldThrow<AddressNotFoundException> {
            client.lookupPostalAddress(321)
        }

        exception.message shouldBe "Did not find any communication party related to herId"
    }

    "lookupPostalAddress throws AddressNotFoundException when communication party has no addresses" {
        val service = mockk<AdresseregisteretRestService>()
        every { service.getCommunicationPartyDetails(654) } returns organizationResponse(654)
            .postalAddress(null)

        val client = AdresseregisteretRestClient(service)

        val exception = shouldThrow<AddressNotFoundException> {
            client.lookupPostalAddress(654)
        }

        exception.message shouldBe "Could not find any physical addresses related to herId"
    }

    "lookupHerId caches successful lookups when cache is enabled" {
        val service = mockk<AdresseregisteretRestService>()
        every { service.getCommunicationPartyDetails(777) } returns organizationResponse(777)

        val client = AdresseregisteretRestClient(
            service = service,
            cacheConfig = CacheConfig(cacheTtl = Duration.ofMinutes(1)),
        )

        client.lookupHerId(777)
        client.lookupHerId(777)

        verify(exactly = 1) { service.getCommunicationPartyDetails(777) }
    }

    "lookupHerId does not cache when cache is disabled" {
        val service = mockk<AdresseregisteretRestService>()
        every { service.getCommunicationPartyDetails(888) } returns organizationResponse(888)

        val client = AdresseregisteretRestClient(service)

        client.lookupHerId(888)
        client.lookupHerId(888)

        verify(exactly = 2) { service.getCommunicationPartyDetails(888) }
    }

    "lookupHerId does not cache missing lookups when cache loader returns null" {
        val service = mockk<AdresseregisteretRestService>()
        every { service.getCommunicationPartyDetails(999) } returns null

        val client = AdresseregisteretRestClient(
            service = service,
            cacheConfig = CacheConfig(cacheTtl = Duration.ofMinutes(1)),
        )

        client.lookupHerId(999) shouldBe null
        client.lookupHerId(999) shouldBe null

        verify(exactly = 2) { service.getCommunicationPartyDetails(999) }
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

