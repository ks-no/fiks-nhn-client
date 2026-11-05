package no.ks.fiks.nhn.msh

import java.time.OffsetDateTime
import java.util.UUID

enum class NotificationType(val value: String) {
    NEW_MESSAGE("NewMessage"),
    REFUSED_MESSAGE("RefusedMessage"),
    MESSAGE_SENT_STATE_UPDATED("MessageSentStateUpdated"),
    MESSAGE_APPREC_INFO_UPDATED("MessageApprecInfoUpdated"),
    MESSAGE_DELIVERY_STATE_UPDATED("MessageDeliveryStateUpdated"),
    ;

    override fun toString() = value
}

data class UnreadNotification(
    val notificationId: UUID,
    val relatedMessageId: UUID?,
    val type: NotificationType,
    val notificationReceiverHerId: Int,
    val notificationTriggeredByHerId: Int?,
    val description: String?,
    val createdAt: OffsetDateTime?,
)
