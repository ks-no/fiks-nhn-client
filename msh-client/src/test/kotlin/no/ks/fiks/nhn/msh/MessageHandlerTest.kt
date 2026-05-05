package no.ks.fiks.nhn.msh

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifySequence
import io.mockk.mockk
import no.ks.fiks.nhn.*
import no.nhn.msh.v2.model.GetBusinessDocumentResponse
import java.util.*

class MessageHandlerTest : FreeSpec() {

    init {

        "getBusinessDocument" - {

            "onIncomingBusinessDocumentReceived is called on successful deserialization" {
                val handler = mockk<MessageHandler>(relaxed = true)
                val id = UUID.randomUUID()
                val client = Client(mockk {
                    coEvery { getBusinessDocument(any(), any()) } returns validMsgHeadResponse()
                }, listOf(handler))

                client.getBusinessDocument(id)

                coVerify {
                    handler.onIncomingBusinessDocumentReceived(id, any(), any())
                }
                coVerify(exactly = 0) {
                    handler.onIncomingBusinessDocumentDeserializationFailed(any(), any(), any())
                    handler.onIncomingBusinessDocumentDecodingFailed(any(), any(), any())
                }
            }

            "onIncomingBusinessDocumentDeserializationFailed is called when deserialization fails" {
                val handler = mockk<MessageHandler>(relaxed = true)
                val id = UUID.randomUUID()
                val client = Client(mockk {
                    coEvery { getBusinessDocument(any(), any()) } returns validAppRecResponse()
                }, listOf(handler))

                shouldThrow<IllegalArgumentException> { client.getBusinessDocument(id) }

                coVerify {
                    handler.onIncomingBusinessDocumentDeserializationFailed(id, any(), any())
                }
                coVerify(exactly = 0) {
                    handler.onIncomingBusinessDocumentReceived(any(), any(), any())
                    handler.onIncomingBusinessDocumentDecodingFailed(any(), any(), any())
                }
            }

            "onIncomingBusinessDocumentDecodingFailed is called when Base64 decoding fails" {
                val handler = mockk<MessageHandler>(relaxed = true)
                val id = UUID.randomUUID()
                val client = Client(mockk {
                    coEvery { getBusinessDocument(any(), any()) } returns invalidBase64Response()
                }, listOf(handler))

                shouldThrow<Exception> { client.getBusinessDocument(id) }

                coVerify {
                    handler.onIncomingBusinessDocumentDecodingFailed(id, any(), any())
                }
                coVerify(exactly = 0) {
                    handler.onIncomingBusinessDocumentReceived(any(), any(), any())
                    handler.onIncomingBusinessDocumentDeserializationFailed(any(), any(), any())
                }
            }

            "rawXml passed to handler matches decoded payload" {
                var capturedRawXml: String? = null
                val handler = object : MessageHandler {
                    override suspend fun onIncomingBusinessDocumentReceived(id: UUID, rawXml: String, document: IncomingBusinessDocument) {
                        capturedRawXml = rawXml
                    }
                }
                val response = validMsgHeadResponse()
                val expectedXml = String(Base64.getDecoder().decode(response.businessDocument))
                val client = Client(mockk {
                    coEvery { getBusinessDocument(any(), any()) } returns response
                }, listOf(handler))

                client.getBusinessDocument(UUID.randomUUID())

                capturedRawXml shouldBe expectedXml
            }

            "all handlers are called even if one throws" {
                val failingHandler = mockk<MessageHandler>(relaxed = true) {
                    coEvery { onIncomingBusinessDocumentReceived(any(), any(), any()) } throws RuntimeException("handler error")
                }
                val secondHandler = mockk<MessageHandler>(relaxed = true)
                val id = UUID.randomUUID()
                val client = Client(mockk {
                    coEvery { getBusinessDocument(any(), any()) } returns validMsgHeadResponse()
                }, listOf(failingHandler, secondHandler))

                client.getBusinessDocument(id)

                coVerifySequence {
                    failingHandler.onIncomingBusinessDocumentReceived(id, any(), any())
                    secondHandler.onIncomingBusinessDocumentReceived(id, any(), any())
                }
            }

            "a throwing handler does not prevent the result from being returned" {
                val handler = mockk<MessageHandler>(relaxed = true) {
                    coEvery { onIncomingBusinessDocumentReceived(any(), any(), any()) } throws RuntimeException("handler error")
                }
                val client = Client(mockk {
                    coEvery { getBusinessDocument(any(), any()) } returns validMsgHeadResponse()
                }, listOf(handler))

                val result = client.getBusinessDocument(UUID.randomUUID())
                result.id shouldBe "6ddb98ed-9e34-4efa-9163-62e4ea0cbf43"
            }

        }

        "getApplicationReceipt" - {

            "onIncomingApplicationReceiptReceived is called on successful deserialization" {
                val handler = mockk<MessageHandler>(relaxed = true)
                val id = UUID.randomUUID()
                val client = Client(mockk {
                    coEvery { getBusinessDocument(any(), any()) } returns validAppRecResponse()
                }, listOf(handler))

                client.getApplicationReceipt(id)

                coVerify {
                    handler.onIncomingApplicationReceiptReceived(id, any(), any())
                }
                coVerify(exactly = 0) {
                    handler.onIncomingApplicationReceiptDeserializationFailed(any(), any(), any())
                    handler.onIncomingApplicationReceiptDecodingFailed(any(), any(), any())
                }
            }

            "onIncomingApplicationReceiptDeserializationFailed is called when deserialization fails" {
                val handler = mockk<MessageHandler>(relaxed = true)
                val id = UUID.randomUUID()
                val client = Client(mockk {
                    coEvery { getBusinessDocument(any(), any()) } returns validMsgHeadResponse()
                }, listOf(handler))

                shouldThrow<IllegalArgumentException> { client.getApplicationReceipt(id) }

                coVerify {
                    handler.onIncomingApplicationReceiptDeserializationFailed(id, any(), any())
                }
                coVerify(exactly = 0) {
                    handler.onIncomingApplicationReceiptReceived(any(), any(), any())
                    handler.onIncomingApplicationReceiptDecodingFailed(any(), any(), any())
                }
            }

            "onIncomingApplicationReceiptDecodingFailed is called when Base64 decoding fails" {
                val handler = mockk<MessageHandler>(relaxed = true)
                val id = UUID.randomUUID()
                val client = Client(mockk {
                    coEvery { getBusinessDocument(any(), any()) } returns invalidBase64Response()
                }, listOf(handler))

                shouldThrow<Exception> { client.getApplicationReceipt(id) }

                coVerify {
                    handler.onIncomingApplicationReceiptDecodingFailed(id, any(), any())
                }
                coVerify(exactly = 0) {
                    handler.onIncomingApplicationReceiptReceived(any(), any(), any())
                    handler.onIncomingApplicationReceiptDeserializationFailed(any(), any(), any())
                }
            }

            "rawBusinessDocument passed to decoding-failed handler matches original payload" {
                var capturedRawBusinessDocument: String? = null
                val handler = object : MessageHandler {
                    override suspend fun onIncomingApplicationReceiptDecodingFailed(id: UUID, rawBusinessDocument: String, exception: Throwable) {
                        capturedRawBusinessDocument = rawBusinessDocument
                    }
                }
                val response = invalidBase64Response()
                val client = Client(mockk {
                    coEvery { getBusinessDocument(any(), any()) } returns response
                }, listOf(handler))

                shouldThrow<Exception> { client.getApplicationReceipt(UUID.randomUUID()) }

                capturedRawBusinessDocument shouldBe response.businessDocument
            }

            "rawXml passed to handler matches decoded payload" {
                var capturedRawXml: String? = null
                val handler = object : MessageHandler {
                    override suspend fun onIncomingApplicationReceiptReceived(id: UUID, rawXml: String, receipt: IncomingApplicationReceipt) {
                        capturedRawXml = rawXml
                    }
                }
                val response = validAppRecResponse()
                val expectedXml = String(Base64.getDecoder().decode(response.businessDocument))
                val client = Client(mockk {
                    coEvery { getBusinessDocument(any(), any()) } returns response
                }, listOf(handler))

                client.getApplicationReceipt(UUID.randomUUID())

                capturedRawXml shouldBe expectedXml
            }

            "all handlers are called even if one throws" {
                val failingHandler = mockk<MessageHandler>(relaxed = true) {
                    coEvery { onIncomingApplicationReceiptReceived(any(), any(), any()) } throws RuntimeException("handler error")
                }
                val secondHandler = mockk<MessageHandler>(relaxed = true)
                val id = UUID.randomUUID()
                val client = Client(mockk {
                    coEvery { getBusinessDocument(any(), any()) } returns validAppRecResponse()
                }, listOf(failingHandler, secondHandler))

                client.getApplicationReceipt(id)

                coVerifySequence {
                    failingHandler.onIncomingApplicationReceiptReceived(id, any(), any())
                    secondHandler.onIncomingApplicationReceiptReceived(id, any(), any())
                }
            }

            "a throwing handler does not prevent the result from being returned" {
                val handler = mockk<MessageHandler>(relaxed = true) {
                    coEvery { onIncomingApplicationReceiptReceived(any(), any(), any()) } throws RuntimeException("handler error")
                }
                val client = Client(mockk {
                    coEvery { getBusinessDocument(any(), any()) } returns validAppRecResponse()
                }, listOf(handler))

                val result = client.getApplicationReceipt(UUID.randomUUID())
                result.id shouldBe "a761e9b9-3495-4f5a-a964-c13d544d2ceb"
            }

        }

    }

}

private fun validMsgHeadResponse() = GetBusinessDocumentResponse().apply {
    businessDocument = Base64.getEncoder().encodeToString(readResourceContent("dialogmelding/1.0/foresporsel-og-svar/dialog-foresporsel-samsvar-test.xml"))
    contentType = "application/xml"
    contentTransferEncoding = "base64"
}

private fun validAppRecResponse() = GetBusinessDocumentResponse().apply {
    businessDocument = Base64.getEncoder().encodeToString(readResourceContent("app-rec/1.0/all-data.xml"))
    contentType = "application/xml"
    contentTransferEncoding = "base64"
}

private fun invalidBase64Response() = GetBusinessDocumentResponse().apply {
    businessDocument = "this is not valid base64!!!"
    contentType = "application/xml"
    contentTransferEncoding = "base64"
}


