package no.ks.fiks.nhn.ar

import io.mockk.every
import io.mockk.mockk
import jakarta.xml.bind.JAXBElement
import no.nhn.common.ar.Code
import no.nhn.common.ar.ElectronicAddress
import no.nhn.common.ar.PhysicalAddress
import no.nhn.register.communicationparty.Organization
import no.nhn.register.communicationparty.OrganizationPerson
import no.nhn.register.communicationparty.Service
import no.nhn.register.hpr.Person
import java.time.OffsetDateTime
import java.util.GregorianCalendar
import java.util.UUID
import javax.xml.datatype.DatatypeFactory
import javax.xml.namespace.QName
import kotlin.random.Random.Default.nextInt
import no.nhn.register.communicationparty.CommunicationParty as NhnCommunicationParty

fun buildClient(service: AdresseregisteretService) = AdresseregisteretClient(service)

fun setupServiceMock(expected: NhnCommunicationParty?) = mockk<AdresseregisteretService> {
    every { getCommunicationPartyDetails(any()) } returns expected
}

fun buildOrganization(
    herId: Int = nextInt(1000, 100000),
    name: String = buildRandomString(),
    parentHerId: Int = nextInt(1000, 100000),
    parentName: String = buildRandomString(),
    physicalAddresses: List<PhysicalAddress> = List(nextInt(1, 5)) { buildPhysicalAddress() },
    electronicAddresses: List<ElectronicAddress> = List(nextInt(1, 5)) { buildElectronicAddress() },
    organizationNumber: Int = nextInt(100000000, 1000000000),
    parentOrganizationNumber: Int = nextInt(100000000, 1000000000),
) = Organization().apply {
    this.herId = herId
    this.name = buildJAXBElement(name)
    this.parentHerId = parentHerId
    this.parentName = buildJAXBElement(parentName)
    this.parentOrganizationNumber = parentOrganizationNumber
    this.physicalAddresses = buildJAXBElement(mockk { every { physicalAddress } returns physicalAddresses })
    this.electronicAddresses = buildJAXBElement(mockk { every { electronicAddress } returns electronicAddresses })
    this.organizationNumber = organizationNumber
}

fun buildOrganizationPerson(
    herId: Int = nextInt(1000, 100000),
    name: String = buildRandomString(),
    parentHerId: Int = nextInt(1000, 100000),
    parentName: String = buildRandomString(),
    parentOrganizationNumber: Int = nextInt(100000000, 1000000000),
    firstName: String = buildRandomString(),
    middleName: String? = buildRandomString(),
    lastName: String = buildRandomString(),
    physicalAddresses: List<PhysicalAddress> = List(nextInt(1, 5)) { buildPhysicalAddress() },
    electronicAddresses: List<ElectronicAddress> = List(nextInt(1, 5)) { buildElectronicAddress() },
) = OrganizationPerson().apply {
    this.herId = herId
    this.name = buildJAXBElement(name)
    this.parentHerId = parentHerId
    this.parentName = buildJAXBElement(parentName)
    this.parentOrganizationNumber = parentOrganizationNumber
    this.person = buildJAXBElement(Person().apply {
        this.firstName = buildJAXBElement(firstName)
        this.middleName = buildJAXBElement(middleName)
        this.lastName = buildJAXBElement(lastName)
    })
    this.physicalAddresses = buildJAXBElement(mockk { every { physicalAddress } returns physicalAddresses })
    this.electronicAddresses = buildJAXBElement(mockk { every { electronicAddress } returns electronicAddresses })
}

fun buildService(
    herId: Int = nextInt(1000, 100000),
    name: String = buildRandomString(),
    parentHerId: Int = nextInt(1000, 100000),
    parentName: String = buildRandomString(),
    parentOrganizationNumber: Int = nextInt(100000000, 1000000000),
    physicalAddresses: List<PhysicalAddress> = List(nextInt(1, 5)) { buildPhysicalAddress() },
    electronicAddresses: List<ElectronicAddress> = List(nextInt(1, 5)) { buildElectronicAddress() },
) = Service().apply {
    this.herId = herId
    this.name = buildJAXBElement(name)
    this.parentHerId = parentHerId
    this.parentName = buildJAXBElement(parentName)
    this.parentOrganizationNumber = parentOrganizationNumber
    this.physicalAddresses = buildJAXBElement(mockk { every { physicalAddress } returns physicalAddresses })
    this.electronicAddresses = buildJAXBElement(mockk { every { electronicAddress } returns electronicAddresses })
}

fun buildPhysicalAddress(
    type: String? = PostalAddressType.entries.random().code,
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

fun buildElectronicAddress(
    type: String? = AddressComponent.entries.random().code,
    address: String? = buildRandomString(),
    lastChanged: OffsetDateTime? = OffsetDateTime.now(),
) = ElectronicAddress().apply {
    this.typeCodeValue = buildJAXBElement(type)
    this.address = buildJAXBElement(address)
    this.lastChanged = lastChanged?.let { DatatypeFactory.newInstance().newXMLGregorianCalendar(GregorianCalendar.from(it.toZonedDateTime())) }
}

fun buildRandomString() = List(nextInt(1, 3)) { UUID.randomUUID().toString().take(nextInt(3, 20)) }.joinToString(" ")

inline fun <reified T> buildJAXBElement(value: T) = JAXBElement(QName.valueOf("field"), T::class.java, value)
