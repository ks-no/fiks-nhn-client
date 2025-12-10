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

class SendToPersonGPTest : StringSpec({
    "Sending an example message to GP for person" {
        val client = mockk<ClientWithFastlegeLookup> { coEvery { sendMessageToGPForPerson(any(), any()) } returns UUID.randomUUID() }
        client.sendMessageToGPForPerson(
            GPForPersonOutgoingBusinessDocument(
                id = UUID.randomUUID(), // This id will be used to connect an application receipt to this message
                sender = Sender(
                    parent = OrganizationCommunicationParty(
                        name = "<Name of sender organization (virksomhet)>",
                        ids = listOf(
                            OrganizationId(
                                id = "<Sender organization HER-id>",
                                type = OrganizationIdType.HER_ID,
                            ),
                        ),
                    ),
                    child = OrganizationCommunicationParty(
                        name = "<Name of the sending service (tjeneste), which is owned by the organization specified above>",
                        ids = listOf(
                            OrganizationId(
                                id = "<The HER-id of this service>",
                                type = OrganizationIdType.HER_ID,
                            )
                        ),
                    )
                ),
                person = Person(
                    fnr = "<Id of the patient the message is about (fødselsnummer, etc.)>",
                    firstName = "<First name>",
                    middleName = "<Middle name>",
                    lastName = "<Last name>",
                ),
                message = OutgoingMessage(
                    subject = "<Message subject>",
                    body = "<Message body>",
                    responsibleHealthcareProfessional = HealthcareProfessional(
                        // Person responsible for this message at the sender
                        firstName = "<First name>",
                        middleName = "<Middle name>",
                        lastName = "<Last name>",
                        phoneNumber = "11223344",
                        roleToPatient = HelsepersonellsFunksjoner.HELSEFAGLIG_KONTAKT, // This persons role with respect to the patient
                    ),
                    recipientContact = RecipientContact(
                        // Required for Dialogmelding 1.1 Helsefaglig Dialog when temaKodet is "Henvendelse om pasient" (which is currently the only option and is chosen automatically)
                        type = Helsepersonell.LEGE, // Professional group of the healthcare professional recieving the message
                    )
                ),
                vedlegg = OutgoingVedlegg(
                    // Only PDF is supported
                    date = OffsetDateTime.now(), // Creation time for the attachment
                    description = "<Description of the attachment>",
                    data = ClassLoader.getSystemResourceAsStream("small.pdf")!!, // InputStream containing the bytes for the attached PDF
                ),
                version = DialogmeldingVersion.V1_1, // 1.0 is also supported, but 1.1 is preferred and will be the main focus
            )
        )
    }
})