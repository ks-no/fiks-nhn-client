package no.ks.fiks.nhn.examples

import io.kotest.core.spec.style.StringSpec
import io.mockk.coEvery
import io.mockk.mockk
import no.ks.fiks.nhn.msh.ClientWithFastlegeLookup

class GetUnreadNotificationsTest : StringSpec({
    "Get unread notifications example" {
        val client = mockk<ClientWithFastlegeLookup> { coEvery { getUnreadNotifications(any(), any(), any()) } returns listOf() }
        client.getUnreadNotifications(
            herIds = listOf(34567),
            notificationsToFetch = 5
        )
    }
})