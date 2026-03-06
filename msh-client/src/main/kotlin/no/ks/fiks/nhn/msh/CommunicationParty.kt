package no.ks.fiks.nhn.msh

import no.ks.fiks.hdir.Adressetype
import no.ks.fiks.hdir.OrganizationIdType
import no.ks.fiks.hdir.PersonIdType


data class Sender(
    val parent: OrganizationCommunicationParty,
    val child: CommunicationParty,
)

data class Receiver(
    val parent: OrganizationCommunicationParty,
    val child: CommunicationParty,
    val patient: Patient,
)

data class Address(
    val type: Adressetype?,
    val streetAdr: String?,
    val postalCode: String?,
    val city: String?,
    val postbox: String?,
    val county: County?,
    val country: Country?,
)

data class County(
    val code: String,
    val name: String,
)

data class Country(
    val code: String,
    val name: String,
)

sealed class CommunicationParty(
    val ids: List<Id>,
    val address: Address?,
) {

    abstract val herId: Int?
    abstract val fullName: String

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CommunicationParty

        return ids == other.ids
    }

    override fun hashCode(): Int {
        return ids.hashCode()
    }

}

class OrganizationCommunicationParty(
    ids: List<OrganizationId>,
    address: Address? = null,
    val name: String,
) : CommunicationParty(ids, address) {

    override val herId: Int?
        get() = ids.firstOrNull { it.type == OrganizationIdType.HER_ID }?.id?.toIntOrNull()

    override val fullName: String
        get() = name

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as OrganizationCommunicationParty

        return name == other.name
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }

    override fun toString(): String {
        return "OrganizationCommunicationParty(ids='$ids', name='$name')"
    }
}

class PersonCommunicationParty(
    ids: List<PersonId>,
    address: Address? = null,
    val firstName: String,
    val middleName: String?,
    val lastName: String,
) : CommunicationParty(ids, address) {

    override val herId: Int?
        get() = ids.firstOrNull { it.type == PersonIdType.HER_ID }?.id?.toIntOrNull()

    override val fullName: String
        get() = listOf(firstName, middleName, lastName).filter { !it.isNullOrBlank() }.joinToString(" ")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as PersonCommunicationParty

        if (firstName != other.firstName) return false
        if (middleName != other.middleName) return false
        if (lastName != other.lastName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + firstName.hashCode()
        result = 31 * result + (middleName?.hashCode() ?: 0)
        result = 31 * result + lastName.hashCode()
        return result
    }

    override fun toString(): String {
        return "PersonCommunicationParty(ids='$ids', firstName='$firstName', middleName=$middleName, lastName='$lastName')"
    }

}

data class Patient(
    val fnr: String,
    val firstName: String,
    val middleName: String?,
    val lastName: String,
)
