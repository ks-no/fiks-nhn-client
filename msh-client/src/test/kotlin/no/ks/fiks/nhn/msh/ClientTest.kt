package no.ks.fiks.nhn.msh

import io.kotest.assertions.asClue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.beNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import no.ks.fiks.hdir.FeilmeldingForApplikasjonskvittering
import no.ks.fiks.hdir.Helsepersonell
import no.ks.fiks.hdir.HelsepersonellsFunksjoner
import no.ks.fiks.hdir.StatusForMottakAvMelding
import no.ks.fiks.nhn.*
import no.nhn.msh.v2.model.*
import java.io.ByteArrayInputStream
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.random.Random.Default.nextBoolean
import kotlin.random.Random.Default.nextBytes
import kotlin.random.Random.Default.nextInt

class ClientTest : FreeSpec() {

    init {

        "Send message" - {
            "The data should be serialized to XML and passed on to the service" {
                val startTime = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS)
                val vedleggBytes = nextBytes(nextInt(1000, 100000))
                val businessDocument = randomOutgoingBusinessDocument(vedleggBytes)

                val requestSlot = slot<PostMessageRequest>()
                val internalClient = mockk<MshInternalClient> {
                    coEvery { postMessage(capture(requestSlot), any()) } returns UUID.randomUUID()
                }
                val client = Client(internalClient)

                client.sendMessage(businessDocument)

                coVerifySequence {
                    internalClient.postMessage(any(), any())
                }

                requestSlot.captured.asClue { request ->
                    request.contentType shouldBe "application/xml"
                    request.contentTransferEncoding shouldBe "base64"

                    val xml = String(Base64.getDecoder().decode(request.businessDocument))
                    xml.validateXmlAgainst(
                        startTime = startTime,
                        document = businessDocument,
                        vedleggBytes = vedleggBytes,
                    )
                }
            }

            "Params should be passed on" {
                val params = RequestParameters(HelseIdTokenParameters(SingleTenantHelseIdTokenParameters(randomHerId().toString())))

                val internalClient = mockk<MshInternalClient> {
                    coEvery { postMessage(any(), any()) } returns UUID.randomUUID()
                }
                val client = Client(internalClient)

                client.sendMessage(randomOutgoingBusinessDocument(), params)
                coVerifySequence {
                    internalClient.postMessage(any(), params)
                }
            }
        }

        "Get messages" - {
            "Messages should be retrieved and mapped" {
                val apiMessages = List(nextInt(0, 10)) { randomApiMessageWithoutMetadata() }

                val internalClient = mockk<MshInternalClient> {
                    coEvery { getMessages(any(), any()) } returns apiMessages
                }
                val client = Client(internalClient)

                val receiverHerId = randomHerId()
                client.getMessages(receiverHerId).asClue {
                    it.forEachIndexed { i, message ->
                        message.id shouldBe apiMessages[i].id
                        message.receiverHerId shouldBe apiMessages[i].receiverHerId
                    }
                }

                coVerifySequence {
                    internalClient.getMessages(receiverHerId, false)
                }
            }

            "Params should be passed on" {
                val params = RequestParameters(HelseIdTokenParameters(SingleTenantHelseIdTokenParameters(randomHerId().toString())))

                val internalClient = mockk<MshInternalClient> {
                    coEvery { getMessages(any(), any(), any()) } returns List(nextInt(0, 10)) { randomApiMessageWithoutMetadata() }
                }
                val client = Client(internalClient)

                val receiverHerId = randomHerId()
                client.getMessages(receiverHerId, params)
                coVerifySequence {
                    internalClient.getMessages(receiverHerId, false, params)
                }
            }
        }

