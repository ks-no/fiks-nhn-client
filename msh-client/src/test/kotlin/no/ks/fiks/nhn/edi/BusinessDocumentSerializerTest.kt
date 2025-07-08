package no.ks.fiks.nhn.edi

import io.kotest.core.spec.style.StringSpec
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

    "Test serialization of Dialogmelding 1.0 message with person receiver child" {
        val start = OffsetDateTime.now().minusSeconds(1)
        val vedleggBytes = nextBytes(nextInt(100, 1000))
        val document = randomOutgoingBusinessDocument(DialogmeldingVersion.V1_0, vedleggBytes, randomPersonReceiverDetails())

        BusinessDocumentSerializer.serializeNhnMessage(document)
            .validateXmlAgainst(start, document, vedleggBytes)
    }

    "Test serialization of Dialogmelding 1.0 message with organization receiver child" {
        val start = OffsetDateTime.now().minusSeconds(1)
        val vedleggBytes = nextBytes(nextInt(100, 1000))
        val document = randomOutgoingBusinessDocument(DialogmeldingVersion.V1_0, vedleggBytes, randomOrganizationReceiverDetails())

        BusinessDocumentSerializer.serializeNhnMessage(document)
            .validateXmlAgainst(start, document, vedleggBytes)
    }

    "Test serialization of Dialogmelding 1.1 message with person receiver child" {
        val start = OffsetDateTime.now().minusSeconds(1)
        val vedleggBytes = nextBytes(nextInt(100, 1000))
        val document = randomOutgoingBusinessDocument(DialogmeldingVersion.V1_1, vedleggBytes, randomPersonReceiverDetails())

        BusinessDocumentSerializer.serializeNhnMessage(document)
            .validateXmlAgainst(start, document, vedleggBytes)
    }

    "Test serialization of Dialogmelding 1.1 message with organization receiver child" {
        val start = OffsetDateTime.now().minusSeconds(1)
        val vedleggBytes = nextBytes(nextInt(100, 1000))
        val document = randomOutgoingBusinessDocument(DialogmeldingVersion.V1_1, vedleggBytes, randomOrganizationReceiverDetails())

        BusinessDocumentSerializer.serializeNhnMessage(document)
            .validateXmlAgainst(start, document, vedleggBytes)
    }

})

private fun randomOutgoingBusinessDocument(
    version: DialogmeldingVersion,
    vedleggBytes: ByteArray,
    receiverChild: ReceiverDetails = randomPersonReceiverDetails(),
): OutgoingBusinessDocument = OutgoingBusinessDocument(
    id = UUID.randomUUID(),
    sender = Organization(
        name = UUID.randomUUID().toString(),
        ids = listOf(OrganizationId(UUID.randomUUID().toString(), OrganizationIdType.entries.random())),
        childOrganization = ChildOrganization(
            name = UUID.randomUUID().toString(),
            ids = listOf(OrganizationId(UUID.randomUUID().toString(), OrganizationIdType.entries.random())),
        )
    ),
    receiver = Receiver(
        parent = OrganizationReceiverDetails(
            name = UUID.randomUUID().toString(),
            ids = listOf(OrganizationId(UUID.randomUUID().toString(), OrganizationIdType.entries.random())),
        ),
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

private fun randomPersonReceiverDetails() = PersonReceiverDetails(
    firstName = UUID.randomUUID().toString(),
    middleName = UUID.randomUUID().toString(),
    lastName = UUID.randomUUID().toString(),
    ids = listOf(PersonId(UUID.randomUUID().toString(), PersonIdType.entries.random())),
)

private fun randomOrganizationReceiverDetails() = OrganizationReceiverDetails(
    name = UUID.randomUUID().toString(),
    ids = listOf(OrganizationId(UUID.randomUUID().toString(), OrganizationIdType.entries.random())),
)
