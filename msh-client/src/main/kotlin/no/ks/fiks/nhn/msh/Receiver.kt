package no.ks.fiks.nhn.msh

data class Receiver(
    val parent: OrganizationReceiverDetails,
    val child: ReceiverDetails,
    val patient: Patient,
)

sealed class ReceiverDetails(val ids: List<Id>)

class OrganizationReceiverDetails(
    ids: List<OrganizationId>,
    val name: String,
) : ReceiverDetails(ids) {
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
}

data class Patient(
    val fnr: String,
    val firstName: String,
    val middleName: String?,
    val lastName: String,
)
