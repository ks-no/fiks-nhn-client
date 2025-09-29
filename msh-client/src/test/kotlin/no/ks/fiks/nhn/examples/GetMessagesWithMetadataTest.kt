package no.ks.fiks.nhn.examples

import io.kotest.core.spec.style.StringSpec
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.ks.fiks.nhn.msh.ClientWithFastlegeLookup
import java.util.*

class GetMessagesWithMetadataTest : StringSpec({
    "Get messages with metadata example" {
        val client = mockk<ClientWithFastlegeLookup> { coEvery { getMessagesWithMetadata(any(), any()) } returns listOf() }
        client.getMessagesWithMetadata( // Returned messages include extra metadata, including the business document id, and whether it is an application receipt
            receiverHerId = 321,
        )
    }
})