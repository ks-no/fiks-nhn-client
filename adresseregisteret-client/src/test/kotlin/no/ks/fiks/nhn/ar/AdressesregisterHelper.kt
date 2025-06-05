package no.ks.fiks.nhn.ar

import io.mockk.every
import io.mockk.mockk
import jakarta.xml.bind.JAXBElement
import no.nhn.common.ar.Code
import no.nhn.common.ar.PhysicalAddress
import no.nhn.register.communicationparty.ICommunicationPartyService
import no.nhn.register.communicationparty.Organization
import no.nhn.register.communicationparty.OrganizationPerson
import no.nhn.register.communicationparty.Service
import no.nhn.register.hpr.Person
import java.util.UUID
import javax.xml.namespace.QName
import kotlin.random.Random.Default.nextInt
import no.nhn.register.communicationparty.CommunicationParty as NhnCommunicationParty

fun buildClient(service: ICommunicationPartyService) = AdresseregisteretClient(Environment(""), Credentials("", ""), service)

fun setupServiceMock(expected: NhnCommunicationParty?) = mockk<ICommunicationPartyService> {
    every { getCommunicationPartyDetails(any()) } returns expected
}

fun buildOrganization(
    herId: Int = nextInt(1000, 100000),
    name: String = buildRandomString(),
    parentHerId: Int = nextInt(1000, 100000),
    parentName: String = buildRandomString(),
    addresses: List<PhysicalAddress> = List(nextInt(1, 5)) { buildPhysicalAddress() },
    organizationNumber: Int = nextInt(100000000, 1000000000),
) = Organization().apply {
    this.herId = herId
    this.name = buildJAXBElement(name)
    this.parentHerId = parentHerId
    this.parentName = buildJAXBElement(parentName)
    this.physicalAddresses = buildJAXBElement(mockk { every { physicalAddress } returns addresses })
    this.organizationNumber = organizationNumber
}

fun buildOrganizationPerson(
    herId: Int = nextInt(1000, 100000),
    name: String = buildRandomString(),
    parentHerId: Int = nextInt(1000, 100000),
    parentName: String = buildRandomString(),
    firstName: String = buildRandomString(),
    middleName: String? = buildRandomString(),
    lastName: String = buildRandomString(),
    addresses: List<PhysicalAddress> = List(nextInt(1, 5)) { buildPhysicalAddress() }
) = OrganizationPerson().apply {
    this.herId = herId
    this.name = buildJAXBElement(name)
    this.parentHerId = parentHerId
    this.parentName = buildJAXBElement(parentName)
    this.person = buildJAXBElement(Person().apply {
        this.firstName = buildJAXBElement(firstName)
        this.middleName = buildJAXBElement(middleName)
        this.lastName = buildJAXBElement(lastName)
    })
    this.physicalAddresses = buildJAXBElement(mockk { every { physicalAddress } returns addresses })
}

fun buildService(
    herId: Int = nextInt(1000, 100000),
    name: String = buildRandomString(),
    parentHerId: Int = nextInt(1000, 100000),
    parentName: String = buildRandomString(),
    addresses: List<PhysicalAddress> = List(nextInt(1, 5)) { buildPhysicalAddress() }
) = Service().apply {
    this.herId = herId
    this.name = buildJAXBElement(name)
    this.parentHerId = parentHerId
    this.parentName = buildJAXBElement(parentName)
    this.physicalAddresses = buildJAXBElement(mockk { every { physicalAddress } returns addresses })
}

fun buildPhysicalAddress(
    type: String? = AddressType.entries.random().code,
    streetAddress: String? = buildRandomString(),
    postbox: String? = UUID.randomUUID().toString(),
    postalCode: Int? = nextInt(0, 10000),
    city: String? = buildRandomString(),
    country: String? = buildRandomString(),
) = PhysicalAddress().apply {
    this.type = buildJAXBElement(Code().apply { codeValue = buildJAXBElement(type) })
    this.streetAddress = buildJAXBElement(streetAddress)
    this.postbox = buildJAXBElement(postbox)
    this.postalCode = postalCode
    this.city = buildJAXBElement(city)
    this.country = buildJAXBElement(Code().apply { codeText = buildJAXBElement(country) })
}

fun buildRandomString() = List(nextInt(1, 3)) { UUID.randomUUID().toString().take(nextInt(3, 20)) }.joinToString(" ")

inline fun <reified T> buildJAXBElement(value: T) = JAXBElement(QName.valueOf("field"), T::class.java, value)
