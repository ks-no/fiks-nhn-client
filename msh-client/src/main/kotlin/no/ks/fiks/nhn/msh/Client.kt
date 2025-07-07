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
    private val apiService: ApiService,
) {

    fun sendMessage(businessDocument: OutgoingBusinessDocument) {
        apiService
            .sendMessage(
                PostMessageRequest()
                    .contentType(CONTENT_TYPE)
                    .contentTransferEncoding(CONTENT_TRANSFER_ENCODING)
                    .businessDocument(Base64.getEncoder().encodeToString(BusinessDocumentSerializer.serializeNhnMessage(businessDocument).toByteArray()))
            )
    }

    fun getMessages(receiverHerId: Int): List<Message> {
        return apiService
            .getMessages(receiverHerId)
            .map { it.toMessageInfo() }

    }

    fun getMessagesWithMetadata(receiverHerId: Int): List<MessageWithMetadata> {
        return apiService
            .getMessagesWithMetadata(receiverHerId)
            .map { it.toMessageInfoWithMetadata() }

    }

    fun getMessage(id: UUID): MessageWithMetadata {
        return apiService
            .getMessage(id)
            .toMessageInfoWithMetadata()
    }

    fun getBusinessDocument(id: UUID): IncomingBusinessDocument {
        return apiService
            .getBusinessDocument(id)
            .let {
                if (it.contentTransferEncoding != CONTENT_TRANSFER_ENCODING) throw IllegalArgumentException("'${it.contentTransferEncoding}' is not a supported transfer encoding")
                if (it.contentType != CONTENT_TYPE) throw IllegalArgumentException("'${it.contentType}' is not a supported content type")
                BusinessDocumentDeserializer.deserializeMsgHead(String(Base64.getDecoder().decode(it.businessDocument)))
            }
    }

    fun getApplicationReceipt(id: UUID): IncomingApplicationReceipt {
        return apiService
            .getBusinessDocument(id)
            .let {
                if (it.contentTransferEncoding != CONTENT_TRANSFER_ENCODING) throw IllegalArgumentException("'${it.contentTransferEncoding}' is not a supported transfer encoding")
                if (it.contentType != CONTENT_TYPE) throw IllegalArgumentException("'${it.contentType}' is not a supported content type")
                BusinessDocumentDeserializer.deserializeAppRec(String(Base64.getDecoder().decode(it.businessDocument)))
            }
    }

    fun sendApplicationReceipt(receipt: OutgoingApplicationReceipt) {
        if (receipt.status == StatusForMottakAvMelding.OK && !receipt.errors.isNullOrEmpty()) throw IllegalArgumentException("Error messages are not allowed when status is OK")
        if (receipt.status != StatusForMottakAvMelding.OK && receipt.errors.isNullOrEmpty()) throw IllegalArgumentException("Must provide at least one error message if status is not OK")

        apiService.sendApplicationReceipt(
            receipt.acknowledgedId, receipt.senderHerId, PostAppRecRequest()
                .appRecStatus(receipt.status.toAppRecStatus())
                .appRecErrorList(receipt.errors?.toAppRecErrors())
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

    fun markMessageRead(id: UUID, receiverHerId: Int) {
        apiService.markMessageRead(id, receiverHerId)
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