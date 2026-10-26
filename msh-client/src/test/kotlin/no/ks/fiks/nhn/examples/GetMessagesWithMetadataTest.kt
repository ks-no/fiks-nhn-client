package no.ks.fiks.nhn.examples

import io.kotest.core.spec.style.StringSpec
import io.mockk.coEvery
import io.mockk.mockk
import no.ks.fiks.nhn.msh.ClientWithFastlegeLookup
import no.ks.fiks.nhn.msh.NotificationType

class GetMessagesWithMetadataTest : StringSpec({
    "Get unread notifications with metadata example" {
        val client = mockk<ClientWithFastlegeLookup> { coEvery { getUnreadNotifications(any(), any(), any()) } returns listOf() }
        client.getUnreadNotifications(
            herIds = listOf(321),
        )
            .filter { it.type == NotificationType.NewMessage }
            .mapNotNull { it.relatedMessageId }
            .map { client.getMessage(it) } // Returned messages include extra metadata, including the business document id and message type
    }
})