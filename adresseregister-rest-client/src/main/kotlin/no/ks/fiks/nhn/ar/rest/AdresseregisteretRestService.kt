package no.ks.fiks.nhn.ar.rest

import no.nhn.register.communicationparty.rest.api.CommunicationPartyApi
import no.nhn.register.communicationparty.rest.invoker.ApiClient
import no.nhn.register.communicationparty.rest.model.CommunicationParty as GeneratedCommunicationParty

open class AdresseregisteretRestService(
    url: String,
    credentials: Credentials,
) {
    private val api: CommunicationPartyApi = ApiClient().apply {
        setBasePath(url)
        setCredentials(credentials.username, credentials.password)
    }.buildClient(CommunicationPartyApi::class.java)

    open fun getCommunicationPartyDetails(herId: Int): GeneratedCommunicationParty? =
        api.apiV1CommunicationpartyHerIdGet(herId)
}
