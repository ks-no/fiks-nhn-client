package no.ks.fiks.nhn.msh

import io.kotest.assertions.asClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.*
import no.nhn.msh.v2.api.MessagesControllerApi
import no.nhn.msh.v2.model.*
import no.nhn.msh.v2.model.Message
import java.util.*
import kotlin.random.Random.Default.nextInt

class ApiServiceTest : StringSpec({

    "Test sending message" {
        val response = mockk<UUID>()
        val api = mockk<MessagesControllerApi> { every { postMessage(any(), any(), any()) } returns response }

        val sourceSystem = UUID.randomUUID().toString()
        val request = PostMessageRequest().businessDocument(sourceSystem)
        ApiService(api, sourceSystem)
            .sendMessage(request)
            .asClue { it shouldBeSameInstanceAs response }

        verify(exactly = 1) {
            api.postMessage("2", sourceSystem, request)
        }
    }

    "Test getting messages" {
        val response = mockk<List<Message>>()
        val api = mockk<MessagesControllerApi> { every { getMessages(any(), any(), any()) } returns response }

        val sourceSystem = UUID.randomUUID().toString()
        val receiverHerId = nextInt(0, 1000000)
        ApiService(api, sourceSystem)
            .getMessages(receiverHerId)
            .asClue { it shouldBeSameInstanceAs response }

        verify(exactly = 1) {
            api.getMessages("2", sourceSystem, MessagesControllerApi.GetMessagesQueryParams().receiverHerIds(setOf(receiverHerId)))
        }
    }

    "Test getting messages with metadata" {
        val response = mockk<List<Message>>()
        val api = mockk<MessagesControllerApi> { every { getMessages(any(), any(), any()) } returns response }

        val sourceSystem = UUID.randomUUID().toString()
        val receiverHerId = nextInt(0, 1000000)
        ApiService(api, sourceSystem)
            .getMessagesWithMetadata(receiverHerId)
            .asClue { it shouldBeSameInstanceAs response }

        verify(exactly = 1) {
            api.getMessages("2", sourceSystem, MessagesControllerApi.GetMessagesQueryParams().receiverHerIds(setOf(receiverHerId)).includeMetadata(true))
        }
    }

    "Test getting message" {
        val response = mockk<Message>()
        val api = mockk<MessagesControllerApi> { every { getMessage(any(), any(), any()) } returns response }

        val messageId = UUID.randomUUID()
        val sourceSystem = UUID.randomUUID().toString()
        ApiService(api, sourceSystem)
            .getMessage(messageId)
            .asClue { it shouldBeSameInstanceAs response }

        verify(exactly = 1) {
            api.getMessage(messageId, "2", sourceSystem)
        }
    }

    "Test getting business document" {
        val response = mockk<GetBusinessDocumentResponse>()
        val api = mockk<MessagesControllerApi> { every { getBusinessDocument(any(), any(), any()) } returns response }

        val messageId = UUID.randomUUID()
        val sourceSystem = UUID.randomUUID().toString()
        ApiService(api, sourceSystem)
            .getBusinessDocument(messageId)
            .asClue { it shouldBeSameInstanceAs response }

        verify(exactly = 1) {
            api.getBusinessDocument(messageId, "2", sourceSystem)
        }
    }

    "Test sending application receipt" {
        val response = mockk<UUID>()
        val api = mockk<MessagesControllerApi> { every { postAppRec(any(), any(), any(), any(), any()) } returns response }

        val appRecId = UUID.randomUUID()
        val senderHerId = nextInt(0, 1000000)
        val sourceSystem = UUID.randomUUID().toString()
        val request = PostAppRecRequest().appRecStatus(AppRecStatus.entries.random())
        ApiService(api, sourceSystem)
            .sendApplicationReceipt(appRecId, senderHerId, request)
            .asClue { it shouldBeSameInstanceAs response }

        verify(exactly = 1) {
            api.postAppRec(appRecId, senderHerId, "2", sourceSystem, request)
        }
    }

    "Test marking message as read" {
        val api = mockk<MessagesControllerApi> { every { markMessageAsRead(any(), any(), any(), any()) } just runs }

        val appRecId = UUID.randomUUID()
        val receiverHerId = nextInt(0, 1000000)
        val sourceSystem = UUID.randomUUID().toString()
        ApiService(api, sourceSystem)
            .markMessageRead(appRecId, receiverHerId)

        verify(exactly = 1) {
            api.markMessageAsRead(appRecId, receiverHerId, "2", sourceSystem)
        }
    }

})
