package no.ks.fiks.nhn.examples

import io.kotest.core.spec.style.StringSpec
import io.mockk.coEvery
import io.mockk.mockk
import no.ks.fiks.hdir.FeilmeldingForApplikasjonskvittering
import no.ks.fiks.hdir.Helsepersonell
import no.ks.fiks.hdir.HelsepersonellsFunksjoner
import no.ks.fiks.hdir.OrganizationIdType
import no.ks.fiks.hdir.StatusForMottakAvMelding
import no.ks.fiks.nhn.msh.*
import java.time.OffsetDateTime
import java.util.*

class SendApplicationReceiptTest : StringSpec({
    "Sending an example application receipt" {
        val client = mockk<ClientWithFastlegeLookup> { coEvery { sendApplicationReceipt(any(), any()) } returns UUID.randomUUID() }
        client.sendApplicationReceipt(
            OutgoingApplicationReceipt(
                acknowledgedId = UUID.randomUUID(), // Id of the received message that this app rec is acknowledging
                senderHerId = 1234,
                status = StatusForMottakAvMelding.AVVIST,
                errors = listOf( // Not allowed when status is OK, required otherwise
                    OutgoingApplicationReceiptError(
                        FeilmeldingForApplikasjonskvittering.ANNEN_FEIL,
                        "Details describing the error",
                    )
                ),
            )
        )
    }
})