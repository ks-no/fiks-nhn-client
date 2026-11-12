package no.ks.fiks.nhn.msh

import mu.KotlinLogging
import no.ks.fiks.hdir.FeilmeldingForApplikasjonskvittering
import no.ks.fiks.hdir.MeldingensFunksjon
import no.ks.fiks.hdir.StatusForMottakAvMelding
import no.ks.fiks.nhn.edi.BusinessDocumentDeserializer
import no.ks.fiks.nhn.edi.BusinessDocumentSerializer
import no.ks.fiks.nhn.parseOffsetDateTimeOrNull
import no.nhn.msh.v3.model.AppRecError as NhnAppRecError
import no.nhn.msh.v3.model.AppRecStatus as NhnAppRecStatus
import no.nhn.msh.v3.model.DeliveryState as NhnDeliveryState
import no.nhn.msh.v3.model.GetMessageResponse as NhnGetMessageResponse
import no.nhn.msh.v3.model.MshConfiguration as NhnMshConfiguration
import no.nhn.msh.v3.model.MshConfigurationRejectMessageFilters as NhnMshConfigurationRejectMessageFilters
import no.nhn.msh.v3.model.NotificationType as NhnNotificationType
import no.nhn.msh.v3.model.PostAppRecRequest as NhnPostAppRecRequest
import no.nhn.msh.v3.model.PostMessageRequest as NhnPostMessageRequest
import no.nhn.msh.v3.model.ReceiveNotificationChannel as NhnReceiveNotificationChannel
import no.nhn.msh.v3.model.StatusInfo as NhnStatusInfo
import no.nhn.msh.v3.model.UnreadNotification as NhnUnreadNotification
import java.util.*

private const val CONTENT_TYPE = "application/xml"
private const val CONTENT_TRANSFER_ENCODING = "base64"

private val log = KotlinLogging.logger {}

