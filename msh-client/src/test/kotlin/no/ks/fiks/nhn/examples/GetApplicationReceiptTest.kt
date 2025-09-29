package no.ks.fiks.nhn.examples

import io.kotest.core.spec.style.StringSpec
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.ks.fiks.nhn.msh.ClientWithFastlegeLookup
import java.util.*

class GetApplicationReceiptTest : StringSpec({
    "Get application receipt example" {
        val client = mockk<ClientWithFastlegeLookup> { coEvery { getApplicationReceipt(any(), any()) } returns mockk() }

        // Gets a business document that is an application receipt, using the business document id returned when getting messages with metadata
        // Includes the status sent by the recipient of the original message and, if it is an error, any relevant error messages
        client.getApplicationReceipt(
            id = UUID.randomUUID(),
        )
    }
})