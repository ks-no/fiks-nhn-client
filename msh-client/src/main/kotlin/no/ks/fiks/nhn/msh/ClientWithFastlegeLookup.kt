package no.ks.fiks.nhn.msh

import no.ks.fiks.nhn.ar.AdresseregisteretClient
import no.ks.fiks.nhn.flr.FastlegeregisteretClient
import java.util.UUID

open class ClientWithFastlegeLookup(
    internalClient: MshInternalClient,
    flrClient: FastlegeregisteretClient,
    arClient: AdresseregisteretClient,
    messageHandlers: List<MessageHandler> = emptyList(),
) : Client(internalClient, messageHandlers) {

    private val receiverBuilder = GpForPersonReceiverBuilder(flrClient, arClient)

    suspend fun sendMessageToGPForPerson(
        businessDocument: GPForPersonOutgoingBusinessDocument,
        requestParameters: RequestParameters? = null,
    ): UUID =
        sendMessage(
            OutgoingBusinessDocument(
                id = businessDocument.id,
                sender = businessDocument.sender,
                receiver = receiverBuilder.buildGpForPersonReceiver(businessDocument.person),
                message = businessDocument.message,
                vedlegg = businessDocument.vedlegg,
                version = businessDocument.version,
                conversationRef = businessDocument.conversationRef,
            ),
            requestParameters,
        )

}