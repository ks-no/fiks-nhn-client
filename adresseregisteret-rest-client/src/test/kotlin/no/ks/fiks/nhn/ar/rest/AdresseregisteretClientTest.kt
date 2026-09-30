package no.ks.fiks.nhn.ar.rest

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Duration
import java.time.OffsetDateTime
import no.ks.fiks.nhn.ar.rest.model.CertificateMetadata
import no.ks.fiks.nhn.ar.rest.model.AdministrativeCode
import no.ks.fiks.nhn.ar.rest.model.CommunicationParty
import no.ks.fiks.nhn.ar.rest.model.CommunicationPartyType
import no.ks.fiks.nhn.ar.rest.model.OrganizationDetails
import no.ks.fiks.nhn.ar.rest.model.ParentOrganization
import no.ks.fiks.nhn.ar.rest.model.PersonDetails
import no.ks.fiks.nhn.ar.rest.model.PostalAddress
import no.ks.fiks.nhn.ar.rest.model.ServiceDetails

class AdresseregisteretClientTest : FreeSpec({
    "lookupHerId" - {
        "returns organization responses in spec format" {
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
            val result = client.lookupHerId(123)!!

            result.herId shouldBe 123
            result.name shouldBe "Test Organisasjon"
            result.type shouldBe CommunicationPartyType.ORGANIZATION
            result.organizationDetails!!.organizationNumber shouldBe "123456789"
            result.postalAddress!!.address shouldBe "Testgata 1"
            result.postalAddress!!.postalBox shouldBe "Postboks 2"
            result.postalAddress!!.postalCode shouldBe "0123"
            result.postalAddress!!.city shouldBe "Oslo"
        }

        "returns person responses in spec format" {
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
            val result = client.lookupHerId(456)!!
            val parent = result.personDetails!!.parentOrganization

            result.herId shouldBe 456
            result.name shouldBe "Ada Maria Lovelace"
            result.type shouldBe CommunicationPartyType.PERSON
            parent.herId shouldBe 321
            parent.name shouldBe "Parent Organization"
            parent.organizationNumber shouldBe "987654321"
            result.postalAddress!!.postalCode shouldBe "0123"
        }

        "returns service responses in spec format" {
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
            val result = client.lookupHerId(789)!!
            val parent = result.serviceDetails!!.parentOrganization

            result.herId shouldBe 789
            result.name shouldBe "Laboratorietjeneste"
            result.type shouldBe CommunicationPartyType.SERVICE
            parent.herId shouldBe 321
            parent.name shouldBe "Parent Organization"
            parent.organizationNumber shouldBe "987654321"
            result.postalAddress!!.address shouldBe "Testgata 1"
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

        "returns independent copies for cached lookups" {
            val service = mockk<AdresseregisteretService>()
            every { service.getCommunicationPartyDetails(778) } returns organizationResponse(778)
                .organizationDetails(
                    OrganizationDetails()
                        .organizationNumber("123456789")
                        .persons(listOf(10, 11))
                        .services(listOf(20, 21))
                )

            val client = AdresseregisteretClient(
                service = service,
                cacheConfig = CacheConfig(cacheTtl = Duration.ofMinutes(1)),
            )

            val first = client.lookupHerId(778)!!
            first.name("Mutated name")
            first.postalAddress!!.address("Changed address")

            val second = client.lookupHerId(778)!!

            second.name shouldBe "Test Organisasjon"
            second.postalAddress!!.address shouldBe "Testgata 1"
            verify(exactly = 1) { service.getCommunicationPartyDetails(778) }
        }

        "preserves offset date fields when returning cached copies" {
            val service = mockk<AdresseregisteretService>()
            val validFrom = OffsetDateTime.parse("2026-09-30T12:34:56+05:30")
            val validTo = OffsetDateTime.parse("2026-10-01T01:02:03-04:00")
            val signingValidFrom = OffsetDateTime.parse("2026-09-15T08:00:00+02:00")
            val signingValidTo = OffsetDateTime.parse("2027-09-15T08:00:00+02:00")

            every { service.getCommunicationPartyDetails(779) } returns organizationResponse(779)
                .validFrom(validFrom)
                .validTo(validTo)
                .currentSigningCertificate(
                    CertificateMetadata()
                        .thumbprint("thumbprint-1")
                        .validFrom(signingValidFrom)
                        .validTo(signingValidTo)
                )

            val client = AdresseregisteretClient(
                service = service,
                cacheConfig = CacheConfig(cacheTtl = Duration.ofMinutes(1)),
            )

            val first = client.lookupHerId(779)!!
            val second = client.lookupHerId(779)!!

            first.validFrom shouldBe validFrom
            first.validTo shouldBe validTo
            first.currentSigningCertificate!!.validFrom shouldBe signingValidFrom
            first.currentSigningCertificate!!.validTo shouldBe signingValidTo

            second.validFrom shouldBe validFrom
            second.validTo shouldBe validTo
            second.currentSigningCertificate!!.validFrom shouldBe signingValidFrom
            second.currentSigningCertificate!!.validTo shouldBe signingValidTo
            verify(exactly = 1) { service.getCommunicationPartyDetails(779) }
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
        "returns generated postal address on happy path" {
            val service = mockk<AdresseregisteretService>()
            every { service.getCommunicationPartyDetails(123) } returns organizationResponse(123)

            val client = AdresseregisteretClient(service)
            val result = client.lookupPostalAddress(123)

            result.address shouldBe "Testgata 1"
            result.postalBox shouldBe "Postboks 2"
            result.postalCode shouldBe "0123"
            result.city shouldBe "Oslo"
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

            exception.message shouldBe "Could not find any postal addresses related to herId"
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

