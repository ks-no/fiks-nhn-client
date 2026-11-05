package no.ks.fiks.nhn.examples

import io.kotest.core.spec.style.StringSpec
import io.mockk.coEvery
import io.mockk.mockk
import no.ks.fiks.hdir.Adressetype
import no.ks.fiks.hdir.Helsepersonell
import no.ks.fiks.hdir.HelsepersonellsFunksjoner
import no.ks.fiks.hdir.OrganizationIdType
import no.ks.fiks.hdir.PersonIdType
import no.ks.fiks.nhn.msh.*
import java.time.OffsetDateTime
import java.util.*

class SendMessageToHerIdTest : StringSpec({
    "Sending an example message to a person identified by HER-id" {
        val client = mockk<Client> { coEvery { sendMessage(any(), any()) } returns UUID.randomUUID() }
        client.sendMessage(
            OutgoingBusinessDocument(
                id = UUID.randomUUID(),
                sender = Sender(
                    parent = OrganizationCommunicationParty(
                        name = "<Name of sender organization (virksomhet)>",
                        ids = listOf(
                            OrganizationId(
                                id = "<Sender organization HER-id>",
                                type = OrganizationIdType.HER_ID,
                            )
                        ),
                        address = Address(
                            type = Adressetype.POSTADRESSE,
                            streetAdr = "<Street address>",
                            postalCode = "<Postal code>",
                            city = "<City>",
                            postbox = "<Postbox>",
                            county = County(code = "<County code>", name = "<County name>"),
                            country = Country(code = "<Country code>", name = "<Country name>"),
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
                        address = Address(
                            type = Adressetype.POSTADRESSE,
                            streetAdr = "<Street address>",
                            postalCode = "<Postal code>",
                            city = "<City>",
                            postbox = "<Postbox>",
                            county = County(code = "<County code>", name = "<County name>"),
                            country = Country(code = "<Country code>", name = "<Country name>"),
                        ),
                    )
                ),
                receiver = Receiver(
                    parent = OrganizationCommunicationParty(
                        name = "<Name of the receiving organization (virksomhet)>",
                        ids = listOf(
                            OrganizationId(
                                id = "<Receiving organization HER-id>",
                                type = OrganizationIdType.HER_ID,
                            )
                        ),
                        address = Address(
                            type = Adressetype.POSTADRESSE,
                            streetAdr = "<Street address>",
                            postalCode = "<Postal code>",
                            city = "<City>",
                            postbox = "<Postbox>",
                            county = County(code = "<County code>", name = "<County name>"),
                            country = Country(code = "<Country code>", name = "<Country name>"),
                        ),
                    ),
                    child = PersonCommunicationParty(
                        firstName = "<First name of receiving person>",
                        middleName = "<Middle name of receiving person>",
                        lastName = "<Last name of receiving person>",
                        ids = listOf(
                            PersonId(
                                id = "<Receiving person HER-id>",
                                type = PersonIdType.HER_ID,
                            )
                        ),
                        address = Address(
                            type = Adressetype.POSTADRESSE,
                            streetAdr = "<Street address>",
                            postalCode = "<Postal code>",
                            city = "<City>",
                            postbox = "<Postbox>",
                            county = County(code = "<County code>", name = "<County name>"),
                            country = Country(code = "<Country code>", name = "<Country name>"),
                        ),
                    ),
                    patient = Patient(
                        fnr = "<Id of the patient the message is about (fødselsnummer, etc.)>",
                        firstName = "<First name>",
                        middleName = "<Middle name>",
                        lastName = "<Last name>",
                    ),
                ),
                message = OutgoingMessage(
                    subject = "<Message subject>",
                    body = "<Message body>",
                    responsibleHealthcareProfessional = HealthcareProfessional(
                        firstName = "<First name>",
                        middleName = "<Middle name>",
                        lastName = "<Last name>",
                        phoneNumber = "11223344",
                        roleToPatient = HelsepersonellsFunksjoner.HELSEFAGLIG_KONTAKT,
                    ),
                    recipientContact = RecipientContact(
                        type = Helsepersonell.LEGE,
                    )
                ),
                vedlegg = listOf(
                    OutgoingVedlegg(
                        date = OffsetDateTime.now(),
                        description = "<Description of the attachment>",
                        data = ClassLoader.getSystemResourceAsStream("small.pdf")!!,
                    )
                ),
                version = DialogmeldingVersion.V1_1,
                conversationRef = ConversationRef(
                    refToParent = "<Reference to the previous message of this dialogue, should contain the same value as the id field of that message>",
                    refToConversation = "Reference to the first message of this dialogue, should contain the same value as id field of that message>",
                )
            )
        )
    }
})