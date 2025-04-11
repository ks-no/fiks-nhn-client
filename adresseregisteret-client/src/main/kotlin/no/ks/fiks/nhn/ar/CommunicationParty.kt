package no.ks.fiks.nhn.ar

data class CommunicationParty(
    val herId: Int,
    val firstName: String,
    val middleName: String?,
    val lastName: String,
    val parent: CommunicationPartyParent,
    val physicalAddresses: List<PhysicalAddress>,
)

data class CommunicationPartyParent(
    val herId: Int,
    val name: String,
)

data class PhysicalAddress(
    val type: Adressetetype?,
    val streetAddress: String?,
    val postbox: String?,
    val postalCode: String?,
    val city: String?,
    val country: String?,
)


// Kodeverk 3401
enum class Adressetetype(
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
        private val codeToType = Adressetetype.values().associateBy { it.code }

        fun fromCode(code: String?): Adressetetype? = code?.let { codeToType[it] }
    }
}
