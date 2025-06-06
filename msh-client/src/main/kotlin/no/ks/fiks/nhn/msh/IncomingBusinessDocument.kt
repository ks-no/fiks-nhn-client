package no.ks.fiks.nhn.msh

import no.ks.fiks.hdir.*
import java.io.InputStream
import java.time.OffsetDateTime
import java.util.*

data class IncomingBusinessDocument(
    val id: String,
    val type: MeldingensFunksjon,
    val sender: Organization,
    val receiver: Receiver,
    val message: IncomingMessage?,
    val vedlegg: IncomingVedlegg?,
)

sealed class IncomingMessage
data class Dialogmelding(
    val type: TypeOpplysningPasientsamhandling,
    val sporsmal: String,
) : IncomingMessage()

data class HelsefagligDialog(
    val tema: TemaForHelsefagligDialog,
) : IncomingMessage()

data class IncomingVedlegg(
    val date: OffsetDateTime?,
    val description: String?,
    val mimeType: String?,
    val data: InputStream?,
)