        "Get messages with metadata" - {
            "Messages should be retrieved and mapped" {
                val apiMessages = List(nextInt(0, 10)) { randomApiMessageWithMetadata() }

                val internalClient = mockk<MshInternalClient> {
                    coEvery { getMessages(any(), any(), any()) } returns apiMessages
                }
                val client = Client(internalClient)

                val receiverHerId = randomHerId()
                client.getMessagesWithMetadata(receiverHerId).asClue {
                    it.forEachIndexed { i, message ->
                        message.id shouldBe apiMessages[i].id
                        message.contentType shouldBe apiMessages[i].contentType
                        message.receiverHerId shouldBe apiMessages[i].receiverHerId
                        message.senderHerId shouldBe apiMessages[i].senderHerId
                        message.businessDocumentId shouldBe apiMessages[i].businessDocumentId
                        message.businessDocumentDate shouldBe apiMessages[i].businessDocumentGenDate
                        message.isAppRec shouldBe apiMessages[i].isAppRec
                    }
                }

                coVerifySequence {
                    internalClient.getMessages(receiverHerId, true)
                }
            }

            "Params should be passed on" {
                val params = RequestParameters(HelseIdTokenParameters(SingleTenantHelseIdTokenParameters(randomHerId().toString())))

                val internalClient = mockk<MshInternalClient> {
                    coEvery { getMessages(any(), any(), any()) } returns List(nextInt(0, 10)) { randomApiMessageWithMetadata() }
                }
                val client = Client(internalClient)

                val receiverHerId = randomHerId()
                client.getMessagesWithMetadata(receiverHerId, params)
                coVerifySequence {
                    internalClient.getMessages(receiverHerId, true, params)
                }
            }
        }

        "Get message" - {
            "Should be retrieved and mapped" {
                val message = randomApiMessageWithMetadata()

                val internalClient = mockk<MshInternalClient> {
                    coEvery { getMessage(any(), any()) } returns message
                }
                val client = Client(internalClient)

                val messageId = UUID.randomUUID()
                client.getMessage(messageId).asClue {
                    it.id shouldBe message.id
                    it.contentType shouldBe message.contentType
                    it.receiverHerId shouldBe message.receiverHerId
                    it.senderHerId shouldBe message.senderHerId
                    it.businessDocumentId shouldBe message.businessDocumentId
                    it.businessDocumentDate shouldBe message.businessDocumentGenDate
                    it.isAppRec shouldBe message.isAppRec
                }

                coVerifySequence {
                    internalClient.getMessage(messageId)
                }
            }

            "Params should be passed on" {
                val params = RequestParameters(HelseIdTokenParameters(SingleTenantHelseIdTokenParameters(randomHerId().toString())))

                val internalClient = mockk<MshInternalClient> {
                    coEvery { getMessage(any(), any()) } returns randomApiMessageWithMetadata()
                }
                val client = Client(internalClient)

                client.getMessage(UUID.randomUUID(), params)
                coVerifySequence {
                    internalClient.getMessage(any(), params)
                }
            }
        }

