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
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verifySequence
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
                val apiService = mockk<ApiService> {
                    every { sendMessage(capture(requestSlot)) } answers {
                        RequestContextHolder.get() should beNull()
                        UUID.randomUUID()
                    }
                }
                val client = Client(apiService)

                client.sendMessage(businessDocument)

                verifySequence {
                    apiService.sendMessage(any())
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

            "Params should be added to context before call and removed when it completes" {
                val params = RequestParameters(HelseIdTokenParameters(SingleTenantHelseIdTokenParameters(randomHerId().toString())))

                val apiService = mockk<ApiService> {
                    every { sendMessage(any()) } answers {
                        RequestContextHolder.get() shouldBe params
                        UUID.randomUUID()
                    }
                }
                val client = Client(apiService)

                RequestContextHolder.get() should beNull()
                client.sendMessage(randomOutgoingBusinessDocument(), params)
                RequestContextHolder.get() should beNull()
            }
        }

        "Get messages" - {
            "Messages should be retrieved and mapped" {
                val apiMessages = List(nextInt(0, 10)) { randomApiMessageWithoutMetadata() }

                val apiService = mockk<ApiService> {
                    every { getMessages(any()) } answers {
                        RequestContextHolder.get() should beNull()
                        apiMessages
                    }
                }
                val client = Client(apiService)

                val receiverHerId = randomHerId()
                client.getMessages(receiverHerId).asClue {
                    it.forEachIndexed { i, message ->
                        message.id shouldBe apiMessages[i].id
                        message.receiverHerId shouldBe apiMessages[i].receiverHerId
                    }
                }

                verifySequence {
                    apiService.getMessages(receiverHerId)
                }
            }

            "Params should be added to context before call and removed when it completes" {
                val params = RequestParameters(HelseIdTokenParameters(SingleTenantHelseIdTokenParameters(randomHerId().toString())))

                val apiService = mockk<ApiService> {
                    every { getMessages(any()) } answers {
                        RequestContextHolder.get() shouldBe params
                        List(nextInt(0, 10)) { randomApiMessageWithoutMetadata() }
                    }
                }
                val client = Client(apiService)

                RequestContextHolder.get() should beNull()
                client.getMessages(randomHerId(), params)
                RequestContextHolder.get() should beNull()
            }
        }

        "Get messages with metadata" - {
            "Messages should be retrieved and mapped" {
                val apiMessages = List(nextInt(0, 10)) { randomApiMessageWithMetadata() }

                val apiService = mockk<ApiService> {
                    every { getMessagesWithMetadata(any()) } answers {
                        RequestContextHolder.get() should beNull()
                        apiMessages
                    }
                }
                val client = Client(apiService)

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

                verifySequence {
                    apiService.getMessagesWithMetadata(receiverHerId)
                }
            }

            "Params should be added to context before call and removed when it completes" {
                val params = RequestParameters(HelseIdTokenParameters(SingleTenantHelseIdTokenParameters(randomHerId().toString())))

                val apiService = mockk<ApiService> {
                    every { getMessagesWithMetadata(any()) } answers {
                        RequestContextHolder.get() shouldBe params
                        List(nextInt(0, 10)) { randomApiMessageWithMetadata() }
                    }
                }
                val client = Client(apiService)

                RequestContextHolder.get() should beNull()
                client.getMessagesWithMetadata(randomHerId(), params)
                RequestContextHolder.get() should beNull()
            }
        }

        "Get message" - {
            "Should be retrieved and mapped" {
                val message = randomApiMessageWithMetadata()

                val apiService = mockk<ApiService> {
                    every { getMessage(any()) } answers {
                        RequestContextHolder.get() should beNull()
                        message
                    }
                }
                val client = Client(apiService)

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

                verifySequence {
                    apiService.getMessage(messageId)
                }
            }

            "Params should be added to context before call and removed when it completes" {
                val params = RequestParameters(HelseIdTokenParameters(SingleTenantHelseIdTokenParameters(randomHerId().toString())))

                val apiService = mockk<ApiService> {
                    every { getMessage(any()) } answers {
                        RequestContextHolder.get() shouldBe params
                        randomApiMessageWithMetadata()
                    }
                }
                val client = Client(apiService)

                RequestContextHolder.get() should beNull()
                client.getMessage(UUID.randomUUID(), params)
                RequestContextHolder.get() should beNull()
            }
        }

        "Get business document" - {
            "Only Base64 encoding is supported" {
                val response = GetBusinessDocumentResponse().apply {
                    businessDocument = UUID.randomUUID().toString()
                    contentType = UUID.randomUUID().toString()
                    contentTransferEncoding = UUID.randomUUID().toString()
                }

                val apiService = mockk<ApiService> {
                    every { getBusinessDocument(any()) } answers {
                        RequestContextHolder.get() should beNull()
                        response
                    }
                }
                val client = Client(apiService)

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

                val apiService = mockk<ApiService> { every { getBusinessDocument(any()) } returns response }
                val client = Client(apiService)

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

                val apiService = mockk<ApiService> { every { getBusinessDocument(any()) } returns response }
                val client = Client(apiService)

                val id = UUID.randomUUID()
                client.getBusinessDocument(id).asClue {
                    it.id shouldBe "6ddb98ed-9e34-4efa-9163-62e4ea0cbf43"
                }

                verifySequence {
                    apiService.getBusinessDocument(id)
                }
            }

            "If the returned message is of wrong type, an exception should be thrown" {
                val response = GetBusinessDocumentResponse().apply {
                    businessDocument = Base64.getEncoder().encodeToString(readResourceContent("app-rec/1.0/all-data.xml"))
                    contentType = "application/xml"
                    contentTransferEncoding = "base64"
                }

                val apiService = mockk<ApiService> { every { getBusinessDocument(any()) } returns response }
                val client = Client(apiService)

                val id = UUID.randomUUID()
                shouldThrow<IllegalArgumentException> { client.getBusinessDocument(id) }.asClue {
                    it.message shouldBe "Expected MsgHead as root element, but found AppRec"
                }

                verifySequence {
                    apiService.getBusinessDocument(id)
                }
            }

            "Params should be added to context before call and removed when it completes" {
                val params = RequestParameters(HelseIdTokenParameters(SingleTenantHelseIdTokenParameters(randomHerId().toString())))

                val apiService = mockk<ApiService> {
                    every { getBusinessDocument(any()) } answers {
                        RequestContextHolder.get() shouldBe params
                        GetBusinessDocumentResponse().apply {
                            businessDocument = Base64.getEncoder().encodeToString(readResourceContent("dialogmelding/1.0/foresporsel-og-svar/dialog-foresporsel-samsvar-test.xml"))
                            contentType = "application/xml"
                            contentTransferEncoding = "base64"
                        }
                    }
                }
                val client = Client(apiService)

                RequestContextHolder.get() should beNull()
                client.getBusinessDocument(UUID.randomUUID(), params)
                RequestContextHolder.get() should beNull()
            }
        }

        "Get application receipt" - {
            "Only Base64 encoding is supported" {
                val response = GetBusinessDocumentResponse().apply {
                    businessDocument = UUID.randomUUID().toString()
                    contentType = UUID.randomUUID().toString()
                    contentTransferEncoding = UUID.randomUUID().toString()
                }

                val apiService = mockk<ApiService> {
                    every { getBusinessDocument(any()) } answers {
                        RequestContextHolder.get() should beNull()
                        response
                    }
                }
                val client = Client(apiService)

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

                val apiService = mockk<ApiService> { every { getBusinessDocument(any()) } returns response }
                val client = Client(apiService)

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

                val apiService = mockk<ApiService> { every { getBusinessDocument(any()) } returns response }
                val client = Client(apiService)

                val id = UUID.randomUUID()
                client.getApplicationReceipt(id).asClue {
                    it.id shouldBe "a761e9b9-3495-4f5a-a964-c13d544d2ceb"
                }

                verifySequence {
                    apiService.getBusinessDocument(id)
                }
            }

            "If the returned message is of wrong type, an exception should be thrown" {
                val response = GetBusinessDocumentResponse().apply {
                    businessDocument = Base64.getEncoder().encodeToString(readResourceContent("dialogmelding/1.0/foresporsel-og-svar/dialog-foresporsel-samsvar-test.xml"))
                    contentType = "application/xml"
                    contentTransferEncoding = "base64"
                }

                val apiService = mockk<ApiService> { every { getBusinessDocument(any()) } returns response }
                val client = Client(apiService)

                val id = UUID.randomUUID()
                shouldThrow<IllegalArgumentException> { client.getApplicationReceipt(id) }.asClue {
                    it.message shouldBe "Expected AppRec as root element, but found MsgHead"
                }

                verifySequence {
                    apiService.getBusinessDocument(id)
                }
            }

            "Params should be added to context before call and removed when it completes" {
                val params = RequestParameters(HelseIdTokenParameters(SingleTenantHelseIdTokenParameters(randomHerId().toString())))

                val apiService = mockk<ApiService> {
                    every { getBusinessDocument(any()) } answers {
                        RequestContextHolder.get() shouldBe params
                        GetBusinessDocumentResponse().apply {
                            businessDocument = Base64.getEncoder().encodeToString(readResourceContent("app-rec/1.0/all-data.xml"))
                            contentType = "application/xml"
                            contentTransferEncoding = "base64"
                        }
                    }
                }
                val client = Client(apiService)

                RequestContextHolder.get() should beNull()
                client.getApplicationReceipt(UUID.randomUUID(), params)
                RequestContextHolder.get() should beNull()
            }
        }

        "Send application receipt" - {
            "Should be able to send OK receipt without errors" {
                val requestSlot = slot<PostAppRecRequest>()
                val apiService = mockk<ApiService> {
                    every { sendApplicationReceipt(any(), any(), capture(requestSlot)) } answers {
                        RequestContextHolder.get() should beNull()
                        UUID.randomUUID()
                    }
                }
                val client = Client(apiService)

                val receipt = OutgoingApplicationReceipt(UUID.randomUUID(), randomHerId(), StatusForMottakAvMelding.OK, emptyList())
                client.sendApplicationReceipt(receipt)

                verifySequence {
                    apiService.sendApplicationReceipt(receipt.acknowledgedId, receipt.senderHerId, any())
                }

                requestSlot.captured.asClue { request ->
                    request.appRecStatus shouldBe AppRecStatus.OK
                    request.appRecErrorList.shouldBeEmpty()
                    request.ebXmlOverrides should beNull()
                }
            }

            "Should be able to send OK_FEIL_I_DELMELDING with errors" {
                val requestSlot = slot<PostAppRecRequest>()
                val apiService = mockk<ApiService> { every { sendApplicationReceipt(any(), any(), capture(requestSlot)) } returns UUID.randomUUID() }
                val client = Client(apiService)

                val receipt = OutgoingApplicationReceipt(
                    acknowledgedId = UUID.randomUUID(),
                    senderHerId = randomHerId(),
                    status = StatusForMottakAvMelding.OK_FEIL_I_DELMELDING,
                    errors = List(nextInt(1, 5)) { randomApplicationReceiptError() },
                )
                client.sendApplicationReceipt(receipt)

                verifySequence {
                    apiService.sendApplicationReceipt(receipt.acknowledgedId, receipt.senderHerId, any())
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
                val apiService = mockk<ApiService> { every { sendApplicationReceipt(any(), any(), capture(requestSlot)) } returns UUID.randomUUID() }
                val client = Client(apiService)

                val receipt = OutgoingApplicationReceipt(
                    acknowledgedId = UUID.randomUUID(),
                    senderHerId = randomHerId(),
                    status = StatusForMottakAvMelding.AVVIST,
                    errors = List(nextInt(1, 5)) { randomApplicationReceiptError() },
                )
                client.sendApplicationReceipt(receipt)

                verifySequence {
                    apiService.sendApplicationReceipt(receipt.acknowledgedId, receipt.senderHerId, any())
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
                val client = Client(mockk<ApiService> { every { sendApplicationReceipt(any(), any(), any()) } returns UUID.randomUUID() })

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
                val client = Client(mockk<ApiService> { every { sendApplicationReceipt(any(), any(), any()) } returns UUID.randomUUID() })

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
                val client = Client(mockk<ApiService> { every { sendApplicationReceipt(any(), any(), any()) } returns UUID.randomUUID() })

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

            "Params should be added to context before call and removed when it completes" {
                val params = RequestParameters(HelseIdTokenParameters(SingleTenantHelseIdTokenParameters(randomHerId().toString())))

                val apiService = mockk<ApiService> {
                    every { sendApplicationReceipt(any(), any(), any()) } answers {
                        RequestContextHolder.get() shouldBe params
                        UUID.randomUUID()
                    }
                }
                val client = Client(apiService)

                RequestContextHolder.get() should beNull()
                client.sendApplicationReceipt(
                    OutgoingApplicationReceipt(
                        acknowledgedId = UUID.randomUUID(),
                        senderHerId = randomHerId(),
                        status = StatusForMottakAvMelding.OK,
                        errors = emptyList(),
                    ), params
                )
                RequestContextHolder.get() should beNull()
            }
        }

        "Mark message read" - {
            "The data should be serialized to XML and passed on to the service" {
                val apiService = mockk<ApiService> {
                    every { markMessageRead(any(), any()) } answers {
                        RequestContextHolder.get() should beNull()
                    }
                }
                val client = Client(apiService)

                val id = UUID.randomUUID()
                val receiverHerId = randomHerId()
                client.markMessageRead(id, receiverHerId)

                verifySequence {
                    apiService.markMessageRead(id, receiverHerId)
                }
            }

            "Params should be added to context before call and removed when it completes" {
                val params = RequestParameters(HelseIdTokenParameters(SingleTenantHelseIdTokenParameters(randomHerId().toString())))

                val apiService = mockk<ApiService> {
                    every { markMessageRead(any(), any()) } answers {
                        RequestContextHolder.get() shouldBe params
                        UUID.randomUUID()
                    }
                }
                val client = Client(apiService)

                RequestContextHolder.get() should beNull()
                client.markMessageRead(UUID.randomUUID(), randomHerId(), params)
                RequestContextHolder.get() should beNull()
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
