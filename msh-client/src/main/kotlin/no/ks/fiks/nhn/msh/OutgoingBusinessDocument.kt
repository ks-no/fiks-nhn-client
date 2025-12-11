package no.ks.fiks.nhn.msh

import no.ks.fiks.hdir.*
import java.io.InputStream
import java.time.OffsetDateTime
import java.util.*

data class OutgoingBusinessDocument(
    val id: UUID,
    val sender: Sender,
    val receiver: Receiver,
    val message: OutgoingMessage,
    val vedlegg: OutgoingVedlegg,
    val version: DialogmeldingVersion,
)

data class GPForPersonOutgoingBusinessDocument(
    val id: UUID,
    val sender: Sender,
    val person: Person,
    val message: OutgoingMessage,
    val vedlegg: OutgoingVedlegg,
    val version: DialogmeldingVersion,
)

data class OutgoingVedlegg(
    val date: OffsetDateTime,
    val description: String,
    val data: InputStream,
)

data class OutgoingMessage(
    val subject: String,
    val body: String,
    val responsibleHealthcareProfessional: HealthcareProfessional,
    val recipientContact: RecipientContact,
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

data class Person(
    val fnr: String,
    val firstName: String,
    val middleName: String? = null,
    val lastName: String,
)

enum class DialogmeldingVersion {
    V1_0,
    V1_1,
}
