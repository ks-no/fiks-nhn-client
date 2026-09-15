package no.ks.fiks.nhn.ar.rest

import feign.RequestInterceptor
import feign.RequestTemplate
import no.ks.fiks.helseid.AccessTokenRequestBuilder
import no.ks.fiks.helseid.Configuration as HelseIdConfiguration
import no.ks.fiks.helseid.TokenType
import no.ks.fiks.helseid.dpop.Endpoint
import no.ks.fiks.helseid.dpop.HttpMethod
import no.ks.fiks.helseid.dpop.ProofBuilder
import no.ks.fiks.helseid.http.DpopHeaderHelper
import no.nhn.register.communicationparty.rest.api.CommunicationPartyApi
import no.nhn.register.communicationparty.rest.invoker.ApiClient
import no.nhn.register.communicationparty.rest.model.CommunicationParty as GeneratedCommunicationParty

private const val REQUIRED_SCOPE = "nhn:communicationparty/read"

open class AdresseregisteretRestService(
    private val url: String,
    helseIdConfiguration: HelseIdConfiguration,
    accessTokenRequestBuilder: AccessTokenRequestBuilder? = null,
) {
    private val accessTokenRequest = (accessTokenRequestBuilder ?: AccessTokenRequestBuilder())
        .tokenType(TokenType.DPOP)
        .build()

    private val tokenProvider = HelseIdTokenProvider(helseIdConfiguration, REQUIRED_SCOPE)
    private val proofBuilder = ProofBuilder(helseIdConfiguration.jwk)
    private val authInterceptor = HelseIdRequestInterceptor(url, tokenProvider, proofBuilder, accessTokenRequest)

    private val api by lazy {
        ApiClient().apply {
            setBasePath(url)
            addAuthorization("helseid-dpop", authInterceptor)
        }.buildClient(CommunicationPartyApi::class.java)
    }

    open fun getCommunicationPartyDetails(herId: Int): GeneratedCommunicationParty? =
        api.apiV1CommunicationpartyHerIdGet(herId)
}

private class HelseIdRequestInterceptor(
    private val baseUrl: String,
    private val tokenProvider: HelseIdTokenProvider,
    private val proofBuilder: ProofBuilder,
    private val accessTokenRequest: no.ks.fiks.helseid.AccessTokenRequest,
) : RequestInterceptor {
    override fun apply(template: RequestTemplate) {
        val endpoint = Endpoint(
            method = HttpMethod.valueOf(template.method()),
            url = buildRequestUrl(template),
        )

        val accessToken = tokenProvider.getAccessToken(accessTokenRequest).accessToken
        val dpopProof = proofBuilder.buildProof(endpoint, accessToken = accessToken)
        DpopHeaderHelper.setHeaders(accessToken, dpopProof) { name: String, value: String ->
            template.header(name, value)
        }
    }

    private fun buildRequestUrl(template: RequestTemplate): String {
        val path = template.path()
        val separator = if (path.startsWith("/")) "" else "/"
        return "${baseUrl.trimEnd('/')}$separator$path"
    }
}
