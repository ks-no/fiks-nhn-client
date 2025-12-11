package no.ks.fiks.nhn.edi

import io.kotest.assertions.asClue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import no.ks.fiks.hdir.Helsepersonell
import no.ks.fiks.hdir.HelsepersonellsFunksjoner
import no.ks.fiks.hdir.OrganizationIdType
import no.ks.fiks.hdir.PersonIdType
import no.ks.fiks.nhn.msh.*
import no.ks.fiks.nhn.validateXmlAgainst
import java.io.ByteArrayInputStream
import java.time.OffsetDateTime
import java.util.*
import kotlin.random.Random.Default.nextBytes
import kotlin.random.Random.Default.nextInt

class BusinessDocumentSerializerTest : StringSpec({

    "Test serialization of Dialogmelding 1.0 message with person sender child" {
        val start = OffsetDateTime.now().minusSeconds(1)
        val vedleggBytes = nextBytes(nextInt(100, 1000))
        val document = randomOutgoingBusinessDocument(DialogmeldingVersion.V1_0, vedleggBytes, senderChild = randomPersonCommunicationParty())

        BusinessDocumentSerializer.serializeNhnMessage(document)
            .validateXmlAgainst(start, document, vedleggBytes)
    }

    "Test serialization of Dialogmelding 1.0 message with organization sender child" {
        val start = OffsetDateTime.now().minusSeconds(1)
        val vedleggBytes = nextBytes(nextInt(100, 1000))
        val document = randomOutgoingBusinessDocument(DialogmeldingVersion.V1_0, vedleggBytes, senderChild = randomOrganizationCommunicationParty())

        BusinessDocumentSerializer.serializeNhnMessage(document)
            .validateXmlAgainst(start, document, vedleggBytes)
    }

    "Test serialization of Dialogmelding 1.0 message with person receiver child" {
        val start = OffsetDateTime.now().minusSeconds(1)
        val vedleggBytes = nextBytes(nextInt(100, 1000))
        val document = randomOutgoingBusinessDocument(DialogmeldingVersion.V1_0, vedleggBytes, receiverChild = randomPersonCommunicationParty())

        BusinessDocumentSerializer.serializeNhnMessage(document)
            .validateXmlAgainst(start, document, vedleggBytes)
    }

    "Test serialization of Dialogmelding 1.0 message with organization receiver child" {
        val start = OffsetDateTime.now().minusSeconds(1)
        val vedleggBytes = nextBytes(nextInt(100, 1000))
        val document = randomOutgoingBusinessDocument(DialogmeldingVersion.V1_0, vedleggBytes, receiverChild = randomOrganizationCommunicationParty())

        BusinessDocumentSerializer.serializeNhnMessage(document)
            .validateXmlAgainst(start, document, vedleggBytes)
    }

    "Test serialization of Dialogmelding 1.1 message with person sender child" {
        val start = OffsetDateTime.now().minusSeconds(1)
        val vedleggBytes = nextBytes(nextInt(100, 1000))
        val document = randomOutgoingBusinessDocument(DialogmeldingVersion.V1_1, vedleggBytes, senderChild = randomPersonCommunicationParty())

        BusinessDocumentSerializer.serializeNhnMessage(document)
            .validateXmlAgainst(start, document, vedleggBytes)
    }

    "Test serialization of Dialogmelding 1.1 message with organization sender child" {
        val start = OffsetDateTime.now().minusSeconds(1)
        val vedleggBytes = nextBytes(nextInt(100, 1000))
        val document = randomOutgoingBusinessDocument(DialogmeldingVersion.V1_1, vedleggBytes, senderChild = randomOrganizationCommunicationParty())

        BusinessDocumentSerializer.serializeNhnMessage(document)
            .validateXmlAgainst(start, document, vedleggBytes)
    }

    "Test serialization of Dialogmelding 1.1 message with person receiver child" {
        val start = OffsetDateTime.now().minusSeconds(1)
        val vedleggBytes = nextBytes(nextInt(100, 1000))
        val document = randomOutgoingBusinessDocument(DialogmeldingVersion.V1_1, vedleggBytes, receiverChild = randomPersonCommunicationParty())

        BusinessDocumentSerializer.serializeNhnMessage(document)
            .validateXmlAgainst(start, document, vedleggBytes)
    }

    "Test serialization of Dialogmelding 1.1 message with organization receiver child" {
        val start = OffsetDateTime.now().minusSeconds(1)
        val vedleggBytes = nextBytes(nextInt(100, 1000))
        val document = randomOutgoingBusinessDocument(DialogmeldingVersion.V1_1, vedleggBytes, receiverChild = randomOrganizationCommunicationParty())

        BusinessDocumentSerializer.serializeNhnMessage(document)
            .validateXmlAgainst(start, document, vedleggBytes)
    }

    "A vedlegg of size 18 MB should be accepted" {
        val vedleggBytes = nextBytes(18000000)
        val document = randomOutgoingBusinessDocument(DialogmeldingVersion.V1_1, vedleggBytes, randomOrganizationCommunicationParty())

        BusinessDocumentSerializer.serializeNhnMessage(document)
    }

    "If vedlegg has size greater than 18 MB, an exception should be thrown" {
        val vedleggBytes = nextBytes(18000001)
        val document = randomOutgoingBusinessDocument(DialogmeldingVersion.V1_1, vedleggBytes, randomOrganizationCommunicationParty())

        shouldThrow<VedleggSizeException> {  BusinessDocumentSerializer.serializeNhnMessage(document) }.asClue {
            it.message shouldBe "The size of vedlegg exceeds the max size of 18000000 bytes"
        }
    }

})

