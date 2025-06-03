package no.ks.fiks.nhn.ar

sealed class CommunicationParty(
    val herId: Int,
    val parent: CommunicationPartyParent?,
    val physicalAddresses: List<PhysicalAddress>,
)

class OrganizationCommunicationParty(
    herId: Int,
    parent: CommunicationPartyParent?,
    physicalAddresses: List<PhysicalAddress>,
    val organizationNumber: String?,
) : CommunicationParty(herId, parent, physicalAddresses) {

    override fun toString(): String {
        return "OrganizationCommunicationParty(herId=$herId, parent=$parent, physicalAddresses=$physicalAddresses, organizationNumber=$organizationNumber)"
    }
}

class PersonCommunicationParty(
    herId: Int,
    parent: CommunicationPartyParent?,
    physicalAddresses: List<PhysicalAddress>,
    val firstName: String,
    val middleName: String?,
    val lastName: String,
) : CommunicationParty(herId, parent, physicalAddresses) {

    override fun toString(): String {
        return "PersonCommunicationParty(herId=$herId, parent=$parent, physicalAddresses=$physicalAddresses, firstName='$firstName', middleName=$middleName, lastName='$lastName')"
    }
}

class ServiceCommunicationParty(
    herId: Int,
    parent: CommunicationPartyParent?,
    physicalAddresses: List<PhysicalAddress>,
) : CommunicationParty(herId, parent, physicalAddresses) {

    override fun toString(): String {
        return "ServiceCommunicationParty(herId=$herId, parent=$parent, physicalAddresses=$physicalAddresses)"
    }
}

data class CommunicationPartyParent(
    val herId: Int,
    val name: String,
)

data class PhysicalAddress(
    val type: AddressType?,
    val streetAddress: String?,
    val postbox: String?,
    val postalCode: String?,
    val city: String?,
    val country: String?,
)


// Kodeverk 3401
enum class AddressType(
    val code: String,
) {
    UBRUKELIG_ADRESSE("BAD"),
    BOSTEDSADRESSE("H"),
    FOLKEREGISTERADRESSE("HP"),
    FERIEADRESSE("HV"),
    FAKTURERINGSADRESSE("INV"),
    POSTADRESSE("PST"),
    BESOKSADRESSE("RES"),
    MIDLERTIDIG_ADRESSE("TMP"),
    ARBEIDSADRESSE("WP");

    companion object {
        private val codeToType = entries.associateBy { it.code }

        fun fromCode(code: String?): AddressType? = code?.let { codeToType[it] }
    }
}
