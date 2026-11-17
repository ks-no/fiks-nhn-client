package no.ks.fiks.nhn.msh

import kotlinx.coroutines.runBlocking
class BlockingClient(
    private val client: Client,
) {

    @JvmOverloads
    fun sendMessage(
        businessDocument: OutgoingBusinessDocument,
        requestParameters: RequestParameters? = null,
    ): String? = runBlocking { client.sendMessage(businessDocument, requestParameters) }

    @JvmOverloads
    fun getMessage(
        id: String,
        requestParameters: RequestParameters? = null,
    ): MessageWithMetadata = runBlocking { client.getMessage(id, requestParameters) }

    @JvmOverloads
    fun getBusinessDocument(
        id: String,
        requestParameters: RequestParameters? = null,
    ): IncomingBusinessDocument = runBlocking { client.getBusinessDocument(id, requestParameters) }

    @JvmOverloads
    fun getApplicationReceipt(
        id: String,
        requestParameters: RequestParameters? = null,
    ): IncomingApplicationReceipt = runBlocking { client.getApplicationReceipt(id, requestParameters) }

    @JvmOverloads
    fun getStatus(
        messageId: String,
        requestParameters: RequestParameters? = null,
    ): GetStatusResponse = runBlocking { client.getStatus(messageId, requestParameters) }

    @JvmOverloads
    fun sendApplicationReceipt(
        receipt: OutgoingApplicationReceipt,
        requestParameters: RequestParameters? = null,
    ) : String? = runBlocking { client.sendApplicationReceipt(receipt, requestParameters) }

    @JvmOverloads
    fun getUnreadNotifications(
        herIds: List<Int>,
        notificationsToFetch: Int? = null,
        requestParameters: RequestParameters? = null,
    ): List<UnreadNotification> = runBlocking { client.getUnreadNotifications(herIds, notificationsToFetch, requestParameters) }

    @JvmOverloads
    fun deleteNotifications(
        notificationIds: List<String>,
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
        herIds: List<Int>,
        requestParameters: RequestParameters? = null,
    ) {
        runBlocking {
            client.deleteMshConfigurations(herIds, requestParameters)
        }
    }

    @JvmOverloads
    fun markMessageDownloaded(
        id: String,
        receiverHerId: Int,
        requestParameters: RequestParameters? = null,
    ) {
        runBlocking {
            client.markMessageDownloaded(id, receiverHerId, requestParameters)
        }
    }

}