open class Client @JvmOverloads constructor(
    private val internalClient: MshInternalClient,
    private val applicationMetadata: ApplicationMetadata,
    private val messageHandlers: List<MessageHandler> = emptyList(),
) {

    suspend fun sendMessage(
        businessDocument: OutgoingBusinessDocument,
        requestParameters: RequestParameters? = null,
    ): String? {
        val request = NhnPostMessageRequest()
            .businessDocument(Base64.getEncoder().encodeToString(BusinessDocumentSerializer.serializeNhnMessage(businessDocument).toByteArray()))
            .senderHerId(businessDocument.resolveSenderHerId())
            .receiverHerIds(businessDocument.resolveReceiverHerIds())
            .contentType(CONTENT_TYPE)
            .contentTransferEncoding(CONTENT_TRANSFER_ENCODING)
            .messageTypeIdentificator(businessDocument.resolveMessageType().verdi)
            .applicationName(applicationMetadata.applicationName)
            .applicationVersion(applicationMetadata.applicationVersion)
        return internalClient.postMessage(
            request = request,
            requestParams = requestParameters,
        )
    }

    suspend fun getMessage(
        id: String,
        requestParameters: RequestParameters? = null,
    ): MessageWithMetadata {
        return internalClient.getMessage(id, requestParameters).toMessageInfoWithMetadata()
    }

    suspend fun getBusinessDocument(
        id: String,
        requestParameters: RequestParameters? = null,
    ): IncomingBusinessDocument {
        return internalClient
            .getBusinessDocument(id, requestParameters)
            .let {
                if (it.contentTransferEncoding != CONTENT_TRANSFER_ENCODING) throw IllegalArgumentException("'${it.contentTransferEncoding}' is not a supported transfer encoding")
                if (it.contentType != CONTENT_TYPE) throw IllegalArgumentException("'${it.contentType}' is not a supported content type")

                val encodedBusinessDocument = it.businessDocument
                val xml = runCatching { String(Base64.getDecoder().decode(encodedBusinessDocument)) }
                    .onFailure { e -> notifyHandlers { h -> h.onIncomingBusinessDocumentDecodingFailed(id, encodedBusinessDocument, e) } }
                    .getOrThrow()
                runCatching { BusinessDocumentDeserializer.deserializeMsgHead(xml) }
                    .onSuccess { document -> notifyHandlers { h -> h.onIncomingBusinessDocumentReceived(id, xml, document) } }
                    .onFailure { e -> notifyHandlers { h -> h.onIncomingBusinessDocumentDeserializationFailed(id, xml, e) } }
                    .getOrThrow()
            }
    }

    suspend fun getApplicationReceipt(
        id: String,
        requestParameters: RequestParameters? = null,
    ): IncomingApplicationReceipt {
        return internalClient
            .getBusinessDocument(id, requestParameters)
            .let {
                if (it.contentTransferEncoding != CONTENT_TRANSFER_ENCODING) throw IllegalArgumentException("'${it.contentTransferEncoding}' is not a supported transfer encoding")
                if (it.contentType != CONTENT_TYPE) throw IllegalArgumentException("'${it.contentType}' is not a supported content type")

                val encodedBusinessDocument = it.businessDocument
                val xml = runCatching { String(Base64.getDecoder().decode(encodedBusinessDocument)) }
                    .onFailure { e -> notifyHandlers { h -> h.onIncomingApplicationReceiptDecodingFailed(id, encodedBusinessDocument, e) } }
                    .getOrThrow()
                runCatching { BusinessDocumentDeserializer.deserializeAppRec(xml) }
                    .onSuccess { receipt -> notifyHandlers { h -> h.onIncomingApplicationReceiptReceived(id, xml, receipt) } }
                    .onFailure { e -> notifyHandlers { h -> h.onIncomingApplicationReceiptDeserializationFailed(id, xml, e) } }
                    .getOrThrow()
            }
    }

    suspend fun getStatus(
        messageId: String,
        requestParameters: RequestParameters? = null,
    ): GetStatusResponse =
        GetStatusResponse(
            statusList = internalClient.getStatus(messageId, requestParameters).statusList?.map { status ->
                status.toStatusInfo()
            },
        )

    suspend fun sendApplicationReceipt(
        receipt: OutgoingApplicationReceipt,
        requestParameters: RequestParameters? = null,
    ): String? {
        if (receipt.status == StatusForMottakAvMelding.OK && !receipt.errors.isNullOrEmpty()) throw IllegalArgumentException("Error messages are not allowed when status is OK")
        if (receipt.status != StatusForMottakAvMelding.OK && receipt.errors.isNullOrEmpty()) throw IllegalArgumentException("Must provide at least one error message if status is not OK")

        val request = NhnPostAppRecRequest()
            .appRecSenderHerId(receipt.senderHerId)
            .appRecStatus(receipt.status.toAppRecStatus())
            .appRecErrorList(receipt.errors?.toAppRecErrors())
            .applicationName(applicationMetadata.applicationName)
            .applicationVersion(applicationMetadata.applicationVersion)
        return internalClient.postAppRec(
            id = receipt.acknowledgedId,
            request = request,
            requestParams = requestParameters,
        )
    }

    suspend fun getUnreadNotifications(
        herIds: List<Int>,
        notificationsToFetch: Int? = null,
        requestParameters: RequestParameters? = null,
    ): List<UnreadNotification> =
        internalClient.getUnreadNotifications(
            herIds = herIds,
            notificationsToFetch = notificationsToFetch,
            requestParams = requestParameters,
        ).map { it.toUnreadNotification() }

    suspend fun deleteNotifications(
        notificationIds: List<String>,
        requestParameters: RequestParameters? = null,
    ) {
        internalClient.deleteNotifications(
            notificationIds = notificationIds,
            requestParams = requestParameters,
        )
    }

    suspend fun setMshConfigurations(
        configurations: List<MshConfiguration>,
        requestParameters: RequestParameters? = null,
    ) {
        internalClient.setMshConfigurations(
            configurations = configurations.map { it.toApiMshConfiguration() },
            requestParams = requestParameters,
        )
    }

    suspend fun deleteMshConfigurations(
        herIds: List<Int>,
        requestParameters: RequestParameters? = null,
    ) {
        internalClient.deleteMshConfigurations(
            herIds = herIds,
            requestParams = requestParameters,
        )
    }

    suspend fun markMessageDownloaded(
        id: String,
        receiverHerId: Int,
        requestParameters: RequestParameters? = null,
    ) = internalClient.markMessageDownloaded(id, receiverHerId, requestParameters)

    private fun notifyHandlers(block: (MessageHandler) -> Unit) {
        messageHandlers.forEach { h ->
            runCatching { block(h) }
                .onFailure { log.warn(it) { "Message handler '${h::class}' threw an exception" } }
        }
    }

    private fun StatusForMottakAvMelding.toAppRecStatus() = when (this) {
        StatusForMottakAvMelding.OK -> NhnAppRecStatus.OK
        StatusForMottakAvMelding.OK_FEIL_I_DELMELDING -> NhnAppRecStatus.OK_ERROR_IN_MESSAGE_PART
        StatusForMottakAvMelding.AVVIST -> NhnAppRecStatus.REJECTED
    }

    private fun OutgoingApplicationReceiptError.toAppRecError(): NhnAppRecError {
        if (type == FeilmeldingForApplikasjonskvittering.UKJENT) throw IllegalArgumentException("Ukjent is not a valid error type for an outgoing application receipt")
        return NhnAppRecError()
            .errorCode(type.verdi)
            .description(type.navn)
            .oid(type.kodeverk)
            .details(details)
    }

    private fun List<OutgoingApplicationReceiptError>.toAppRecErrors() = map { it.toAppRecError() }

    private fun OutgoingBusinessDocument.resolveSenderHerId(): Int =
        resolveHerId(sender.child, sender.parent)
            ?: throw IllegalArgumentException("Outgoing business document is missing sender HER-id")

    private fun OutgoingBusinessDocument.resolveReceiverHerIds(): List<Int> =
        listOfNotNull(resolveHerId(receiver.child, receiver.parent))
            .ifEmpty { throw IllegalArgumentException("Outgoing business document is missing receiver HER-id") }

    private fun resolveHerId(primary: CommunicationParty, fallback: CommunicationParty): Int? =
        primary.herId ?: fallback.herId

    private fun OutgoingBusinessDocument.resolveMessageType(): MeldingensFunksjon =
        BusinessDocumentSerializer.resolveMessageType(version)

    private fun NhnGetMessageResponse.toMessageInfoWithMetadata() = MessageWithMetadata(
        id = id,
        contentType = contentType,
        receiverHerIds = receiverHerIds?.toList(),
        senderHerId = senderHerId,
        businessDocumentId = businessDocumentId,
        businessDocumentDate = businessDocumentGenDate?.parseOffsetDateTimeOrNull(),
        businessDocumentMsgType = businessDocumentMsgType,
    )

    private fun NhnUnreadNotification.toUnreadNotification() = UnreadNotification(
        notificationId = notificationId,
        relatedMessageId = relatedMessageId,

        type = when (type) {
            NhnNotificationType.NEW_MESSAGE -> NotificationType.NEW_MESSAGE
            NhnNotificationType.REFUSED_MESSAGE -> NotificationType.REFUSED_MESSAGE
            NhnNotificationType.MESSAGE_SENT_STATE_UPDATED -> NotificationType.MESSAGE_SENT_STATE_UPDATED
            NhnNotificationType.MESSAGE_APPREC_INFO_UPDATED -> NotificationType.MESSAGE_APPREC_INFO_UPDATED
            NhnNotificationType.MESSAGE_DELIVERY_STATE_UPDATED -> NotificationType.MESSAGE_DELIVERY_STATE_UPDATED
        },
        notificationReceiverHerId = notificationReceiverHerId,
        notificationTriggeredByHerId = notificationTriggeredByHerId,
        description = description,
        createdAt = createdAt,
    )

    private fun NhnStatusInfo.toStatusInfo() = StatusInfo(
        receiverHerId = receiverHerId,
        sent = sent,
        deliveryState = transportDeliveryState.toDeliveryState(),
        apprecInfo = apprecInfo?.let { info ->
            AppRecInfo(
                appRecStatus = info.appRecStatus?.toAppRecStatus(),
                appRecId = info.appRecId,
                appRecErrors = info.appRecErrorList?.map { it.toIncomingApplicationReceiptError() } ?: emptyList(),
            )
        },
    )

    private fun NhnDeliveryState.toDeliveryState() = when (this) {
        NhnDeliveryState.UNCONFIRMED -> DeliveryState.UNCONFIRMED
        NhnDeliveryState.ACKNOWLEDGED -> DeliveryState.ACKNOWLEDGED
        NhnDeliveryState.REJECTED -> DeliveryState.REJECTED
        NhnDeliveryState.ABANDONED -> DeliveryState.ABANDONED
    }

    private fun NhnAppRecStatus.toAppRecStatus() = when (this) {
        NhnAppRecStatus.OK -> AppRecStatus.OK
        NhnAppRecStatus.REJECTED -> AppRecStatus.REJECTED
        NhnAppRecStatus.OK_ERROR_IN_MESSAGE_PART -> AppRecStatus.OK_ERROR_IN_MESSAGE_PART
    }

    private fun NhnAppRecError.toIncomingApplicationReceiptError() = IncomingApplicationReceiptError(
        type = FeilmeldingForApplikasjonskvittering.entries.find { it.verdi == errorCode } ?: FeilmeldingForApplikasjonskvittering.UKJENT,
        details = details,
        errorCode = errorCode,
        description = description,
        oid = oid,
    )

    private fun MshConfiguration.toApiMshConfiguration() = NhnMshConfiguration()
        .herId(herId)
        .receiveNotificationChannel(NhnReceiveNotificationChannel.API)
        .clientLocked(clientLocked)
        .rejectMessageFilters(
            rejectMessageFilters?.let { filters ->
                NhnMshConfigurationRejectMessageFilters().messageFunction(filters.messageFunctions)
            }
        )

}