        "Get business document" - {
            "Only Base64 encoding is supported" {
                val response = GetBusinessDocumentResponse().apply {
                    businessDocument = UUID.randomUUID().toString()
                    contentType = UUID.randomUUID().toString()
                    contentTransferEncoding = UUID.randomUUID().toString()
                }

                val internalClient = mockk<MshInternalClient> {
                    coEvery { getBusinessDocument(any(), any()) } returns response
                }
                val client = Client(internalClient)

                val id = UUID.randomUUID()
                shouldThrow<IllegalArgumentException> { client.getBusinessDocument(id) }.asClue {
                    it.message shouldBe "'${response.contentTransferEncoding}' is not a supported transfer encoding"
                }
            }

            "Only XML is supported" {
                val response = GetBusinessDocumentResponse().apply {
                    businessDocument = UUID.randomUUID().toString()
                    contentType = UUID.randomUUID().toString()
                    contentTransferEncoding = "base64"
                }

                val internalClient = mockk<MshInternalClient> {
                    coEvery { getBusinessDocument(any(), any()) } returns response
                }
                val client = Client(internalClient)

                val id = UUID.randomUUID()
                shouldThrow<IllegalArgumentException> { client.getBusinessDocument(id) }.asClue {
                    it.message shouldBe "'${response.contentType}' is not a supported content type"
                }
            }

            "When encoding and type is correct, the document should be retrieved, deserialized and mapped" {
                val response = GetBusinessDocumentResponse().apply {
                    businessDocument = Base64.getEncoder().encodeToString(readResourceContent("dialogmelding/1.0/foresporsel-og-svar/dialog-foresporsel-samsvar-test.xml"))
                    contentType = "application/xml"
                    contentTransferEncoding = "base64"
                }

                val internalClient = mockk<MshInternalClient> {
                    coEvery { getBusinessDocument(any(), any()) } returns response
                }
                val client = Client(internalClient)

                val id = UUID.randomUUID()
                client.getBusinessDocument(id).asClue {
                    it.id shouldBe "6ddb98ed-9e34-4efa-9163-62e4ea0cbf43"
                }

                coVerifySequence {
                    internalClient.getBusinessDocument(id)
                }
            }

            "If the returned message is of wrong type, an exception should be thrown" {
                val response = GetBusinessDocumentResponse().apply {
                    businessDocument = Base64.getEncoder().encodeToString(readResourceContent("app-rec/1.0/all-data.xml"))
                    contentType = "application/xml"
                    contentTransferEncoding = "base64"
                }

                val internalClient = mockk<MshInternalClient> {
                    coEvery { getBusinessDocument(any(), any()) } returns response
                }
                val client = Client(internalClient)

                val id = UUID.randomUUID()
                shouldThrow<IllegalArgumentException> { client.getBusinessDocument(id) }.asClue {
                    it.message shouldBe "Expected MsgHead as root element, but found AppRec"
                }

                coVerifySequence {
                    internalClient.getBusinessDocument(id)
                }
            }

            "Params should be passed on" {
                val params = RequestParameters(HelseIdTokenParameters(SingleTenantHelseIdTokenParameters(randomHerId().toString())))

                val internalClient = mockk<MshInternalClient> {
                    coEvery { getBusinessDocument(any(), any()) } returns GetBusinessDocumentResponse().apply {
                        businessDocument = Base64.getEncoder().encodeToString(readResourceContent("dialogmelding/1.0/foresporsel-og-svar/dialog-foresporsel-samsvar-test.xml"))
                        contentType = "application/xml"
                        contentTransferEncoding = "base64"
                    }
                }
                val client = Client(internalClient)

                client.getBusinessDocument(UUID.randomUUID(), params)
                coVerifySequence {
                    internalClient.getBusinessDocument(any(), params)
                }
            }
        }

