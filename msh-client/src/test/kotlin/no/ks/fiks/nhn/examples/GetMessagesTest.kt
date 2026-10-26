package no.ks.fiks.nhn.examples

import io.kotest.core.spec.style.StringSpec
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.ks.fiks.nhn.msh.ClientWithFastlegeLookup
import java.util.*

class GetMessagesTest : StringSpec({
    "Get messages example" {
        val client = mockk<ClientWithFastlegeLookup> { coEvery { getMessages(any(), any()) } returns listOf() }
        client.getMessages(
            receiverHerId = 34567,
        )
    }
})