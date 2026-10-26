package no.ks.fiks.nhn.msh

import kotlinx.coroutines.runBlocking
import java.util.*

class BlockingClient(
    private val client: Client,
) {

    @JvmOverloads
    fun sendMessage(
        businessDocument: OutgoingBusinessDocument,
        requestParameters: RequestParameters? = null,
    ): UUID = runBlocking { client.sendMessage(businessDocument, requestParameters) }

    @JvmOverloads
    fun getMessages(
        receiverHerId: Int,
        requestParameters: RequestParameters? = null,
    ): List<Message> = runBlocking { client.getMessages(receiverHerId, requestParameters) }

    @JvmOverloads
    fun getMessagesWithMetadata(
        receiverHerId: Int,
        requestParameters: RequestParameters? = null,
    ): List<MessageWithMetadata> = runBlocking { client.getMessagesWithMetadata(receiverHerId, requestParameters) }

    @JvmOverloads
    fun getMessage(
        id: UUID,
        requestParameters: RequestParameters? = null,
    ): MessageWithMetadata = runBlocking { client.getMessage(id, requestParameters) }

    @JvmOverloads
    fun getBusinessDocument(
        id: UUID,
        requestParameters: RequestParameters? = null,
    ): IncomingBusinessDocument = runBlocking { client.getBusinessDocument(id, requestParameters) }

    @JvmOverloads
    fun getApplicationReceipt(
        id: UUID,
        requestParameters: RequestParameters? = null,
    ): IncomingApplicationReceipt = runBlocking { client.getApplicationReceipt(id, requestParameters) }

    @JvmOverloads
    fun getApplicationReceiptsForMessage(
        messageId: UUID,
        requestParameters: RequestParameters? = null,
    ): List<ApplicationReceiptInfo> = runBlocking { client.getApplicationReceiptsForMessage(messageId, requestParameters) }

    @JvmOverloads
    fun sendApplicationReceipt(
        receipt: OutgoingApplicationReceipt,
        requestParameters: RequestParameters? = null,
    ) : UUID = runBlocking { client.sendApplicationReceipt(receipt, requestParameters) }

    @JvmOverloads
    fun getUnreadNotifications(
        herIds: Collection<Int>,
        notificationsToFetch: Int? = null,
        requestParameters: RequestParameters? = null,
    ): List<UnreadNotification> = runBlocking { client.getUnreadNotifications(herIds, notificationsToFetch, requestParameters) }

    @JvmOverloads
    fun deleteNotifications(
        notificationIds: Collection<UUID>,
        requestParameters: RequestParameters? = null,
    ) {
        runBlocking {
            client.deleteNotifications(notificationIds, requestParameters)
        }
    }

    @JvmOverloads
    fun setMshConfigurations(
        configurations: List<MshConfiguration>,
        requestParameters: RequestParameters? = null,
    ) {
        runBlocking {
            client.setMshConfigurations(configurations, requestParameters)
        }
    }

    @JvmOverloads
    fun deleteMshConfigurations(
        herIds: Collection<Int>,
        requestParameters: RequestParameters? = null,
    ) {
        runBlocking {
            client.deleteMshConfigurations(herIds, requestParameters)
        }
    }

    @JvmOverloads
    fun markMessageDownloaded(
        id: UUID,
        receiverHerId: Int,
        requestParameters: RequestParameters? = null,
    ) {
        runBlocking {
            client.markMessageDownloaded(id, receiverHerId, requestParameters)
        }
    }

    @JvmOverloads
    fun getStatus(
        id: UUID,
        requestParameters: RequestParameters? = null,
    ): List<StatusInfo> = runBlocking { client.getStatus(id, requestParameters) }

}
