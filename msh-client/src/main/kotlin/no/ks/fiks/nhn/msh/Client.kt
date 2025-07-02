package no.ks.fiks.nhn.msh

import no.ks.fiks.hdir.FeilmeldingForApplikasjonskvittering
import no.ks.fiks.hdir.StatusForMottakAvMelding
import no.ks.fiks.helseid.HelseIdClient
import no.ks.fiks.helseid.dpop.ProofBuilder
import no.ks.fiks.nhn.ar.AdresseregisteretClient
import no.ks.fiks.nhn.ar.AdresseregisteretService
import no.ks.fiks.nhn.edi.BusinessDocumentDeserializer
import no.ks.fiks.nhn.edi.BusinessDocumentSerializer
import no.ks.fiks.nhn.flr.FastlegeregisteretClient
import no.nhn.msh.v2.api.MessagesControllerApi
import no.nhn.msh.v2.model.AppRecError
import no.nhn.msh.v2.model.AppRecStatus
import no.nhn.msh.v2.model.PostAppRecRequest
import no.nhn.msh.v2.model.PostMessageRequest
import java.util.*
import no.ks.fiks.nhn.ar.Credentials as ArCredentials
import no.ks.fiks.nhn.flr.Credentials as FlrCredentials
import no.nhn.msh.v2.model.Message as NhnMessage

private const val API_VERSION = "2"

private const val CONTENT_TYPE = "application/xml"
private const val CONTENT_TRANSFER_ENCODING = "base64"

class Client(
    configuration: Configuration,
) {

    private val mshClient = MshFeignClientBuilder.build(
        mshBaseUrl = configuration.environments.mshBaseUrl,
        helseIdClient = HelseIdClient(
            no.ks.fiks.helseid.Configuration(
                clientId = configuration.helseId.clientId,
                jwk = configuration.helseId.jwk,
                environment = configuration.environments.helseIdEnvironment,
            ),
        ),
        proofBuilder = ProofBuilder(configuration.helseId.jwk),
        tokenParams = configuration.helseId.tokenParams,
    )

    private val flrClient = FastlegeregisteretClient(
        url = configuration.environments.fastlegeregisterUrl,
        credentials = configuration.fastlegeregisteret.let {
            FlrCredentials(
                username = it.username,
                password = it.password,
            )
        },
    )
    private val arClient = AdresseregisteretClient(
        AdresseregisteretService(
            url = configuration.environments.adresseregisterUrl,
            credentials = configuration.adresseregisteret.let {
                ArCredentials(
                    username = it.username,
                    password = it.password,
                )
            },
        )
    )
    private val receiverBuilder = GpForPersonReceiverBuilder(flrClient, arClient)

    private val sourceSystem = configuration.sourceSystem

    fun sendMessageToGPForPerson(businessDocument: GPForPersonOutgoingBusinessDocument) {
        sendMessage(
            OutgoingBusinessDocument(
                id = businessDocument.id,
                sender = businessDocument.sender,
                receiver = receiverBuilder.buildGpForPersonReceiver(businessDocument.person),
                message = businessDocument.message,
                vedlegg = businessDocument.vedlegg,
                version = businessDocument.version,
            )
        )
    }

    fun sendMessage(businessDocument: OutgoingBusinessDocument) {
        mshClient
            .postMessage(
                API_VERSION, sourceSystem, PostMessageRequest()
                    .contentType(CONTENT_TYPE)
                    .contentTransferEncoding(CONTENT_TRANSFER_ENCODING)
                    .businessDocument(Base64.getEncoder().encodeToString(BusinessDocumentSerializer.serializeNhnMessage(businessDocument).toByteArray()))
            )
    }

    fun getMessages(receiverHerId: Int): List<Message> {
        return mshClient
            .getMessages(
                API_VERSION, sourceSystem, MessagesControllerApi.GetMessagesQueryParams()
                    .receiverHerIds(setOf(receiverHerId))
            )
            .map { it.toMessageInfo() }

    }

    fun getMessagesWithMetadata(receiverHerId: Int): List<MessageWithMetadata> {
        return mshClient
            .getMessages(
                API_VERSION, sourceSystem, MessagesControllerApi.GetMessagesQueryParams()
                    .receiverHerIds(setOf(receiverHerId))
                    .includeMetadata(true)
            )
            .map { it.toMessageInfoWithMetadata() }

    }

    fun getMessage(id: UUID): MessageWithMetadata {
        return mshClient
            .getMessage(id, API_VERSION, sourceSystem)
            .toMessageInfoWithMetadata()
    }

    fun getBusinessDocument(id: UUID): IncomingBusinessDocument {
        return mshClient
            .getBusinessDocument(id, API_VERSION, sourceSystem)
            .let {
                BusinessDocumentDeserializer.deserializeMsgHead(String(Base64.getDecoder().decode(it.businessDocument)))
            }
    }

    fun getApplicationReceipt(id: UUID): IncomingApplicationReceipt {
        return mshClient
            .getBusinessDocument(id, API_VERSION, sourceSystem)
            .let {
                BusinessDocumentDeserializer.deserializeAppRec(String(Base64.getDecoder().decode(it.businessDocument)))
            }
    }

    fun sendApplicationReceipt(receipt: OutgoingApplicationReceipt) {
        if (receipt.status == StatusForMottakAvMelding.OK && !receipt.errors.isNullOrEmpty()) throw IllegalArgumentException("Error messages are not allowed when status is OK")
        if (receipt.status != StatusForMottakAvMelding.OK && receipt.errors.isNullOrEmpty()) throw IllegalArgumentException("Must provide at least one error message if status is not OK")

        mshClient.postAppRec(
            receipt.acknowledgedId, receipt.senderHerId, API_VERSION, sourceSystem, PostAppRecRequest()
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
        mshClient.markMessageAsRead(id, receiverHerId, API_VERSION, sourceSystem)
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