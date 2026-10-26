package no.ks.fiks.nhn.msh

import java.time.OffsetDateTime
import java.util.UUID

enum class NotificationType {
    NewMessage,
    RefusedMessage,
    MessageSentStateUpdated,
    MessageApprecInfoUpdated,
    MessageDeliveryStateUpdated,
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
