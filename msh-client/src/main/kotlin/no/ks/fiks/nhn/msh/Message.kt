package no.ks.fiks.nhn.msh

import java.time.OffsetDateTime
import java.util.*

data class Message(
    val id: UUID,
    val receiverHerId: Int,
)

data class MessageWithMetadata(
    val id: UUID,
    val contentType: String,
    val receiverHerId: Int,
    val senderHerId: Int,
    val businessDocumentId: String,
    val businessDocumentDate: OffsetDateTime?,
    val isAppRec: Boolean,
)
