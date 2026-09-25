package no.ks.fiks.nhn.ar.rest

import feign.RequestInterceptor
import feign.RequestTemplate
import mu.KotlinLogging
import no.ks.fiks.helseid.HelseIdClient
import no.ks.fiks.helseid.dpop.Endpoint
import no.ks.fiks.helseid.dpop.HttpMethod
import no.ks.fiks.helseid.dpop.ProofBuilder
import no.ks.fiks.helseid.http.DpopHeaderHelper

private val log = KotlinLogging.logger {}

internal class DpopAuthInterceptor(
    private val baseUrl: String,
    private val helseIdClient: HelseIdClient,
    private val proofBuilder: ProofBuilder,
    private val accessTokenRequest: no.ks.fiks.helseid.AccessTokenRequest,
) : RequestInterceptor {
    override fun apply(template: RequestTemplate) {
        try {
            val endpoint = Endpoint(
                method = HttpMethod.valueOf(template.method()),
                url = buildRequestUrl(template),
            )

            val tokenResponse = helseIdClient.getAccessToken(accessTokenRequest)
            val dpopProof = proofBuilder.buildProof(endpoint, accessToken = tokenResponse.accessToken)
            DpopHeaderHelper.setHeaders(tokenResponse.accessToken, dpopProof) { name: String, value: String ->
                template.header(name, value)
            }
        } catch (e: Exception) {
            log.error(e) { "Failed to add DPoP authorization headers" }
            throw e
        }
    }

    private fun buildRequestUrl(template: RequestTemplate): String {
        val path = template.path()
        val separator = if (path.startsWith("/")) "" else "/"
        return baseUrl.trimEnd('/') + separator + path
    }
}


