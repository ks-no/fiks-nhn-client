package no.ks.fiks.nhn.examples

import io.kotest.core.spec.style.StringSpec
import io.mockk.coEvery
import io.mockk.mockk
import no.ks.fiks.nhn.msh.ClientWithFastlegeLookup
import no.ks.fiks.nhn.msh.MessageWithMetadata
import no.ks.fiks.nhn.msh.NotificationType
import no.ks.fiks.nhn.msh.UnreadNotification
import java.time.OffsetDateTime
import java.util.UUID

class GetMessagesWithMetadataTest : StringSpec({
    "returns message metadata for unread notifications" {
        val messageId = UUID.randomUUID().toString()
        val messageId2 = UUID.randomUUID().toString()
        val metadata = mockk<MessageWithMetadata>()
        val client = mockk<ClientWithFastlegeLookup> {
            coEvery { getMessage(messageId, any()) } returns metadata
            coEvery { getMessage(messageId2, any()) } returns metadata
            coEvery { getUnreadNotifications(any(), any(), any()) } returns listOf(
                UnreadNotification(
                    notificationId = UUID.randomUUID().toString(),
                    relatedMessageId = messageId,
                    type = NotificationType.NEW_MESSAGE,
                    notificationReceiverHerId = 321,
                    notificationTriggeredByHerId = 123,
                    description = "new message",
                    createdAt = OffsetDateTime.now(),
                ),
                UnreadNotification(
                    notificationId = UUID.randomUUID().toString(),
                    relatedMessageId = messageId2,
                    type = NotificationType.NEW_MESSAGE,
                    notificationReceiverHerId = 321,
                    notificationTriggeredByHerId = 123,
                    description = "another new message",
                    createdAt = OffsetDateTime.now(),
                )
            )
        }

        client.getUnreadNotifications(herIds = listOf(321))
            .filter { it.type == NotificationType.NEW_MESSAGE }
            .mapNotNull { it.relatedMessageId }
            .forEach { client.getMessage(it) }
    }
})