        "Get application receipt" - {
            "Only Base64 encoding is supported" {
                val response = GetBusinessDocumentResponse().apply {
                    businessDocument = UUID.randomUUID().toString()
                    contentType = UUID.randomUUID().toString()
                    contentTransferEncoding = UUID.randomUUID().toString()
                }

                val internalClient = mockk<MshInternalClient> {
                    coEvery { getBusinessDocument(any(), any()) } returns response
                }
                val client = Client(internalClient)

                val id = UUID.randomUUID()
                shouldThrow<IllegalArgumentException> { client.getApplicationReceipt(id) }.asClue {
                    it.message shouldBe "'${response.contentTransferEncoding}' is not a supported transfer encoding"
                }
            }

            "Only XML is supported" {
                val response = GetBusinessDocumentResponse().apply {
                    businessDocument = UUID.randomUUID().toString()
                    contentType = UUID.randomUUID().toString()
                    contentTransferEncoding = "base64"
                }

                val internalClient = mockk<MshInternalClient> {
                    coEvery { getBusinessDocument(any(), any()) } returns response
                }
                val client = Client(internalClient)

                val id = UUID.randomUUID()
                shouldThrow<IllegalArgumentException> { client.getApplicationReceipt(id) }.asClue {
                    it.message shouldBe "'${response.contentType}' is not a supported content type"
                }
            }

            "When encoding and type is correct, the document should be retrieved, deserialized and mapped" {
                val response = GetBusinessDocumentResponse().apply {
                    businessDocument = Base64.getEncoder().encodeToString(readResourceContent("app-rec/1.0/all-data.xml"))
                    contentType = "application/xml"
                    contentTransferEncoding = "base64"
                }

                val internalClient = mockk<MshInternalClient> {
                    coEvery { getBusinessDocument(any(), any()) } returns response
                }
                val client = Client(internalClient)

                val id = UUID.randomUUID()
                client.getApplicationReceipt(id).asClue {
                    it.id shouldBe "a761e9b9-3495-4f5a-a964-c13d544d2ceb"
                }

                coVerifySequence {
                    internalClient.getBusinessDocument(id)
                }
            }

            "If the returned message is of wrong type, an exception should be thrown" {
                val response = GetBusinessDocumentResponse().apply {
                    businessDocument = Base64.getEncoder().encodeToString(readResourceContent("dialogmelding/1.0/foresporsel-og-svar/dialog-foresporsel-samsvar-test.xml"))
                    contentType = "application/xml"
                    contentTransferEncoding = "base64"
                }
                val internalClient = mockk<MshInternalClient> {
                    coEvery { getBusinessDocument(any(), any()) } returns response
                }
                val client = Client(internalClient)

                val id = UUID.randomUUID()
                shouldThrow<IllegalArgumentException> { client.getApplicationReceipt(id) }.asClue {
                    it.message shouldBe "Expected AppRec as root element, but found MsgHead"
                }

                coVerifySequence {
                    internalClient.getBusinessDocument(id)
                }
            }

            "Params should be passed on" {
                val params = RequestParameters(HelseIdTokenParameters(SingleTenantHelseIdTokenParameters(randomHerId().toString())))

                val internalClient = mockk<MshInternalClient> {
                    coEvery { getBusinessDocument(any(), any()) } returns GetBusinessDocumentResponse().apply {
                        businessDocument = Base64.getEncoder().encodeToString(readResourceContent("app-rec/1.0/all-data.xml"))
                        contentType = "application/xml"
                        contentTransferEncoding = "base64"
                    }
                }
                val client = Client(internalClient)

                client.getApplicationReceipt(UUID.randomUUID(), params)
                coVerifySequence {
                    internalClient.getBusinessDocument(any(), params)
                }
            }
        }

