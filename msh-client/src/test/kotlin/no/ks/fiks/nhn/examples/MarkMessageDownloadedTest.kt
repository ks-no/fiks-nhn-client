package no.ks.fiks.nhn.examples

import io.kotest.core.spec.style.StringSpec
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.ks.fiks.nhn.msh.ClientWithFastlegeLookup
import java.util.*

class MarkMessageDownloadedTest : StringSpec({
    "Example for marking message as downloaded" {
        val client = mockk<ClientWithFastlegeLookup> { coEvery { markMessageDownloaded(any(), any()) } just runs }
        client.markMessageDownloaded(
            id = UUID.randomUUID(),
            receiverHerId = 2345,
        )
    }
})