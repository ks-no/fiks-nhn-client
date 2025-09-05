package no.ks.fiks.nhn.ar

import java.time.OffsetDateTime

sealed class CommunicationParty(
    val herId: Int,
    val name: String,
    val parent: CommunicationPartyParent?,
    val physicalAddresses: List<PhysicalAddress>,
    val electronicAddresses: List<ElectronicAddress>,
)

class OrganizationCommunicationParty(
    herId: Int,
    name: String,
    parent: CommunicationPartyParent?,
    physicalAddresses: List<PhysicalAddress>,
    electronicAddresses: List<ElectronicAddress>,
    val organizationNumber: String?,
) : CommunicationParty(herId, name, parent, physicalAddresses, electronicAddresses) {

    override fun toString(): String {
        return "OrganizationCommunicationParty(herId=$herId, parent=$parent, physicalAddresses=$physicalAddresses, organizationNumber=$organizationNumber)"
    }
}

class PersonCommunicationParty(
    herId: Int,
    name: String,
    parent: CommunicationPartyParent?,
    physicalAddresses: List<PhysicalAddress>,
    electronicAddresses: List<ElectronicAddress>,
    val firstName: String,
    val middleName: String?,
    val lastName: String,
) : CommunicationParty(herId, name, parent, physicalAddresses, electronicAddresses) {

    override fun toString(): String {
        return "PersonCommunicationParty(herId=$herId, parent=$parent, physicalAddresses=$physicalAddresses, firstName='$firstName', middleName=$middleName, lastName='$lastName')"
    }
}

class ServiceCommunicationParty(
    herId: Int,
    name: String,
    parent: CommunicationPartyParent?,
    physicalAddresses: List<PhysicalAddress>,
    electronicAddresses: List<ElectronicAddress>,
) : CommunicationParty(herId, name, parent, physicalAddresses, electronicAddresses) {

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
    val type: PostalAddressType?,
    val streetAddress: String?,
    val postbox: String?,
    val postalCode: String?,
    val city: String?,
    val country: String?,
)

data class PostalAddress(
    val name: String?,
    val streetAddress: String?,
    val postbox: String?,
    val postalCode: String?,
    val city: String?,
    val country: String?,
)

data class ElectronicAddress(
    val type: AddressComponent?,
    val address: String?,
    val lastChanged: OffsetDateTime?,
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

// From NHN Code service, code 9044
enum class AddressComponent(
    val code: String,
    val text: String,
) {
    FHIR_ENDEPUNKT("E_FHIR", "FHIR-Endepunkt"),
    EDI("E_EDI", "EDI (Standardisert elektronisk melding)"),
    EDI_WEBSERVICE("E_EDI_WS", "EDI Webservice (Standardisert elektronisk melding)"),
    EDI_AMTRIX("E_AMT", "EDI Amtrix"),
    EDI_BIZTALK("E_BIZ", "EDI BizTalk"),
    NHN_SMTP("E_NSM", "NHN SMTP"),
    EDI_SYNKRON_HTTP("E_EDI_HTTP_SYNC", "EDI-meldinger over synkron HTTP"),
    EDI_ASYNKRON_HTTP("E_EDI_HTTP_ASYNC", "EDI-meldinger over asynkron HTTP"),
    ASYNKRON_AMQP("E_SB_ASYNC", "Asynkron AMQP kø"),
    FEILMELDING_AMQP("E_SB_ERROR", "Feilmelding AMQP kø"),
    SYNKRON_AMQP("E_SB_SYNC", "Synkron AMQP kø"),
    SYNKRON_SVAR_AMQP("E_SB_SYNCREPLY", "Synkron Svar AMQP kø"),

    DIGITALT_SERTIFIKAT("E_DS", "Digital sertifikat"),
    KRYPTERINGSSERTIFIKAT("E_KS", "Krypteringssertifikat"),
    SIGNERINGSSERTIFIKAT("E_SS", "Signeringssertifikat"),

    TELEFONNUMMER("E_TLF", "Telefonnummer"),
    SENTRALBORDNUMMER("E_CEN", "Sentralbordnummer"),
    DIREKTENUMMER("E_DIR", "Direktenummer"),
    SAMHANDLINGSNUMMER_ICE("E_ICE", "Samhandlingsnummer ICE"),
    MOBILTELEFONNUMMER("E_MOB", "Mobiltelefonnummer"),
    TELEFONNUMMER_SENTRALBORD("E_SEN", "Telefonnumer sentralbord"),
    EPOST("E_EPO", "Epost-adresse"),
    FAXNUMMER("E_FAX", "Faxnummer"),
    HJEMMESIDE("E_URL", "Hjemmeside"),

    LOKAL_ELEKTRONISK_ADRESSE("E_LOK", "Lokal Elektronisk Adresse"),
    MELDINGSINFOMRASJON("E_MLD", "Meldingsinformasjon"),
    ADRESSELENKE_FOR_INNSYNRSRETT("E_PTL", "Adresselenke for innsynsrett"),
    BREV_ELLER_ANNEN_UTSKRIFT("E_BRV", "Brev eller annen utskrift på et fysisk medium"),
    ANNET("E_DIV", "Annet"),

    BOLIGNUMMER("ADL", "Bolignummer"),
    BYGNINGSNUMMER("BUI", "Bygning nummer"),
    GATENUMMER("HNR", "Gatenummer"),
    GATENAVN("STR", "Gatenavn"),
    POSTBOKSNUMMER("POB", "Postboksnummer"),
    POSTNUMMER("ZIP", "Postnummer"),
    POSTKONTOR("POO", "Postkontor"),
    POSTSTED("POP", "Poststed"),
    GPS_KOORDINATER("GPS", "GPS-koordinater"),
    NASJON("CNT", "Nasjon"),
    BESKRIVELSE("DSC", "Beskrivelse");

    companion object {
        private val codeToType = AddressComponent.entries.associateBy { it.code }

        fun fromCode(code: String?): AddressComponent? = code?.let { codeToType[it] }
    }
}