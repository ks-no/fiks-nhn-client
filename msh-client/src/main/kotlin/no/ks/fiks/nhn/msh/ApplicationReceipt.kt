package no.ks.fiks.nhn.msh

import no.ks.fiks.hdir.FeilmeldingForApplikasjonskvittering
import no.ks.fiks.hdir.StatusForMottakAvMelding
import java.util.*

data class IncomingApplicationReceipt(
    val id: String,
    val acknowledgedBusinessDocumentId: String,
    val status: StatusForMottakAvMelding,
    val errors: List<ApplicationReceiptError>,
    val sender: Institution,
    val receiver: Institution,
)

data class ApplicationReceiptInfo(
    val receiverHerId: Int,
    val status: StatusForMottakAvMelding?,
    val errors: List<ApplicationReceiptError>,
)

data class OutgoingApplicationReceipt(
    val acknowledgedId: UUID,
    val senderHerId: Int,
    val status: StatusForMottakAvMelding,
    val errors: List<ApplicationReceiptError>? = null,
)

data class ApplicationReceiptError(
    val type: FeilmeldingForApplikasjonskvittering,
    val details: String?,
    val description: String? = null,
    val oid: String? = null,
)

data class Institution(
    val name: String,
    val id: Id,
    val department: Department?,
    val person: InstitutionPerson?,
)

data class Department(
    val name: String,
    val id: Id,
)

data class InstitutionPerson(
    val name: String,
    val id: Id,
)