        "Send application receipt" - {
            "Should be able to send OK receipt without errors" {
                val returnedId = UUID.randomUUID()
                val requestSlot = slot<PostAppRecRequest>()
                val internalClient = mockk<MshInternalClient> {
                    coEvery { postAppRec(any(), any(), capture(requestSlot), any()) } returns returnedId
                }
                val client = Client(internalClient)

                val receipt = OutgoingApplicationReceipt(UUID.randomUUID(), randomHerId(), StatusForMottakAvMelding.OK, emptyList())
                client.sendApplicationReceipt(receipt).asClue {
                    it shouldBe returnedId
                }

                coVerifySequence {
                    internalClient.postAppRec(receipt.acknowledgedId, receipt.senderHerId, any())
                }

                requestSlot.captured.asClue { request ->
                    request.appRecStatus shouldBe AppRecStatus.OK
                    request.appRecErrorList.shouldBeEmpty()
                    request.ebXmlOverrides should beNull()
                }
            }

            "Should be able to send OK_FEIL_I_DELMELDING with errors" {
                val requestSlot = slot<PostAppRecRequest>()
                val internalClient = mockk<MshInternalClient> { coEvery { postAppRec(any(), any(), capture(requestSlot), any()) } returns UUID.randomUUID() }
                val client = Client(internalClient)

                val receipt = OutgoingApplicationReceipt(
                    acknowledgedId = UUID.randomUUID(),
                    senderHerId = randomHerId(),
                    status = StatusForMottakAvMelding.OK_FEIL_I_DELMELDING,
                    errors = List(nextInt(1, 5)) { randomApplicationReceiptError() },
                )
                client.sendApplicationReceipt(receipt)

                coVerifySequence {
                    internalClient.postAppRec(receipt.acknowledgedId, receipt.senderHerId, any())
                }

                requestSlot.captured.asClue { request ->
                    request.appRecStatus shouldBe AppRecStatus.OK_ERROR_IN_MESSAGE_PART
                    request.appRecErrorList shouldHaveSize receipt.errors!!.size
                    receipt.errors.forEach {
                        request.appRecErrorList shouldContain AppRecError().apply {
                            errorCode = it.type.verdi
                            details = it.details
                        }
                    }
                    request.ebXmlOverrides should beNull()
                }
            }

            "Should be able to send AVVIST with errors" {
                val requestSlot = slot<PostAppRecRequest>()
                val internalClient = mockk<MshInternalClient> { coEvery { postAppRec(any(), any(), capture(requestSlot), any()) } returns UUID.randomUUID() }
                val client = Client(internalClient)

                val receipt = OutgoingApplicationReceipt(
                    acknowledgedId = UUID.randomUUID(),
                    senderHerId = randomHerId(),
                    status = StatusForMottakAvMelding.AVVIST,
                    errors = List(nextInt(1, 5)) { randomApplicationReceiptError() },
                )
                client.sendApplicationReceipt(receipt)

                coVerifySequence {
                    internalClient.postAppRec(receipt.acknowledgedId, receipt.senderHerId, any())
                }

                requestSlot.captured.asClue { request ->
                    request.appRecStatus shouldBe AppRecStatus.REJECTED
                    request.appRecErrorList shouldHaveSize receipt.errors!!.size
                    receipt.errors.forEach {
                        request.appRecErrorList shouldContain AppRecError().apply {
                            errorCode = it.type.verdi
                            details = it.details
                        }
                    }
                    request.ebXmlOverrides should beNull()
                }
            }

            "Trying to send an OK receipt with errors should cause an exception to be thrown" {
                val client = Client(mockk<MshInternalClient> { coEvery { postAppRec(any(), any(), any(), any()) } returns UUID.randomUUID() })

                val receipt = OutgoingApplicationReceipt(
                    acknowledgedId = UUID.randomUUID(),
                    senderHerId = randomHerId(),
                    status = StatusForMottakAvMelding.OK,
                    errors = listOf(randomApplicationReceiptError()),
                )

                shouldThrow<IllegalArgumentException> { client.sendApplicationReceipt(receipt) }.asClue {
                    it.message shouldBe "Error messages are not allowed when status is OK"
                }
            }

            "Trying to send an OK_FEIL_I_DELMELDING receipt without errors should cause an exception to be thrown" {
                val client = Client(mockk<MshInternalClient> { coEvery { postAppRec(any(), any(), any(), any()) } returns UUID.randomUUID() })

                val receipt = OutgoingApplicationReceipt(
                    acknowledgedId = UUID.randomUUID(),
                    senderHerId = randomHerId(),
                    status = StatusForMottakAvMelding.OK_FEIL_I_DELMELDING,
                    errors = emptyList(),
                )

                shouldThrow<IllegalArgumentException> { client.sendApplicationReceipt(receipt) }.asClue {
                    it.message shouldBe "Must provide at least one error message if status is not OK"
                }
            }

            "Trying to send an AVVIST receipt without errors should cause an exception to be thrown" {
                val client = Client(mockk<MshInternalClient> { coEvery { postAppRec(any(), any(), any(), any()) } returns UUID.randomUUID() })

                val receipt = OutgoingApplicationReceipt(
                    acknowledgedId = UUID.randomUUID(),
                    senderHerId = randomHerId(),
                    status = StatusForMottakAvMelding.AVVIST,
                    errors = emptyList(),
                )

                shouldThrow<IllegalArgumentException> { client.sendApplicationReceipt(receipt) }.asClue {
                    it.message shouldBe "Must provide at least one error message if status is not OK"
                }
            }

            "Params should be passed on" {
                val params = RequestParameters(HelseIdTokenParameters(SingleTenantHelseIdTokenParameters(randomHerId().toString())))

                val internalClient = mockk<MshInternalClient> {
                    coEvery { postAppRec(any(), any(), any(), any()) } returns UUID.randomUUID()
                }
                val client = Client(internalClient)

                client.sendApplicationReceipt(
                    OutgoingApplicationReceipt(
                        acknowledgedId = UUID.randomUUID(),
                        senderHerId = randomHerId(),
                        status = StatusForMottakAvMelding.OK,
                        errors = emptyList(),
                    ), params
                )
                coVerifySequence {
                    internalClient.postAppRec(any(), any(), any(), params)
                }
            }
        }

