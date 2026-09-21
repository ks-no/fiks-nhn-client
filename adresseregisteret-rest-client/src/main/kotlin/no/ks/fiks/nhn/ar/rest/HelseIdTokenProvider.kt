package no.ks.fiks.nhn.ar.rest

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.benmanes.caffeine.cache.Caffeine
import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import mu.KotlinLogging
import no.ks.fiks.helseid.AccessTokenRequest
import no.ks.fiks.helseid.AccessTokenRequestBuilder
import no.ks.fiks.helseid.CachedHttpDiscoveryOpenIdConfiguration
import no.ks.fiks.helseid.Configuration
import no.ks.fiks.helseid.MultiTenantAccessTokenRequest
import no.ks.fiks.helseid.OrganizationNumberAccessTokenRequest
import no.ks.fiks.helseid.SingleTenantAccessTokenRequest
import no.ks.fiks.helseid.TokenResponse
import no.ks.fiks.helseid.TokenType
import no.ks.fiks.helseid.dpop.Endpoint
import no.ks.fiks.helseid.dpop.HttpMethod
import no.ks.fiks.helseid.dpop.ProofBuilder
import no.ks.fiks.helseid.http.ErrorCodes
import no.ks.fiks.helseid.http.Headers
import no.ks.fiks.helseid.http.HttpException
import org.apache.hc.client5.http.classic.HttpClient
import org.apache.hc.client5.http.classic.methods.HttpPost
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.core5.http.ClassicHttpResponse
import org.apache.hc.core5.http.message.BasicNameValuePair
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant
import java.util.Date
import java.util.UUID

private const val CLIENT_ASSERTION_TYPE_VALUE = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer"
private const val GRANT_TYPE_VALUE = "client_credentials"
private const val JWS_HEADER_TYPE_VALUE = "client-authentication+jwt"
private const val CLAIM_ASSERTION_DETAILS = "assertion_details"

private val jwsHeaderType = JOSEObjectType(JWS_HEADER_TYPE_VALUE)
private val jwtRequestLifetime = Duration.ofSeconds(5)
private val accessTokenLifetime = Duration.ofMinutes(5)
private val accessTokenRenewalThreshold = Duration.ofSeconds(30)
private val log = KotlinLogging.logger {}

private object FormFields {
    const val CLIENT_ID = "client_id"
    const val CLIENT_ASSERTION = "client_assertion"
    const val CLIENT_ASSERTION_TYPE = "client_assertion_type"
    const val GRANT_TYPE = "grant_type"
    const val SCOPE = "scope"
}

