package no.ks.fiks.nhn.msh

import no.ks.fiks.hdir.*
import java.io.InputStream
import java.time.OffsetDateTime
import java.util.*

data class OutgoingBusinessDocument(
    val id: UUID,
    val sender: Organization,
    val receiver: Receiver,
    val message: BusinessDocumentMessage,
    val vedlegg: Vedlegg,
    val version: DialogmeldingVersion,
)

data class GPForPersonOutgoingBusinessDocument(
    val id: UUID,
    val sender: Organization,
    val person: Person,
    val message: BusinessDocumentMessage,
    val vedlegg: Vedlegg,
    val version: DialogmeldingVersion,
)

data class Vedlegg(
    val date: OffsetDateTime,
    val description: String,
    val data: InputStream,
)

data class Person(
    val fnr: String,
    val firstName: String,
    val middleName: String? = null,
    val lastName: String,
)

data class IncomingBusinessDocument(
    val id: String,
    val type: MeldingensFunksjon,
    val sender: Organization,
    val receiver: Receiver,
    val dialogmelding: Dialogmelding?,
    val vedlegg: InputStream?,
)

data class BusinessDocumentMessage(
    val subject: String,
    val body: String,
    val responsibleHealthcareProfessional: HealthcareProfessional,
    val recipientContact: RecipientContact,
)

data class Organization(
    val name: String,
    val id: Id,
    val childOrganization: Organization? = null,
)

sealed class Receiver
data class HerIdReceiver(
    val parent: HerIdReceiverParent,
    val child: HerIdReceiverChild,
    val patient: Patient,
) : Receiver()

data class HerIdReceiverParent(
    val name: String,
    val id: Id,
)

sealed class HerIdReceiverChild
data class OrganizationHerIdReceiverChild(
    val name: String,
    val id: Id,
) : HerIdReceiverChild()
data class PersonHerIdReceiverChild(
    val id: Id,
    val firstName: String,
    val middleName: String?,
    val lastName: String,
) : HerIdReceiverChild()

data class Patient(
    val fnr: String,
    val firstName: String,
    val middleName: String?,
    val lastName: String,
)

data class Dialogmelding(
    val type: TypeOpplysningPasientsamhandling,
    val sporsmal: String,
)

data class HealthcareProfessional(
    val firstName: String,
    val middleName: String? = null,
    val lastName: String,
    val phoneNumber: String,
    val roleToPatient: HelsepersonellsFunksjoner,
)

data class RecipientContact(
    val type: Helsepersonell,
)

enum class DialogmeldingVersion {
    V1_0,
    V1_1,
}
