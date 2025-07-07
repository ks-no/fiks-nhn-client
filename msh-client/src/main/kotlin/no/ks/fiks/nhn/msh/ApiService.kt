package no.ks.fiks.nhn.msh

import no.nhn.msh.v2.api.MessagesControllerApi
import no.nhn.msh.v2.model.GetBusinessDocumentResponse
import no.nhn.msh.v2.model.Message
import no.nhn.msh.v2.model.PostAppRecRequest
import no.nhn.msh.v2.model.PostMessageRequest
import java.util.*

private const val API_VERSION = "2"

class ApiService(
    private val api: MessagesControllerApi,
    private val sourceSystem: String,
) {

    fun sendMessage(request: PostMessageRequest): UUID = api.postMessage(API_VERSION, sourceSystem, request)

    fun getMessages(receiverHerId: Int): List<Message> =
        api.getMessages(
            API_VERSION, sourceSystem, MessagesControllerApi.GetMessagesQueryParams()
                .receiverHerIds(setOf(receiverHerId))
        )

    fun getMessagesWithMetadata(receiverHerId: Int): List<Message> =
        api.getMessages(
            API_VERSION, sourceSystem, MessagesControllerApi.GetMessagesQueryParams()
                .receiverHerIds(setOf(receiverHerId))
                .includeMetadata(true)
        )

    fun getMessage(id: UUID): Message = api.getMessage(id, API_VERSION, sourceSystem)

    fun getBusinessDocument(id: UUID): GetBusinessDocumentResponse = api.getBusinessDocument(id, API_VERSION, sourceSystem)

    fun sendApplicationReceipt(acknowledgedId: UUID, senderHerId: Int, request: PostAppRecRequest): UUID = api.postAppRec(acknowledgedId, senderHerId, API_VERSION, sourceSystem, request)

    fun markMessageRead(id: UUID, receiverHerId: Int) {
        api.markMessageAsRead(id, receiverHerId, API_VERSION, sourceSystem)
    }

}