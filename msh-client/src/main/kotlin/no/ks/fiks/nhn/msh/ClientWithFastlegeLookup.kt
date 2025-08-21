package no.ks.fiks.nhn.msh

import no.ks.fiks.nhn.ar.AdresseregisteretClient
import no.ks.fiks.nhn.flr.FastlegeregisteretClient

class ClientWithFastlegeLookup(
    internalClient: MshInternalClient,
    flrClient: FastlegeregisteretClient,
    arClient: AdresseregisteretClient,
) : Client(internalClient) {

    private val receiverBuilder = GpForPersonReceiverBuilder(flrClient, arClient)

    suspend fun sendMessageToGPForPerson(
        businessDocument: GPForPersonOutgoingBusinessDocument,
        requestParameters: RequestParameters? = null,
    ) {
        sendMessage(
            OutgoingBusinessDocument(
                id = businessDocument.id,
                sender = businessDocument.sender,
                receiver = receiverBuilder.buildGpForPersonReceiver(businessDocument.person),
                message = businessDocument.message,
                vedlegg = businessDocument.vedlegg,
                version = businessDocument.version,
            ),
            requestParameters,
        )
    }

}