private fun randomOutgoingBusinessDocument(
    version: DialogmeldingVersion,
    vedleggBytes: ByteArray,
    senderParent: OrganizationCommunicationParty = randomOrganizationCommunicationParty(),
    senderChild: CommunicationParty = randomPersonCommunicationParty(),
    receiverParent: OrganizationCommunicationParty = randomOrganizationCommunicationParty(),
    receiverChild: CommunicationParty = randomPersonCommunicationParty(),
): OutgoingBusinessDocument = OutgoingBusinessDocument(
    id = UUID.randomUUID(),
    sender = Sender(
        parent = senderParent,
        child = senderChild,
    ),
    receiver = Receiver(
        parent = receiverParent,
        child = receiverChild,
        patient = Patient(
            fnr = UUID.randomUUID().toString(),
            firstName = UUID.randomUUID().toString(),
            middleName = UUID.randomUUID().toString(),
            lastName = UUID.randomUUID().toString(),
        ),
    ),
    message = OutgoingMessage(
        subject = UUID.randomUUID().toString(),
        body = UUID.randomUUID().toString() + "\n" + UUID.randomUUID().toString(),
        responsibleHealthcareProfessional = HealthcareProfessional(
            firstName = UUID.randomUUID().toString(),
            middleName = UUID.randomUUID().toString(),
            lastName = UUID.randomUUID().toString(),
            phoneNumber = UUID.randomUUID().toString(),
            roleToPatient = HelsepersonellsFunksjoner.entries.random(),
        ),
        recipientContact = RecipientContact(Helsepersonell.entries.random()),
    ),
    vedlegg = OutgoingVedlegg(
        date = OffsetDateTime.now(),
        description = UUID.randomUUID().toString() + "\n" + UUID.randomUUID().toString(),
        data = ByteArrayInputStream(vedleggBytes),
    ),
    version = version,
)

private fun randomPersonCommunicationParty() = PersonCommunicationParty(
    firstName = UUID.randomUUID().toString(),
    middleName = UUID.randomUUID().toString(),
    lastName = UUID.randomUUID().toString(),
    ids = listOf(PersonId(UUID.randomUUID().toString(), PersonIdType.entries.random())),
)

private fun randomOrganizationCommunicationParty() = OrganizationCommunicationParty(
    name = UUID.randomUUID().toString(),
    ids = listOf(OrganizationId(UUID.randomUUID().toString(), OrganizationIdType.entries.random())),
)
