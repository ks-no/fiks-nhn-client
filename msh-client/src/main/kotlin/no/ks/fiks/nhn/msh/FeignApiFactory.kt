package no.ks.fiks.nhn.msh

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import feign.Feign
import feign.RequestInterceptor
import feign.RequestTemplate
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import no.ks.fiks.helseid.AccessTokenRequestBuilder
import no.ks.fiks.helseid.HelseIdClient
import no.ks.fiks.helseid.TenancyType
import no.ks.fiks.helseid.dpop.Endpoint
import no.ks.fiks.helseid.dpop.HttpMethod
import no.ks.fiks.helseid.dpop.ProofBuilder
import no.ks.fiks.helseid.http.DpopHttpRequestHelper
import no.nhn.msh.v2.api.MessagesControllerApi
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

// Meldingstjener, MSH (Message Service Handler)
internal object FeignApiFactory {

    private val mapper = ObjectMapper()
        .registerModule(JavaTimeModule())
        .registerModule(SimpleModule().apply {
            addDeserializer(OffsetDateTime::class.java, object : JsonDeserializer<OffsetDateTime>() { // API returns ISO 8601 dates without offset that can't be parsed by the default deserializer
                override fun deserialize(parser: JsonParser, context: DeserializationContext): OffsetDateTime {
                    val local = LocalDateTime.parse(parser.text)
                    return OffsetDateTime.of(local, ZoneOffset.UTC)
                }
            })
        })

    fun createApi(
        mshBaseUrl: String,
        helseIdClient: HelseIdClient,
        proofBuilder: ProofBuilder,
        tokenParams: HelseIdTokenParameters? = null,
    ): MessagesControllerApi = create(mshBaseUrl, DpopHttpRequestHelper(helseIdClient, proofBuilder), tokenParams)

    private fun create(
        mshBaseUrl: String,
        httpHelper: DpopHttpRequestHelper,
        tokenParams: HelseIdTokenParameters?
    ): MessagesControllerApi = Feign.builder()
        .encoder(JacksonEncoder(mapper))
        .decoder(JacksonDecoder(mapper))
        .requestInterceptor(buildAuthorizationRequestInterceptor(httpHelper, tokenParams))
        .target(MessagesControllerApi::class.java, mshBaseUrl)

    private fun buildAuthorizationRequestInterceptor(httpHelper: DpopHttpRequestHelper, tokenParams: HelseIdTokenParameters?) =
        AuthorizationRequestInterceptor(httpHelper, tokenParams)

}

private class AuthorizationRequestInterceptor(
    val httpHelper: DpopHttpRequestHelper,
    val defaultTokenParams: HelseIdTokenParameters?,
) : RequestInterceptor {

    override fun apply(template: RequestTemplate) {
        val accessTokenRequestBuilder = buildBaseAccessTokenRequest(RequestContextHolder.get(), defaultTokenParams)
        val endpoint = Endpoint(
            method = HttpMethod.valueOf(template.method()),
            url = template.feignTarget().url() + template.path(),
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

    private fun buildBaseAccessTokenRequest(requestParams: RequestParameters?, defaultTokenParams: HelseIdTokenParameters?): AccessTokenRequestBuilder? {
        if (requestParams == null && defaultTokenParams == null) return null

        return AccessTokenRequestBuilder()
            .setTenantParams(requestParams?.helseId?.tenant ?: defaultTokenParams?.tenant)
    }

    private fun AccessTokenRequestBuilder.setTenantParams(tenantParams: HelseIdTenantParameters?) =
        apply {
            tenantParams?.let { tenant ->
                when (tenant) {
                    is SingleTenantHelseIdTokenParameters ->
                        tenancyType(TenancyType.SINGLE)
                            .childOrganizationNumber(tenant.childOrganization)

                    is MultiTenantHelseIdTokenParameters ->
                        tenancyType(TenancyType.MULTI)
                            .parentOrganizationNumber(tenant.parentOrganization)
                            .also { if (tenant.childOrganization != null) childOrganizationNumber(tenant.childOrganization) }
                }
            }
        }

}

