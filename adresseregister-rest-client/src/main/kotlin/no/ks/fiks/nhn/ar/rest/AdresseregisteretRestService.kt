package no.ks.fiks.nhn.ar.rest

import feign.RequestInterceptor
import feign.RequestTemplate
import no.ks.fiks.helseid.AccessTokenRequestBuilder
import no.ks.fiks.helseid.HelseIdClient
import no.ks.fiks.helseid.dpop.Endpoint
import no.ks.fiks.helseid.dpop.HttpMethod
import no.ks.fiks.helseid.dpop.ProofBuilder
import no.ks.fiks.helseid.http.DpopHttpRequestHelper
import no.nhn.register.communicationparty.rest.api.CommunicationPartyApi
import no.nhn.register.communicationparty.rest.invoker.ApiClient
import no.nhn.register.communicationparty.rest.model.CommunicationParty as GeneratedCommunicationParty

open class AdresseregisteretRestService(
    private val url: String,
    helseIdClient: HelseIdClient,
    proofBuilder: ProofBuilder,
    private val accessTokenRequestBuilder: AccessTokenRequestBuilder? = null,
) {
    private val authInterceptor = HelseIdRequestInterceptor(url, helseIdClient, proofBuilder, accessTokenRequestBuilder)

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
    helseIdClient: HelseIdClient,
    proofBuilder: ProofBuilder,
    private val accessTokenRequestBuilder: AccessTokenRequestBuilder?,
) : RequestInterceptor {
    private val httpHelper by lazy { DpopHttpRequestHelper(helseIdClient, proofBuilder) }

    override fun apply(template: RequestTemplate) {
        val endpoint = Endpoint(
            method = HttpMethod.valueOf(template.method()),
            url = buildRequestUrl(template),
        )

        if (accessTokenRequestBuilder == null) {
            httpHelper.addAuthorizationHeader(endpoint) { name, value ->
                template.header(name, value)
            }
        } else {
            httpHelper.addAuthorizationHeader(endpoint, accessTokenRequestBuilder) { name, value ->
                template.header(name, value)
            }
        }
    }

    private fun buildRequestUrl(template: RequestTemplate): String {
        val path = template.path()
        val separator = if (path.startsWith("/")) "" else "/"
        return "${baseUrl.trimEnd('/')}$separator$path"
    }
}