class HelseIdTokenProvider(
    configuration: Configuration,
    private val scope: String,
    private val httpClient: HttpClient = HttpClients.createMinimal(),
    private val openIdConfiguration: no.ks.fiks.helseid.OpenIdConfiguration =
        CachedHttpDiscoveryOpenIdConfiguration(configuration.environment.issuer),
) {
    private val clientId = configuration.clientId
    private val audience = configuration.environment.audience
    private val jwk = JWK.parse(configuration.jwk)
    private val signer = RSASSASigner(jwk.toRSAKey())
    private val dpopProofBuilder = ProofBuilder(configuration.jwk)
    private val mapper = ObjectMapper()
        .findAndRegisterModules()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    private val tokenCache = Caffeine.newBuilder()
        .expireAfterWrite(accessTokenLifetime.minus(accessTokenRenewalThreshold))
        .build<AccessTokenRequest, TokenResponse> { getNewAccessToken(it) }

    @JvmOverloads
    fun getAccessToken(request: AccessTokenRequest = AccessTokenRequestBuilder().tokenType(TokenType.DPOP).build()): TokenResponse =
        tokenCache.get(request)

    private fun getNewAccessToken(request: AccessTokenRequest) = when (request.tokenType) {
        TokenType.BEARER -> getNewBearerToken(request)
        TokenType.DPOP -> getNewDpopAccessToken(request)
    }

    private fun getNewBearerToken(request: AccessTokenRequest): TokenResponse {
        log.debug { "Renewing access token: $request" }
        return httpClient.execute(buildPostRequest(request)) {
            if (it.code >= 300) throw HttpException(it.code, it.readBodyAsString())
            mapper.readValue<InternalTokenResponse>(it.readBodyAsString()).toTokenResponse()
        }
    }

    private fun buildPostRequest(request: AccessTokenRequest) = HttpPost(openIdConfiguration.getTokenEndpoint()).apply {
        entity = buildUrlEncodedFormEntity(buildSignedJwt(request).serialize())
    }

    private fun getNewDpopAccessToken(request: AccessTokenRequest): TokenResponse {
        log.debug { "Renewing DPoP access token: $request" }
        val nonce = httpClient.execute(buildDpopPostRequest(request)) {
            if (it.code != 400) throw HttpException(it.code, it.readBodyAsString())

            val body = it.readBodyAsString()
            val error = mapper.readValue<InternalErrorResponse>(body)
            if (error.code != ErrorCodes.USE_DPOP_NONCE) throw HttpException(it.code, body)
            it.getFirstHeader(Headers.DPOP_NONCE)?.value
        }

        if (nonce == null) throw RuntimeException("Expected ${Headers.DPOP_NONCE} header to be set")

        return httpClient.execute(buildDpopPostRequest(request, nonce)) {
            if (it.code >= 300) throw HttpException(it.code, it.readBodyAsString())
            mapper.readValue<InternalTokenResponse>(it.readBodyAsString()).toTokenResponse()
        }
    }

    private fun buildDpopPostRequest(request: AccessTokenRequest, nonce: String? = null) = HttpPost(openIdConfiguration.getTokenEndpoint()).apply {
        entity = buildUrlEncodedFormEntity(buildSignedJwt(request).serialize())
        addHeader(
            Headers.DPOP,
            dpopProofBuilder.buildProof(
                Endpoint(HttpMethod.POST, openIdConfiguration.getTokenEndpoint().toString()),
                nonce,
            ),
        )
    }

    private fun buildUrlEncodedFormEntity(serializedJwtClaim: String) =
        UrlEncodedFormEntity(
            buildList {
                add(BasicNameValuePair(FormFields.CLIENT_ID, clientId))
                add(BasicNameValuePair(FormFields.CLIENT_ASSERTION, serializedJwtClaim))
                add(BasicNameValuePair(FormFields.CLIENT_ASSERTION_TYPE, CLIENT_ASSERTION_TYPE_VALUE))
                add(BasicNameValuePair(FormFields.GRANT_TYPE, GRANT_TYPE_VALUE))
                add(BasicNameValuePair(FormFields.SCOPE, scope))
            },
            StandardCharsets.UTF_8,
        )

    private fun buildSignedJwt(request: AccessTokenRequest) =
        Instant.now().let { now ->
            SignedJWT(
                JWSHeader.Builder(JWSAlgorithm.PS512)
                    .keyID(jwk.keyID)
                    .type(jwsHeaderType)
                    .build(),
                JWTClaimsSet.Builder()
                    .subject(clientId)
                    .issuer(clientId)
                    .audience(audience)
                    .issueTime(now.toDate())
                    .jwtID(UUID.randomUUID().toString())
                    .notBeforeTime(now.toDate())
                    .expirationTime(now.plus(jwtRequestLifetime).toDate())
                    .apply {
                        if (request is OrganizationNumberAccessTokenRequest) {
                            claim(CLAIM_ASSERTION_DETAILS, request.buildAssertionDetailsClaim())
                        }
                    }
                    .build(),
            ).apply {
                log.debug { "Generated JWT id: ${this.jwtClaimsSet.jwtid}" }
                sign(signer)
            }
        }

    private fun OrganizationNumberAccessTokenRequest.buildAssertionDetailsClaim() = when (this) {
        is SingleTenantAccessTokenRequest -> no.ks.fiks.helseid.AssertionDetailsBuilder.buildSingleTenantClaim(childOrganizationNumber)
        is MultiTenantAccessTokenRequest -> no.ks.fiks.helseid.AssertionDetailsBuilder.buildMultiTenantClaim(parentOrganizationNumber, childOrganizationNumber)
    }
}

private data class InternalTokenResponse(
    @param:JsonProperty("access_token")
    val accessToken: String,

    @param:JsonProperty("expires_in")
    val expiresIn: Int,

    @param:JsonProperty("token_type")
    val tokenType: String,

    @param:JsonProperty("scope")
    val scope: String,
) {
    fun toTokenResponse() = TokenResponse(
        accessToken = accessToken,
        expiresIn = expiresIn,
        tokenType = tokenType,
        scope = scope,
    )
}

private data class InternalErrorResponse(
    @param:JsonProperty("error")
    val code: String?,

    @param:JsonProperty("error_description")
    val description: String?,
)

private fun ClassicHttpResponse.readBodyAsString() = entity.content.readBytes().decodeToString()

private fun Instant.toDate() = Date(toEpochMilli())
