package no.ks.fiks.nhn.msh

import java.time.OffsetDateTime
import no.nhn.msh.v3.model.NotificationType as NhnNotificationType

enum class NotificationType(val value: String) {
    NEW_MESSAGE("NewMessage"),
    REFUSED_MESSAGE("RefusedMessage"),
    MESSAGE_SENT_STATE_UPDATED("MessageSentStateUpdated"),
    MESSAGE_APPREC_INFO_UPDATED("MessageApprecInfoUpdated"),
    MESSAGE_DELIVERY_STATE_UPDATED("MessageDeliveryStateUpdated"),
    ;

    override fun toString() = value
}

internal fun NhnNotificationType.toNotificationType() = when (this) {
    NhnNotificationType.NEW_MESSAGE -> NotificationType.NEW_MESSAGE
    NhnNotificationType.REFUSED_MESSAGE -> NotificationType.REFUSED_MESSAGE
    NhnNotificationType.MESSAGE_SENT_STATE_UPDATED -> NotificationType.MESSAGE_SENT_STATE_UPDATED
    NhnNotificationType.MESSAGE_APPREC_INFO_UPDATED -> NotificationType.MESSAGE_APPREC_INFO_UPDATED
    NhnNotificationType.MESSAGE_DELIVERY_STATE_UPDATED -> NotificationType.MESSAGE_DELIVERY_STATE_UPDATED
}

data class UnreadNotification(
    val notificationId: String,
    val relatedMessageId: String?,
    val type: NotificationType,
    val notificationReceiverHerId: Int,
    val notificationTriggeredByHerId: Int?,
    val description: String?,
    val createdAt: OffsetDateTime?,
)
