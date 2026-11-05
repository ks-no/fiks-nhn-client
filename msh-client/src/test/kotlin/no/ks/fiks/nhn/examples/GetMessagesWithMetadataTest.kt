package no.ks.fiks.nhn.examples

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.ks.fiks.nhn.msh.ClientWithFastlegeLookup
import no.ks.fiks.nhn.msh.MessageWithMetadata
import no.ks.fiks.nhn.msh.NotificationType
import no.ks.fiks.nhn.msh.UnreadNotification
import java.time.OffsetDateTime
import java.util.UUID

class GetMessagesWithMetadataTest : StringSpec({
    "returns empty list when there are no unread notifications" {
        val client = mockk<ClientWithFastlegeLookup> {
            coEvery { getUnreadNotifications(any(), any(), any()) } returns emptyList()
        }

        val result = client.getUnreadNotifications(herIds = listOf(321))

        result shouldBe emptyList()
    }

    "returns message metadata for unread notifications with related messages" {
        val relatedMessageId = UUID.randomUUID()
        val expected = MessageWithMetadata(
            id = relatedMessageId,
            contentType = "application/xml",
            receiverHerIds = listOf(321),
            senderHerId = 123,
            businessDocumentId = "business-document-1",
            businessDocumentDate = OffsetDateTime.now(),
            businessDocumentMsgType = "message-type",
        )

        val client = mockk<ClientWithFastlegeLookup> {
            coEvery { getUnreadNotifications(any(), any(), any()) } returns listOf(
                UnreadNotification(
                    notificationId = UUID.randomUUID(),
                    relatedMessageId = relatedMessageId,
                    type = NotificationType.NEW_MESSAGE,
                    notificationReceiverHerId = 321,
                    notificationTriggeredByHerId = 123,
                    description = "new message",
                    createdAt = OffsetDateTime.now(),
                ),
                UnreadNotification(
                    notificationId = UUID.randomUUID(),
                    relatedMessageId = null,
                    type = NotificationType.NEW_MESSAGE,
                    notificationReceiverHerId = 321,
                    notificationTriggeredByHerId = 123,
                    description = "missing message id",
                    createdAt = OffsetDateTime.now(),
                ),
                UnreadNotification(
                    notificationId = UUID.randomUUID(),
                    relatedMessageId = UUID.randomUUID(),
                    type = NotificationType.REFUSED_MESSAGE,
                    notificationReceiverHerId = 321,
                    notificationTriggeredByHerId = 123,
                    description = "ignored type",
                    createdAt = OffsetDateTime.now(),
                ),
            )
            coEvery { getMessage(relatedMessageId) } returns expected
        }

        val result = client.getUnreadNotifications(herIds = listOf(321))
            .filter { it.type == NotificationType.NEW_MESSAGE }
            .mapNotNull { it.relatedMessageId }
            .map { client.getMessage(it) }

        result shouldBe listOf(expected)
    }
})