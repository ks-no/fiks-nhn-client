package no.ks.fiks.nhn.ar

import io.kotest.assertions.asClue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.beNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import no.nhn.common.ar.GenericFault
import no.nhn.register.communicationparty.*
import java.util.*
import kotlin.random.Random.Default.nextInt
import no.nhn.register.communicationparty.CommunicationParty as NhnCommunicationParty

class LookupHerIdTest : StringSpec({

    "Verify that an organization returned from the API is mapped correctly" {
        val queryHerId = nextInt(1000, 100000)
        val expected = buildOrganization()

        buildClient(setupServiceMock(expected))
            .lookupHerId(queryHerId)
            .asClue {
                it.shouldBeInstanceOf<OrganizationCommunicationParty>()
                it shouldHaveSameValuesAs expected
                with(it) {
                    organizationNumber!!.toInt() shouldBe expected.organizationNumber
                }
            }
    }

    "Verify that a person returned from the API is mapped correctly" {
        val queryHerId = nextInt(1000, 100000)
        val expected = buildOrganizationPerson()

        AdresseregisteretClient("", Credentials("", ""), setupServiceMock(expected))
            .lookupHerId(queryHerId)
            .asClue {
                it.shouldBeInstanceOf<PersonCommunicationParty>()
                it shouldHaveSameValuesAs expected
                with(it) {
                    firstName shouldBe expected.person.value.firstName.value
                    middleName shouldBe expected.person.value.middleName.value
                    lastName shouldBe expected.person.value.lastName.value
                }
            }
    }

    "Verify that a service returned from the API is mapped correctly" {
        val queryHerId = nextInt(1000, 100000)
        val expected = buildService()

        buildClient(setupServiceMock(expected))
            .lookupHerId(queryHerId)
            .asClue {
                it.shouldBeInstanceOf<ServiceCommunicationParty>()
                it shouldHaveSameValuesAs expected
            }
    }

    "Organization number should be padded with 0 to a total length of 9" {
        val organizationNumber = nextInt(100, 1000)

        buildClient(setupServiceMock(buildOrganization(organizationNumber = organizationNumber)))
            .lookupHerId(nextInt(1000, 100000))
            .asClue {
                it.shouldBeInstanceOf<OrganizationCommunicationParty>()
                it.organizationNumber shouldBe "000000$organizationNumber"
            }
    }

    "Address postal code should be padded with 0 to a total length of 4" {
        val postalCode1 = nextInt(1, 10)
        val postalCode2 = nextInt(10, 100)
        val postalCode3 = nextInt(100, 1000)
        val postalCode4 = nextInt(1000, 10000)

        buildClient(
            setupServiceMock(
                buildOrganization(
                    addresses = listOf(
                        buildPhysicalAddress(postalCode = postalCode1),
                        buildPhysicalAddress(postalCode = postalCode2),
                        buildPhysicalAddress(postalCode = postalCode3),
                        buildPhysicalAddress(postalCode = postalCode4),
                    )
                )
            )
        )
            .lookupHerId(nextInt(1000, 100000))
            .asClue {
                it.shouldBeInstanceOf<OrganizationCommunicationParty>()
                it.physicalAddresses[0].postalCode shouldBe "000$postalCode1"
                it.physicalAddresses[1].postalCode shouldBe "00$postalCode2"
                it.physicalAddresses[2].postalCode shouldBe "0$postalCode3"
                it.physicalAddresses[3].postalCode shouldBe postalCode4.toString()
            }
    }

    "Unknown address type should map to null" {
        val type1 = AddressType.entries.random()
        val type2 = AddressType.entries.random()

        buildClient(
            setupServiceMock(
                buildOrganization(
                    addresses = listOf(
                        buildPhysicalAddress(type = type1.code),
                        buildPhysicalAddress(type = UUID.randomUUID().toString()),
                        buildPhysicalAddress(type = type2.code),
                    )
                )
            )
        )
            .lookupHerId(nextInt(1000, 100000))
            .asClue {
                it.shouldBeInstanceOf<OrganizationCommunicationParty>()
                it.physicalAddresses[0].type shouldBe type1
                it.physicalAddresses[1].type should beNull()
                it.physicalAddresses[2].type shouldBe type2
            }
    }

    "Should handle null values in address" {
        buildClient(
            setupServiceMock(
                buildOrganization(
                    addresses = listOf(
                        buildPhysicalAddress(
                            type = null,
                            streetAddress = null,
                            postbox = null,
                            postalCode = null,
                            city = null,
                            country = null,
                        ),
                    )
                )
            )
        )
            .lookupHerId(nextInt(1000, 100000))
            .asClue {
                it.shouldBeInstanceOf<OrganizationCommunicationParty>()
                it.physicalAddresses shouldHaveSize 1
                with(it.physicalAddresses.single()) {
                    type shouldBe null
                    streetAddress shouldBe null
                    postbox shouldBe null
                    postalCode shouldBe null
                    city shouldBe null
                    country shouldBe null
                }
            }
    }

    "Should handle empty values in address" {
        buildClient(
            setupServiceMock(
                buildOrganization(
                    addresses = listOf(
                        buildPhysicalAddress(
                            type = "",
                            streetAddress = " ",
                            postbox = "\t",
                            city = "",
                            country = "",
                        ),
                    )
                )
            )
        )
            .lookupHerId(nextInt(1000, 100000))
            .asClue {
                it.shouldBeInstanceOf<OrganizationCommunicationParty>()
                it.physicalAddresses shouldHaveSize 1
                with(it.physicalAddresses.single()) {
                    type shouldBe null
                    streetAddress shouldBe null
                    postbox shouldBe null
                    city shouldBe null
                    country shouldBe null
                }
            }
    }

    "Should be able to map person without middle name" {
        val expected = buildOrganizationPerson(middleName = null)

        buildClient(setupServiceMock(expected))
            .lookupHerId(nextInt(1000, 100000))
            .asClue {
                it.shouldBeInstanceOf<PersonCommunicationParty>()
                it shouldHaveSameValuesAs expected
                with(it) {
                    firstName shouldBe expected.person.value.firstName.value
                    middleName should beNull()
                    lastName shouldBe expected.person.value.lastName.value
                }
            }
    }

    "An exception thrown by the API should be mapped to a client exception" {
        val exceptionMessage = UUID.randomUUID().toString()
        val faultErrorCode = UUID.randomUUID().toString()
        val faultMessage = UUID.randomUUID().toString()

        shouldThrow<AdresseregisteretApiException> {
            buildClient(
                mockk {
                    every { getCommunicationPartyDetails(any()) } throws ICommunicationPartyServiceGetCommunicationPartyDetailsGenericFaultFaultFaultMessage(
                        exceptionMessage,
                        GenericFault().apply {
                            errorCode = buildJAXBElement(faultErrorCode)
                            message = buildJAXBElement(faultMessage)
                        })
                })
                .lookupHerId(nextInt(1000, 100000))
        }.asClue {
            it.errorCode shouldBe faultErrorCode
            it.faultMessage shouldBe faultMessage
            it.message shouldBe exceptionMessage
        }
    }

})

private infix fun CommunicationParty.shouldHaveSameValuesAs(expected: NhnCommunicationParty) {
    herId shouldBe expected.herId
    parent shouldNot beNull()
    parent!!.herId shouldBe expected.parentHerId
    parent.name shouldBe expected.parentName.value
    physicalAddresses shouldHaveSize expected.physicalAddresses.value.physicalAddress.size
    physicalAddresses.zip(expected.physicalAddresses.value.physicalAddress).forEach { (actual, expected) ->
        actual.type!!.code shouldBe expected.type.value.codeValue.value
        actual.streetAddress shouldBe expected.streetAddress.value
        actual.postbox shouldBe expected.postbox.value
        actual.postalCode!!.toInt() shouldBe expected.postalCode
        actual.city shouldBe expected.city.value
        actual.country shouldBe expected.country.value.codeText.value
    }
}