        "Mark message read" - {
            "The data should be serialized to XML and passed on to the service" {
                val internalClient = mockk<MshInternalClient> {
                    coEvery { markMessageRead(any(), any(), any()) } just runs
                }
                val client = Client(internalClient)

                val id = UUID.randomUUID()
                val receiverHerId = randomHerId()
                client.markMessageRead(id, receiverHerId)

                coVerifySequence {
                    internalClient.markMessageRead(id, receiverHerId)
                }
            }

            "Params should be passed on" {
                val params = RequestParameters(HelseIdTokenParameters(SingleTenantHelseIdTokenParameters(randomHerId().toString())))

                val internalClient = mockk<MshInternalClient> {
                    coEvery { markMessageRead(any(), any(), any()) } just runs
                }
                val client = Client(internalClient)

                client.markMessageRead(UUID.randomUUID(), randomHerId(), params)
                coVerifySequence {
                    internalClient.markMessageRead(any(), any(), params)
                }
            }
        }

    }

}

private fun randomOutgoingBusinessDocument(
    vedleggBytes: ByteArray = nextBytes(nextInt(1000, 100000)),
): OutgoingBusinessDocument = OutgoingBusinessDocument(
    id = UUID.randomUUID(),
    sender = Organization(
        name = randomString(),
        ids = listOf(randomOrganizationHerId()),
        childOrganization = ChildOrganization(
            name = randomString(),
            ids = listOf(randomOrganizationHerId()),
        ),
    ),
    receiver = Receiver(
        parent = OrganizationReceiverDetails(
            ids = listOf(randomOrganizationHerId()),
            name = randomString(),
        ),
        child = PersonReceiverDetails(
            ids = listOf(randomPersonHerId()),
            firstName = randomString(),
            middleName = randomString(),
            lastName = randomString(),
        ),
        patient = Patient(
            fnr = randomString(),
            firstName = randomString(),
            middleName = randomString(),
            lastName = randomString(),
        )
    ),
    message = OutgoingMessage(
        subject = randomString(),
        body = randomString(),
        responsibleHealthcareProfessional = HealthcareProfessional(
            firstName = randomString(),
            middleName = randomString(),
            lastName = randomString(),
            phoneNumber = randomString(),
            roleToPatient = HelsepersonellsFunksjoner.entries.random(),
        ),
        recipientContact = RecipientContact(
            type = Helsepersonell.entries.random(),
        ),
    ),
    vedlegg = OutgoingVedlegg(
        date = OffsetDateTime.now(),
        description = randomString(),
        data = ByteArrayInputStream(vedleggBytes),
    ),
    version = DialogmeldingVersion.entries.random(),
)

private fun randomApiMessageWithoutMetadata() = Message().apply {
    id = UUID.randomUUID()
    receiverHerId = randomHerId()
}

private fun randomApiMessageWithMetadata() = Message().apply {
    id = UUID.randomUUID()
    contentType = UUID.randomUUID().toString()
    receiverHerId = randomHerId()
    senderHerId = randomHerId()
    businessDocumentId = UUID.randomUUID().toString()
    businessDocumentGenDate = OffsetDateTime.now()
    isAppRec = nextBoolean()
}

private fun randomApplicationReceiptError() = ApplicationReceiptError(
    type = FeilmeldingForApplikasjonskvittering.entries.minus(FeilmeldingForApplikasjonskvittering.UKJENT).random(),
    details = UUID.randomUUID().toString(),
)
