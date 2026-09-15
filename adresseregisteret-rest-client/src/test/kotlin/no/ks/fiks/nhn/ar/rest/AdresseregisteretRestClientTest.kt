package no.ks.fiks.nhn.ar.rest

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import no.nhn.register.communicationparty.rest.model.CommunicationParty
import no.nhn.register.communicationparty.rest.model.CommunicationPartyType
import no.nhn.register.communicationparty.rest.model.OrganizationDetails
import no.nhn.register.communicationparty.rest.model.PostalAddress

class AdresseregisteretRestClientTest : StringSpec({
    "mapping generated REST communication party to domain model" {
        val service = mockk<AdresseregisteretRestService>()
        every { service.getCommunicationPartyDetails(123) } returns CommunicationParty()
            .herId(123)
            .name("Test Organisasjon")
            .type(CommunicationPartyType.ORGANIZATION)
            .organizationDetails(
                OrganizationDetails()
                    .organizationNumber("123456789")
            )
            .postalAddress(
                PostalAddress()
                    .address("Testgata 1")
                    .postalBox("Postboks 2")
                    .postalCode("0123")
                    .city("Oslo")
            )

        val client = AdresseregisteretRestClient(service)
        val result = client.lookupHerId(123)

        result.shouldBeInstanceOf<OrganizationCommunicationParty>()
        result!!.name shouldBe "Test Organisasjon"
        result.organizationNumber shouldBe "123456789"
        result.physicalAddresses.single().streetAddress shouldBe "Testgata 1"
        result.physicalAddresses.single().postbox shouldBe "Postboks 2"
        result.physicalAddresses.single().postalCode shouldBe "0123"
        result.physicalAddresses.single().city shouldBe "Oslo"
    }
})
