package no.ks.fiks.nhn.msh

data class Receiver(
    val parent: OrganizationReceiverDetails,
    val child: ReceiverDetails,
    val patient: Patient,
)

sealed class ReceiverDetails(val id: Id)
class OrganizationReceiverDetails(
    id: OrganizationId,
    val name: String,
) : ReceiverDetails(id)
class PersonReceiverDetails(
    id: PersonId,
    val firstName: String,
    val middleName: String?,
    val lastName: String,
) : ReceiverDetails(id)

data class Patient(
    val fnr: String,
    val firstName: String,
    val middleName: String?,
    val lastName: String,
)
