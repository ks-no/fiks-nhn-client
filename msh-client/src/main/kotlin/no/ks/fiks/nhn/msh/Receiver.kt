package no.ks.fiks.nhn.msh

data class Receiver(
    val parent: OrganizationReceiverDetails,
    val child: ReceiverDetails,
    val patient: Patient,
)

sealed class ReceiverDetails(val ids: List<Id>) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ReceiverDetails

        return ids == other.ids
    }

    override fun hashCode(): Int {
        return ids.hashCode()
    }

}

class OrganizationReceiverDetails(
    ids: List<OrganizationId>,
    val name: String,
) : ReceiverDetails(ids) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as OrganizationReceiverDetails

        return name == other.name
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }

    override fun toString(): String {
        return "OrganizationReceiverDetails(ids='$ids', name='$name')"
    }
}

class PersonReceiverDetails(
    ids: List<PersonId>,
    val firstName: String,
    val middleName: String?,
    val lastName: String,
) : ReceiverDetails(ids) {

    override fun toString(): String {
        return "PersonReceiverDetails(ids='$ids', firstName='$firstName', middleName=$middleName, lastName='$lastName')"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as PersonReceiverDetails

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


}

data class Patient(
    val fnr: String,
    val firstName: String,
    val middleName: String?,
    val lastName: String,
)
