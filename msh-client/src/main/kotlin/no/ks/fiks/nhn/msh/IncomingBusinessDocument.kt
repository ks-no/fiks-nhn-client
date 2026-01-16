package no.ks.fiks.nhn.msh

import no.ks.fiks.hdir.KodeverkVerdi
import no.ks.fiks.hdir.MeldingensFunksjon
import java.io.InputStream
import java.time.LocalDate
import java.time.OffsetDateTime

data class IncomingBusinessDocument(
    val id: String,
    val date: OffsetDateTime?,
    val type: MeldingensFunksjon,
    val sender: Sender,
    val receiver: Receiver,
    val message: Dialogmelding?,
    val vedlegg: IncomingVedlegg?,
    val conversationRef: ConversationRef?,
)

data class Dialogmelding(
    val foresporsel: Foresporsel?,
    val notat: Notat?,
)

data class Foresporsel(
    val type: KodeverkVerdi,
    val sporsmal: String?,
)

data class Notat(
    val tema: KodeverkVerdi,
    val temaBeskrivelse: String?,
    val innhold: String?,
    val dato: LocalDate?,
)

data class IncomingVedlegg(
    val date: OffsetDateTime?,
    val description: String?,
    val mimeType: String?,
    val data: InputStream?,
)
