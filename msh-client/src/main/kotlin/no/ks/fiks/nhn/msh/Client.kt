package no.ks.fiks.nhn.msh

import no.ks.fiks.hdir.FeilmeldingForApplikasjonskvittering
import no.ks.fiks.hdir.StatusForMottakAvMelding
import no.ks.fiks.nhn.edi.BusinessDocumentDeserializer
import no.ks.fiks.nhn.edi.BusinessDocumentSerializer
import no.nhn.msh.v2.model.AppRecError
import no.nhn.msh.v2.model.AppRecStatus
import no.nhn.msh.v2.model.PostAppRecRequest
import no.nhn.msh.v2.model.PostMessageRequest
import java.util.*
import no.nhn.msh.v2.model.Message as NhnMessage

private const val CONTENT_TYPE = "application/xml"
private const val CONTENT_TRANSFER_ENCODING = "base64"

open class Client(
    private val internalClient: MshInternalClient,
) {

    @JvmOverloads
    suspend fun sendMessage(
        businessDocument: OutgoingBusinessDocument,
        requestParameters: RequestParameters? = null,
    ): UUID =
        internalClient
            .postMessage(
                request = PostMessageRequest()
                    .contentType(CONTENT_TYPE)
                    .contentTransferEncoding(CONTENT_TRANSFER_ENCODING)
                    .businessDocument(Base64.getEncoder().encodeToString(BusinessDocumentSerializer.serializeNhnMessage(businessDocument).toByteArray())),
                requestParams = requestParameters,
            )

    @JvmOverloads
    suspend fun getMessages(
        receiverHerId: Int,
        requestParameters: RequestParameters? = null,
    ): List<Message> {
        return internalClient
            .getMessages(
                receiverHerId = receiverHerId,
                includeMetadata = false,
                requestParams = requestParameters,
            )
            .map { it.toMessageInfo() }
    }

    @JvmOverloads
    suspend fun getMessagesWithMetadata(
        receiverHerId: Int,
        requestParameters: RequestParameters? = null,
    ): List<MessageWithMetadata> {
        return internalClient
            .getMessages(
                receiverHerId = receiverHerId,
                includeMetadata = true,
                requestParams = requestParameters,
            )
            .map { it.toMessageInfoWithMetadata() }
    }

    @JvmOverloads
    suspend fun getMessage(
        id: UUID,
        requestParameters: RequestParameters? = null,
    ): MessageWithMetadata {
        return internalClient
            .getMessage(id, requestParameters)
            .toMessageInfoWithMetadata()
    }

    @JvmOverloads
    suspend fun getBusinessDocument(
        id: UUID,
        requestParameters: RequestParameters? = null,
    ): IncomingBusinessDocument {
        return internalClient
            .getBusinessDocument(id, requestParameters)
            .let {
                if (it.contentTransferEncoding != CONTENT_TRANSFER_ENCODING) throw IllegalArgumentException("'${it.contentTransferEncoding}' is not a supported transfer encoding")
                if (it.contentType != CONTENT_TYPE) throw IllegalArgumentException("'${it.contentType}' is not a supported content type")
                BusinessDocumentDeserializer.deserializeMsgHead(String(Base64.getDecoder().decode(it.businessDocument)))
            }
    }

    @JvmOverloads
    suspend fun getApplicationReceipt(
        id: UUID,
        requestParameters: RequestParameters? = null,
    ): IncomingApplicationReceipt {
        return internalClient
            .getBusinessDocument(id, requestParameters)
            .let {
                if (it.contentTransferEncoding != CONTENT_TRANSFER_ENCODING) throw IllegalArgumentException("'${it.contentTransferEncoding}' is not a supported transfer encoding")
                if (it.contentType != CONTENT_TYPE) throw IllegalArgumentException("'${it.contentType}' is not a supported content type")
                BusinessDocumentDeserializer.deserializeAppRec(String(Base64.getDecoder().decode(it.businessDocument)))
            }
    }

    @JvmOverloads
    suspend fun sendApplicationReceipt(
        receipt: OutgoingApplicationReceipt,
        requestParameters: RequestParameters? = null,
    ) {
        if (receipt.status == StatusForMottakAvMelding.OK && !receipt.errors.isNullOrEmpty()) throw IllegalArgumentException("Error messages are not allowed when status is OK")
        if (receipt.status != StatusForMottakAvMelding.OK && receipt.errors.isNullOrEmpty()) throw IllegalArgumentException("Must provide at least one error message if status is not OK")

        internalClient.postAppRec(
            id = receipt.acknowledgedId,
            senderHerId = receipt.senderHerId,
            request = PostAppRecRequest()
                .appRecStatus(receipt.status.toAppRecStatus())
                .appRecErrorList(receipt.errors?.toAppRecErrors()),
            requestParams = requestParameters,
        )
    }

    private fun StatusForMottakAvMelding.toAppRecStatus() = when (this) {
        StatusForMottakAvMelding.OK -> AppRecStatus.OK
        StatusForMottakAvMelding.OK_FEIL_I_DELMELDING -> AppRecStatus.OK_ERROR_IN_MESSAGE_PART
        StatusForMottakAvMelding.AVVIST -> AppRecStatus.REJECTED
    }

    private fun List<ApplicationReceiptError>.toAppRecErrors() = map { it.toAppRecError() }

    private fun ApplicationReceiptError.toAppRecError(): AppRecError {
        if (type == FeilmeldingForApplikasjonskvittering.UKJENT) throw IllegalArgumentException("Ukjent is not a valid error type for an outgoing application receipt")
        return AppRecError()
            .errorCode(type.verdi)
            .details(details)
    }

    @JvmOverloads
    suspend fun markMessageRead(
        id: UUID,
        receiverHerId: Int,
        requestParameters: RequestParameters? = null,
    ) {
        internalClient.markMessageRead(
            id = id,
            senderHerId = receiverHerId,
            requestParams = requestParameters,
        )
    }

    private fun NhnMessage.toMessageInfo() = Message(
        id = id,
        receiverHerId = receiverHerId,
    )

    private fun NhnMessage.toMessageInfoWithMetadata() = MessageWithMetadata(
        id = id,
        contentType = contentType,
        receiverHerId = receiverHerId,
        senderHerId = senderHerId,
        businessDocumentId = businessDocumentId,
        businessDocumentDate = businessDocumentGenDate,
        isAppRec = isAppRec,
    )

}