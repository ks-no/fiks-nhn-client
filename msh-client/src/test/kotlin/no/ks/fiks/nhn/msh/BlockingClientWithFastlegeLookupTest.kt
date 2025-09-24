package no.ks.fiks.nhn.msh

import io.kotest.assertions.asClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verifySequence
import java.util.*
import kotlin.random.Random.Default.nextInt

class BlockingClientWithFastlegeLookupTest : StringSpec() {

    init {
        "Send message to GP for person should proxy to internal client" {
            val id = UUID.randomUUID()
            val asyncClient = mockk<ClientWithFastlegeLookup> { coEvery { sendMessageToGPForPerson(any(), any()) } returns id }
            val blockingClient = BlockingClientWithFastlegeLookup(asyncClient)

            val businessDocument = mockk<GPForPersonOutgoingBusinessDocument>()
            val requestParameters = RequestParameters()
            blockingClient.sendMessageToGPForPerson(businessDocument, requestParameters).asClue {
                it shouldBe id
            }

            verifySequence { blockingClient.sendMessageToGPForPerson(businessDocument, requestParameters) }
        }
        "Send message should proxy to internal client" {
            val id = UUID.randomUUID()
            val asyncClient = mockk<ClientWithFastlegeLookup> { coEvery { sendMessage(any(), any()) } returns id }
            val blockingClient = BlockingClientWithFastlegeLookup(asyncClient)

            val businessDocument = mockk<OutgoingBusinessDocument>()
            val requestParameters = RequestParameters()
            blockingClient.sendMessage(businessDocument, requestParameters).asClue {
                it shouldBe id
            }

            verifySequence { blockingClient.sendMessage(businessDocument, requestParameters) }
        }

        "Get messages should proxy to internal client" {
            val messages = listOf<Message>(mockk(), mockk())
            val asyncClient = mockk<ClientWithFastlegeLookup> { coEvery { getMessages(any(), any()) } returns messages }
            val blockingClient = BlockingClientWithFastlegeLookup(asyncClient)

            val receiverHerId = nextInt(1, 100000)
            val requestParameters = RequestParameters()
            blockingClient.getMessages(receiverHerId, requestParameters).asClue {
                it shouldBe messages
            }

            verifySequence { blockingClient.getMessages(receiverHerId, requestParameters) }
        }

        "Get messages with metadata should proxy to internal client" {
            val messages = listOf<MessageWithMetadata>(mockk(), mockk())
            val asyncClient = mockk<ClientWithFastlegeLookup> { coEvery { getMessagesWithMetadata(any(), any()) } returns messages }
            val blockingClient = BlockingClientWithFastlegeLookup(asyncClient)

            val receiverHerId = nextInt(1, 100000)
            val requestParameters = RequestParameters()
            blockingClient.getMessagesWithMetadata(receiverHerId, requestParameters).asClue {
                it shouldBe messages
            }

            verifySequence { blockingClient.getMessagesWithMetadata(receiverHerId, requestParameters) }
        }

        "Get message should proxy to internal client" {
            val id = UUID.randomUUID()
            val message = mockk<MessageWithMetadata>()
            val asyncClient = mockk<ClientWithFastlegeLookup> { coEvery { getMessage(any(), any()) } returns message }
            val blockingClient = BlockingClientWithFastlegeLookup(asyncClient)

            val requestParameters = RequestParameters()
            blockingClient.getMessage(id, requestParameters).asClue {
                it shouldBe message
            }

            verifySequence { blockingClient.getMessage(id, requestParameters) }
        }

        "Get business document should proxy to internal client" {
            val id = UUID.randomUUID()
            val businessDocument = mockk<IncomingBusinessDocument>()
            val asyncClient = mockk<ClientWithFastlegeLookup> { coEvery { getBusinessDocument(any(), any()) } returns businessDocument }
            val blockingClient = BlockingClientWithFastlegeLookup(asyncClient)

            val requestParameters = RequestParameters()
            blockingClient.getBusinessDocument(id, requestParameters).asClue {
                it shouldBe businessDocument
            }

            verifySequence { blockingClient.getBusinessDocument(id, requestParameters) }
        }

        "Get application receipt should proxy to internal client" {
            val id = UUID.randomUUID()
            val appRec = mockk<IncomingApplicationReceipt>()
            val asyncClient = mockk<ClientWithFastlegeLookup> { coEvery { getApplicationReceipt(any(), any()) } returns appRec }
            val blockingClient = BlockingClientWithFastlegeLookup(asyncClient)

            val requestParameters = RequestParameters()
            blockingClient.getApplicationReceipt(id, requestParameters).asClue {
                it shouldBe appRec
            }

            verifySequence { blockingClient.getApplicationReceipt(id, requestParameters) }
        }

        "Get application receipts for message should proxy to internal client" {
            val id = UUID.randomUUID()
            val appRecs = listOf<ApplicationReceiptInfo>(mockk(), mockk())
            val asyncClient = mockk<ClientWithFastlegeLookup> { coEvery { getApplicationReceiptsForMessage(any(), any()) } returns appRecs }
            val blockingClient = BlockingClientWithFastlegeLookup(asyncClient)

            val requestParameters = RequestParameters()
            blockingClient.getApplicationReceiptsForMessage(id, requestParameters).asClue {
                it shouldBe appRecs
            }

            verifySequence { blockingClient.getApplicationReceiptsForMessage(id, requestParameters) }
        }

        "Send application receipt should proxy to internal client" {
            val id = UUID.randomUUID()
            val asyncClient = mockk<ClientWithFastlegeLookup> { coEvery { sendApplicationReceipt(any(), any()) } returns id }
            val blockingClient = BlockingClientWithFastlegeLookup(asyncClient)

            val appRec = mockk<OutgoingApplicationReceipt>()
            val requestParameters = RequestParameters()
            blockingClient.sendApplicationReceipt(appRec, requestParameters).asClue {
                it shouldBe id
            }

            verifySequence { blockingClient.sendApplicationReceipt(appRec, requestParameters) }
        }

        "Mark message read should proxy to internal client" {
            val id = UUID.randomUUID()
            val asyncClient = mockk<ClientWithFastlegeLookup> { coEvery { markMessageRead(any(), any(), any()) } just runs }
            val blockingClient = BlockingClientWithFastlegeLookup(asyncClient)

            val receiverHerId = nextInt(1, 100000)
            val requestParameters = RequestParameters()
            blockingClient.markMessageRead(id, receiverHerId, requestParameters)

            verifySequence { blockingClient.markMessageRead(id, receiverHerId, requestParameters) }
        }

        "Get status should proxy to internal client" {
            val id = UUID.randomUUID()
            val statuses = listOf<StatusInfo>(mockk(), mockk(), mockk())
            val asyncClient = mockk<ClientWithFastlegeLookup> { coEvery { getStatus(any(), any()) } returns statuses }
            val blockingClient = BlockingClientWithFastlegeLookup(asyncClient)

            val requestParameters = RequestParameters()
            blockingClient.getStatus(id, requestParameters).asClue {
                it shouldBe statuses
            }

            verifySequence { blockingClient.getStatus(id, requestParameters) }
        }
    }

}

