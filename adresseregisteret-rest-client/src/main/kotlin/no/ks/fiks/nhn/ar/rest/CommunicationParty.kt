package no.ks.fiks.nhn.ar.rest

import java.time.OffsetDateTime

sealed class CommunicationParty(
    val herId: Int,
    val name: String,
    val parent: CommunicationPartyParent?,
    val physicalAddresses: List<PhysicalAddress>,
    val email: String?,
    val homepageUrl: String?,
    val phoneNumber: String?,
    val faxNumber: String?,
    val ediAddress: String?,
    val fhirAddress: String?,
)

class OrganizationCommunicationParty(
    herId: Int,
    name: String,
    parent: CommunicationPartyParent?,
    physicalAddresses: List<PhysicalAddress>,
    email: String?,
    homepageUrl: String?,
    phoneNumber: String?,
    faxNumber: String?,
    ediAddress: String?,
    fhirAddress: String?,
    val organizationNumber: String?,
) : CommunicationParty(herId, name, parent, physicalAddresses, email, homepageUrl, phoneNumber, faxNumber, ediAddress, fhirAddress) {

    override fun toString(): String {
        return "OrganizationCommunicationParty(herId=$herId, parent=$parent, physicalAddresses=$physicalAddresses, organizationNumber=$organizationNumber)"
    }
}

class PersonCommunicationParty(
    herId: Int,
    name: String,
    parent: CommunicationPartyParent?,
    physicalAddresses: List<PhysicalAddress>,
    email: String?,
    homepageUrl: String?,
    phoneNumber: String?,
    faxNumber: String?,
    ediAddress: String?,
    fhirAddress: String?,
) : CommunicationParty(herId, name, parent, physicalAddresses, email, homepageUrl, phoneNumber, faxNumber, ediAddress, fhirAddress) {

    override fun toString(): String {
        return "PersonCommunicationParty(herId=$herId, name='$name', parent=$parent, physicalAddresses=$physicalAddresses)"
    }
}

class ServiceCommunicationParty(
    herId: Int,
    name: String,
    parent: CommunicationPartyParent?,
    physicalAddresses: List<PhysicalAddress>,
    email: String?,
    homepageUrl: String?,
    phoneNumber: String?,
    faxNumber: String?,
    ediAddress: String?,
    fhirAddress: String?,
) : CommunicationParty(herId, name, parent, physicalAddresses, email, homepageUrl, phoneNumber, faxNumber, ediAddress, fhirAddress) {

    override fun toString(): String {
        return "ServiceCommunicationParty(herId=$herId, parent=$parent, physicalAddresses=$physicalAddresses)"
    }
}

data class CommunicationPartyParent(
    val herId: Int,
    val name: String,
    val organizationNumber: String,
)

data class PhysicalAddress(
    val type: PostalAddressType,
    val streetAddress: String?,
    val postbox: String?,
    val postalCode: String?,
    val city: String?,
    val country: Country?,
)

data class PostalAddress(
    val name: String?,
    val streetAddress: String?,
    val postbox: String?,
    val postalCode: String?,
    val city: String?,
    val country: Country?,
)

data class Country(
    val code: String,
    val name: String,
)


// Kodeverk 3401
enum class PostalAddressType(
    val code: String,
) {
    POSTADRESSE("PST"),
    BESOKSADRESSE("RES");

    companion object {
        private val codeToType = entries.associateBy { it.code }

        fun fromCode(code: String?): PostalAddressType? = code?.let { codeToType[it] }
    }
}

fun PhysicalAddress.toPostalAddress(name: String) =
    PostalAddress(
        name = name,
        streetAddress = streetAddress,
        postbox = postbox,
        postalCode = postalCode,
        city = city,
        country = country,
    )

