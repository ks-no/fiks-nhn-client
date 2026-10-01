package no.ks.fiks.nhn.ar.rest

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Duration
import java.time.OffsetDateTime
import no.ks.fiks.nhn.ar.rest.model.AmqpAddress
import no.ks.fiks.nhn.ar.rest.model.AmqpTransportStatus
import no.ks.fiks.nhn.ar.rest.model.CertificateMetadata
import no.ks.fiks.nhn.ar.rest.model.AdministrativeCode
import no.ks.fiks.nhn.ar.rest.model.CommunicationParty
import no.ks.fiks.nhn.ar.rest.model.CommunicationPartyType
import no.ks.fiks.nhn.ar.rest.model.InterMunicipalityCoverageArea
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
                .amqpTransportStatus(AmqpTransportStatus.DISABLED)
                .organizationDetails(
                    OrganizationDetails()
                        .businessType(administrativeCode("ORG", "Organization"))
                        .persons(emptyList())
                        .services(emptyList())
                        .organizationNumber("123456789")
                )
                .postalAddress(postalAddress())

            val client = AdresseregisteretClient(service, CacheConfig())
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
                .amqpTransportStatus(AmqpTransportStatus.DISABLED)
                .personDetails(
                    PersonDetails()
                        .hprNumber(78910)
                        .parentOrganization(parentOrganization())
                )
                .postalAddress(postalAddress())

            val client = AdresseregisteretClient(service, CacheConfig())
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
                .amqpTransportStatus(AmqpTransportStatus.DISABLED)
                .serviceDetails(
                    ServiceDetails()
                        .serviceType(administrativeCode("LAB", "Laboratory"))
                        .parentOrganization(parentOrganization())
                )
                .postalAddress(postalAddress())

            val client = AdresseregisteretClient(service, CacheConfig())
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

            val client = AdresseregisteretClient(service, CacheConfig())

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

            val client = AdresseregisteretClient(service, CacheConfig())

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

            val client = AdresseregisteretClient(service, CacheConfig())

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
                        .businessType(administrativeCode("ORG", "Organization"))
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

        "returns every value in CommunicationParty through cached lookups" {
            val service = mockk<AdresseregisteretService>()
            val validFrom = OffsetDateTime.parse("2026-01-02T03:04:05+01:00")
            val validTo = OffsetDateTime.parse("2026-12-31T23:59:59+01:00")
            val signingValidFrom = OffsetDateTime.parse("2025-01-01T00:00:00+01:00")
            val signingValidTo = OffsetDateTime.parse("2027-01-01T00:00:00+01:00")
            val encryptionValidFrom = OffsetDateTime.parse("2025-02-01T00:00:00+01:00")
            val encryptionValidTo = OffsetDateTime.parse("2027-02-01T00:00:00+01:00")

            every { service.getCommunicationPartyDetails(111222) } returns CommunicationParty()
                .herId(111222)
                .name("Full Test Organization")
                .displayName("Display Name")
                .type(CommunicationPartyType.SERVICE)
                .organizationDetails(
                    OrganizationDetails()
                        .organizationNumber("999888777")
                        .businessType(
                            AdministrativeCode()
                                .codeListId("3401")
                                .value("BUS")
                                .name("Business type")
                                .url("https://example.com/business-type")
                        )
                        .persons(listOf(1, 2, 3))
                        .services(listOf(4, 5, 6))
                )
                .personDetails(
                    PersonDetails()
                        .hprNumber(123456)
                        .parentOrganization(
                            ParentOrganization()
                                .name("Person Parent")
                                .herId(9001)
                                .organizationNumber("111222333")
                        )
                )
                .serviceDetails(
                    ServiceDetails()
                        .serviceType(
                            AdministrativeCode()
                                .codeListId("4402")
                                .value("LAB")
                                .name("Laboratory")
                                .url("https://example.com/service-type")
                        )
                        .interMunicipalityCoverageArea(
                            InterMunicipalityCoverageArea()
                                .municipalityHerIds(listOf(301, 302, 303))
                        )
                        .serviceSpecification("urn:test:service-specification")
                        .parentOrganization(
                            ParentOrganization()
                                .name("Service Parent")
                                .herId(9002)
                                .organizationNumber("444555666")
                        )
                )
                .currentSigningCertificate(
                    CertificateMetadata()
                        .thumbprint("sign-thumbprint")
                        .validFrom(signingValidFrom)
                        .validTo(signingValidTo)
                )
                .currentEncryptionCertificate(
                    CertificateMetadata()
                        .thumbprint("enc-thumbprint")
                        .validFrom(encryptionValidFrom)
                        .validTo(encryptionValidTo)
                )
                .email("full@test.no")
                .homepageUrl("https://example.no")
                .phoneNumber("+47 12 34 56 78")
                .faxNumber("+47 87 65 43 21")
                .ediAddress("edi-address")
                .fhirAddress("https://fhir.example.no")
                .postalAddress(
                    PostalAddress()
                        .address("Testgata 1")
                        .postalBox("Postboks 2")
                        .postalCode("0123")
                        .city("Oslo")
                )
                .amqpTransportStatus(AmqpTransportStatus.ENABLED)
                .amqpAddress(
                    AmqpAddress()
                        .amqpSyncQueue("sync-queue")
                        .amqpSyncReplyQueue("sync-reply-queue")
                        .amqpAsyncQueue("async-queue")
                        .amqpErrorQueue("error-queue")
                )
                .validFrom(validFrom)
                .validTo(validTo)

            val client = AdresseregisteretClient(
                service = service,
                cacheConfig = CacheConfig(cacheTtl = Duration.ofMinutes(1)),
            )

            val first = client.lookupHerId(111222)!!
            val second = client.lookupHerId(111222)!!

            first shouldBe second
            second.herId shouldBe 111222
            second.name shouldBe "Full Test Organization"
            second.displayName shouldBe "Display Name"
            second.type shouldBe CommunicationPartyType.SERVICE

            second.organizationDetails!!.organizationNumber shouldBe "999888777"
            second.organizationDetails!!.businessType.codeListId shouldBe "3401"
            second.organizationDetails!!.businessType.value shouldBe "BUS"
            second.organizationDetails!!.businessType.name shouldBe "Business type"
            second.organizationDetails!!.businessType.url shouldBe "https://example.com/business-type"
            second.organizationDetails!!.persons shouldBe listOf(1, 2, 3)
            second.organizationDetails!!.services shouldBe listOf(4, 5, 6)

            second.personDetails!!.hprNumber shouldBe 123456
            second.personDetails!!.parentOrganization.name shouldBe "Person Parent"
            second.personDetails!!.parentOrganization.herId shouldBe 9001
            second.personDetails!!.parentOrganization.organizationNumber shouldBe "111222333"

            second.serviceDetails!!.serviceType.codeListId shouldBe "4402"
            second.serviceDetails!!.serviceType.value shouldBe "LAB"
            second.serviceDetails!!.serviceType.name shouldBe "Laboratory"
            second.serviceDetails!!.serviceType.url shouldBe "https://example.com/service-type"
            second.serviceDetails!!.interMunicipalityCoverageArea!!.municipalityHerIds shouldBe listOf(301, 302, 303)
            second.serviceDetails!!.serviceSpecification shouldBe "urn:test:service-specification"
            second.serviceDetails!!.parentOrganization.name shouldBe "Service Parent"
            second.serviceDetails!!.parentOrganization.herId shouldBe 9002
            second.serviceDetails!!.parentOrganization.organizationNumber shouldBe "444555666"

            second.currentSigningCertificate!!.thumbprint shouldBe "sign-thumbprint"
            second.currentSigningCertificate!!.validFrom shouldBe signingValidFrom
            second.currentSigningCertificate!!.validTo shouldBe signingValidTo

            second.currentEncryptionCertificate!!.thumbprint shouldBe "enc-thumbprint"
            second.currentEncryptionCertificate!!.validFrom shouldBe encryptionValidFrom
            second.currentEncryptionCertificate!!.validTo shouldBe encryptionValidTo

            second.email shouldBe "full@test.no"
            second.homepageUrl shouldBe "https://example.no"
            second.phoneNumber shouldBe "+47 12 34 56 78"
            second.faxNumber shouldBe "+47 87 65 43 21"
            second.ediAddress shouldBe "edi-address"
            second.fhirAddress shouldBe "https://fhir.example.no"

            second.postalAddress!!.address shouldBe "Testgata 1"
            second.postalAddress!!.postalBox shouldBe "Postboks 2"
            second.postalAddress!!.postalCode shouldBe "0123"
            second.postalAddress!!.city shouldBe "Oslo"

            second.amqpTransportStatus shouldBe AmqpTransportStatus.ENABLED
            second.amqpAddress!!.amqpSyncQueue shouldBe "sync-queue"
            second.amqpAddress!!.amqpSyncReplyQueue shouldBe "sync-reply-queue"
            second.amqpAddress!!.amqpAsyncQueue shouldBe "async-queue"
            second.amqpAddress!!.amqpErrorQueue shouldBe "error-queue"

            second.validFrom shouldBe validFrom
            second.validTo shouldBe validTo
            verify(exactly = 1) { service.getCommunicationPartyDetails(111222) }
        }

        "does not cache when cache is disabled" {
            val service = mockk<AdresseregisteretService>()
            every { service.getCommunicationPartyDetails(888) } returns organizationResponse(888)

            val client = AdresseregisteretClient(
                service = service,
                cacheConfig = CacheConfig(cacheTtl = Duration.ZERO),
            )

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

            val client = AdresseregisteretClient(service, CacheConfig())
            val result = client.lookupPostalAddress(123)

            result.address shouldBe "Testgata 1"
            result.postalBox shouldBe "Postboks 2"
            result.postalCode shouldBe "0123"
            result.city shouldBe "Oslo"
        }

        "throws AddressNotFoundException when no communication party exists" {
            val service = mockk<AdresseregisteretService>()
            every { service.getCommunicationPartyDetails(321) } returns null

            val client = AdresseregisteretClient(service, CacheConfig())

            val exception = shouldThrow<AddressNotFoundException> {
                client.lookupPostalAddress(321)
            }

            exception.message shouldBe "Did not find any communication party related to herId"
        }

        "throws AddressNotFoundException when communication party has no addresses" {
            val service = mockk<AdresseregisteretService>()
            every { service.getCommunicationPartyDetails(654) } returns organizationResponse(654)
                .postalAddress(null)

            val client = AdresseregisteretClient(service, CacheConfig())

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
    .amqpTransportStatus(AmqpTransportStatus.DISABLED)
    .organizationDetails(
        OrganizationDetails()
            .businessType(administrativeCode("ORG", "Organization"))
            .persons(emptyList())
            .services(emptyList())
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

private fun administrativeCode(value: String, name: String) = AdministrativeCode()
    .codeListId("3401")
    .value(value)
    .name(name)

