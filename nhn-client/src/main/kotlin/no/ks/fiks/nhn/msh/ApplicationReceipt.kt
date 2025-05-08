package no.ks.fiks.nhn.msh

import no.ks.fiks.hdir.FeilmeldingForApplikasjonskvittering
import no.ks.fiks.hdir.StatusForMottakAvMelding

data class ApplicationReceipt(
    val id: String,
    val originalMessageId: String,
    val status: StatusForMottakAvMelding,
    val errors: List<ApplicationReceiptError>,
    val sender: Organisation,
    val receiver: Organisation,
)

data class ApplicationReceiptError(
    val type: FeilmeldingForApplikasjonskvittering,
    val description: String?,
)
