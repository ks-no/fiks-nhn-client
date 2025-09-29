package no.ks.fiks.nhn.examples

import io.kotest.core.spec.style.StringSpec
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.ks.fiks.nhn.msh.ClientWithFastlegeLookup
import java.util.*

class GetBusinessDocumentTest : StringSpec({
    "Get business document example" {
        val client = mockk<ClientWithFastlegeLookup> { coEvery { getBusinessDocument(any(), any()) } returns mockk() }

        // Gets a business document that is not an application receipt, using the business document id returned when getting messages with metadata
        client.getBusinessDocument(
            id = UUID.randomUUID(),
        )
    }
})