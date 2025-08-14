package no.ks.fiks.nhn.msh

import no.ks.fiks.nhn.ar.AdresseregisteretClient
import no.ks.fiks.nhn.flr.FastlegeregisteretClient

class ClientWithFastlegeLookup(
    apiService: ApiService,
    flrClient: FastlegeregisteretClient,
    arClient: AdresseregisteretClient,
) : Client(apiService) {

    private val receiverBuilder = GpForPersonReceiverBuilder(flrClient, arClient)

    fun sendMessageToGPForPerson(
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