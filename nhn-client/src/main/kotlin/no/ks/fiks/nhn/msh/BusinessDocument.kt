package no.ks.fiks.nhn.msh

import no.ks.fiks.hdir.*
import java.io.InputStream
import java.util.UUID

data class OutgoingBusinessDocument(
    val id: UUID,
    val type: MeldingensFunksjon,
    val sender: Organisation,
    val receiver: Receiver,
    val vedlegg: InputStream?,
)

data class FastlegeForPersonOutgoingBusinessDocument(
    val id: UUID,
    val type: MeldingensFunksjon,
    val sender: Organisation,
    val personFnr: String,
    val vedlegg: InputStream?,
)

data class IncomingBusinessDocument(
    val id: String,
    val type: MeldingensFunksjon,
    val sender: Organisation,
    val receiver: Receiver,
    val dialogmelding: Dialogmelding?,
    val vedlegg: InputStream?,
)

data class ApplicationReceipt(
    val id: String,
    val acknowledgedBusinessDocumentId: String,
    val status: StatusForMottakAvMelding,
    val errors: List<ApplicationReceiptError>,
    val sender: Organisation,
    val receiver: Organisation,
)

data class ApplicationReceiptError(
    val type: FeilmeldingForApplikasjonskvittering,
    val description: String?,
)

data class Organisation(
    val name: String,
    val id: Id,
    val childOrganisation: Organisation? = null,
)

data class Id(
    val id: String,
    val type: IdType,
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
data class OrganisasjonHerIdReceiverChild(
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

