package no.ks.fiks.nhn.examples

import io.kotest.core.spec.style.StringSpec
import io.mockk.coEvery
import io.mockk.mockk
import no.ks.fiks.nhn.msh.ClientWithFastlegeLookup
import no.ks.fiks.nhn.msh.NotificationType
import no.ks.fiks.nhn.msh.UnreadNotification
import java.time.OffsetDateTime
import java.util.UUID

class GetMessagesWithMetadataTest : StringSpec({
    "returns message metadata for unread notifications" {
        val client = mockk<ClientWithFastlegeLookup> {
            coEvery { getUnreadNotifications(any(), any(), any()) } returns listOf(
                UnreadNotification(
                    notificationId = UUID.randomUUID().toString(),
                    relatedMessageId = UUID.randomUUID().toString(),
                    type = NotificationType.NEW_MESSAGE,
                    notificationReceiverHerId = 321,
                    notificationTriggeredByHerId = 123,
                    description = "new message",
                    createdAt = OffsetDateTime.now(),
                ),
                UnreadNotification(
                    notificationId = UUID.randomUUID().toString(),
                    relatedMessageId = null,
                    type = NotificationType.NEW_MESSAGE,
                    notificationReceiverHerId = 321,
                    notificationTriggeredByHerId = 123,
                    description = "missing message id",
                    createdAt = OffsetDateTime.now(),
                )
            )
        }

        client.getUnreadNotifications(herIds = listOf(321))
            .forEach { client.getMessage(it.notificationId) }
    }
})