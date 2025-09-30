package no.ks.fiks.nhn.examples

import io.kotest.core.spec.style.StringSpec
import io.mockk.coEvery
import io.mockk.mockk
import no.ks.fiks.hdir.Helsepersonell
import no.ks.fiks.hdir.HelsepersonellsFunksjoner
import no.ks.fiks.hdir.OrganizationIdType
import no.ks.fiks.nhn.msh.*
import java.time.OffsetDateTime
import java.util.*

class RequestParametersTest : StringSpec({
    "Sending an example message to GP for person" {
        val client = mockk<ClientWithFastlegeLookup> { coEvery { getMessages(any(), any()) } returns listOf() }
        client.getMessages( // Returned messages only include an id and HER-id of the receiver, without any additional metadata
            receiverHerId = 34567,
            requestParameters = RequestParameters(
                helseId = HelseIdTokenParameters(
                    tenant = MultiTenantHelseIdTokenParameters( // Can also be single tenant
                        parentOrganization = "111111111", // Organization number
                        childOrganization = "222222222", // Organization number
                    )
                ),
            ),
        )
    }
})