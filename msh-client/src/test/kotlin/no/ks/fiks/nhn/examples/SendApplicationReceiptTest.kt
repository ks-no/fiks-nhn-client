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
    "Sending an OK example application receipt" {
        val client = mockk<ClientWithFastlegeLookup> { coEvery { sendApplicationReceipt(any(), any()) } returns UUID.randomUUID() }
        client.sendApplicationReceipt(
            OutgoingApplicationReceipt(
                acknowledgedId = UUID.randomUUID(), // Id of the received message that this app rec is acknowledging
                senderHerId = 1234,
                status = StatusForMottakAvMelding.OK,
            )
        )
    }

    "Sending an OK_FEIL_I_DELMELDING example application receipt" {
        val client = mockk<ClientWithFastlegeLookup> { coEvery { sendApplicationReceipt(any(), any()) } returns UUID.randomUUID() }
        client.sendApplicationReceipt(
            OutgoingApplicationReceipt(
                acknowledgedId = UUID.randomUUID(),
                senderHerId = 1234,
                status = StatusForMottakAvMelding.OK_FEIL_I_DELMELDING,
                errors = listOf(
                    OutgoingApplicationReceiptError(
                        FeilmeldingForApplikasjonskvittering.ANNEN_FEIL,
                        "Details describing the error",
                    ),
                    OutgoingApplicationReceiptError(
                        FeilmeldingForApplikasjonskvittering.PASIENT_MANGLER_NAVN,
                    ),
                ),
            )
        )
    }

    "Sending an AVVIST example application receipt" {
        val client = mockk<ClientWithFastlegeLookup> { coEvery { sendApplicationReceipt(any(), any()) } returns UUID.randomUUID() }
        client.sendApplicationReceipt(
            OutgoingApplicationReceipt(
                acknowledgedId = UUID.randomUUID(),
                senderHerId = 1234,
                status = StatusForMottakAvMelding.AVVIST,
                errors = listOf(
                    OutgoingApplicationReceiptError(
                        FeilmeldingForApplikasjonskvittering.UGYLDIG_XML,
                        "XML is invalid",
                    )
                ),
            )
        )
    }
})