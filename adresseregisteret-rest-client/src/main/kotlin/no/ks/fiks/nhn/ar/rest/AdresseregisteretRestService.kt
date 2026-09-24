package no.ks.fiks.nhn.ar.rest

import no.ks.fiks.helseid.AccessTokenRequestBuilder
import no.ks.fiks.helseid.Configuration as HelseIdConfiguration
import no.ks.fiks.helseid.HelseIdClient
import no.ks.fiks.helseid.TokenType
import no.ks.fiks.helseid.dpop.ProofBuilder
import no.nhn.register.communicationparty.rest.api.CommunicationPartyApi
import no.nhn.register.communicationparty.rest.invoker.ApiClient
import no.nhn.register.communicationparty.rest.model.CommunicationParty as GeneratedCommunicationParty

open class AdresseregisteretRestService(
    private val url: String,
    helseIdConfiguration: HelseIdConfiguration,
    accessTokenRequestBuilder: AccessTokenRequestBuilder? = null,
) {
    private val accessTokenRequest = (accessTokenRequestBuilder ?: AccessTokenRequestBuilder())
        .tokenType(TokenType.DPOP)
        .build()

    private val authInterceptor = DpopAuthInterceptor(
        baseUrl = url,
        helseIdClient = HelseIdClient(helseIdConfiguration),
        proofBuilder = ProofBuilder(helseIdConfiguration.jwk),
        accessTokenRequest = accessTokenRequest,
    )

    private val api =
        ApiClient().apply {
            setBasePath(url)
            addAuthorization("helseid-dpop", authInterceptor)
        }.buildClient(CommunicationPartyApi::class.java)

    open fun getCommunicationPartyDetails(herId: Int): GeneratedCommunicationParty? =
        api.apiV1CommunicationpartyHerIdGet(herId)
}

