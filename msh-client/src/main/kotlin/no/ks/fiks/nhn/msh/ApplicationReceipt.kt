package no.ks.fiks.nhn.msh

import no.ks.fiks.hdir.FeilmeldingForApplikasjonskvittering
import no.ks.fiks.hdir.StatusForMottakAvMelding

data class IncomingApplicationReceipt(
    val id: String,
    val acknowledgedBusinessDocumentId: String,
    val status: StatusForMottakAvMelding,
    val errors: List<IncomingApplicationReceiptError>,
    val sender: Institution,
    val receiver: Institution,
)

data class ApplicationReceiptInfo(
    val receiverHerId: Int,
    val status: StatusForMottakAvMelding?,
    val errors: List<IncomingApplicationReceiptError>,
)

data class IncomingApplicationReceiptError(
    val type: FeilmeldingForApplikasjonskvittering,
    val details: String?,
    val errorCode: String?,
    val description: String?,
    val oid: String?,
)

data class OutgoingApplicationReceipt(
    val acknowledgedId: String,
    val senderHerId: Int,
    val status: StatusForMottakAvMelding,
    val errors: List<OutgoingApplicationReceiptError>? = null,
    val transportMetadataOverrides: AppRecTransportMetadataOverrides? = null,
)

data class AppRecTransportMetadataOverrides(
    val transportSenderHerId: Int? = null,
    val transportReceiverHerId: Int? = null,
    val cpaId: String? = null,
    val conversationId: String? = null,
    val service: String? = null,
    val serviceType: String? = null,
    val action: String? = null,
    val senderRole: String? = null,
    val receiverRole: String? = null,
    val middlewareName: String? = null,
    val middlewareVersion: String? = null,
    val compressPayload: Boolean? = null,
)

data class OutgoingApplicationReceiptError(
    val type: FeilmeldingForApplikasjonskvittering,
    val details: String? = null,
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
