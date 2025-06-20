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
import no.ks.fiks.helseid.TenancyType
import no.ks.fiks.helseid.dpop.Endpoint
import no.ks.fiks.helseid.dpop.HttpMethod
import no.ks.fiks.helseid.http.HttpRequestHelper
import no.nhn.msh.v2.api.MessagesControllerApi
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import no.ks.fiks.helseid.Configuration as HelseIdConfiguration

// Meldingstjener, MSH (Message Service Handler)
internal object MshFeignClientBuilder {

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

    fun build(configuration: Configuration): MessagesControllerApi = Feign.builder()
        .encoder(JacksonEncoder(mapper))
        .decoder(JacksonDecoder(mapper))
        .requestInterceptor(buildAuthorizationRequestInterceptor(configuration))
        .target(MessagesControllerApi::class.java, configuration.environments.mshBaseUrl)

    private fun buildAuthorizationRequestInterceptor(configuration: Configuration) = AuthorizationRequestInterceptor(
        HttpRequestHelper(
            HelseIdConfiguration(
                environment = configuration.environments.helseIdEnvironment,
                clientId = configuration.helseId.clientId,
                jwk = configuration.helseId.jwk,
            )
        ), buildBaseAccessTokenRequest(configuration)
    )

    private fun buildBaseAccessTokenRequest(configuration: Configuration) =
        configuration.helseId.tokenConfiguration.let { config ->
            when (config) {
                is SingleTenantHelseIdTokenConfiguration -> AccessTokenRequestBuilder()
                    .tenancyType(TenancyType.SINGLE)
                    .childOrganizationNumber(config.childOrganization)

                is MultiTenantHelseIdTokenConfiguration -> AccessTokenRequestBuilder()
                    .tenancyType(TenancyType.MULTI)
                    .parentOrganizationNumber(config.parentOrganization)
                    .also { if (config.childOrganization != null) it.childOrganizationNumber(config.childOrganization) }

                null -> null
            }
        }

}

private class AuthorizationRequestInterceptor(
    val httpHelper: HttpRequestHelper,
    val accessTokenRequestBuilder: AccessTokenRequestBuilder? = null,
) : RequestInterceptor {

    override fun apply(template: RequestTemplate) {
        val endpoint = Endpoint(
            method = HttpMethod.valueOf(template.method()),
            url = template.feignTarget().url() + template.path(),
        )
        if (accessTokenRequestBuilder == null) {
            httpHelper.addDpopAuthorizationHeader(endpoint) { name, value ->
                template.header(name, value)
            }
        } else {
            httpHelper.addDpopAuthorizationHeader(endpoint, accessTokenRequestBuilder) { name, value ->
                template.header(name, value)
            }
        }

    }

}
