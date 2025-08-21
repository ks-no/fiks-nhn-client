package no.ks.fiks.nhn.msh

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import no.ks.fiks.helseid.AccessTokenRequestBuilder
import no.ks.fiks.helseid.HelseIdClient
import no.ks.fiks.helseid.TenancyType
import no.ks.fiks.helseid.dpop.Endpoint
import no.ks.fiks.helseid.dpop.HttpMethod
import no.ks.fiks.helseid.dpop.ProofBuilder
import no.ks.fiks.helseid.http.DpopHttpRequestHelper
import no.nhn.msh.v2.model.*
import no.nhn.msh.v2.model.Message
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

private const val API_VERSION_HEADER = "api-version"
private const val API_VERSION = "2"

private const val SOURCE_SYSTEM_HEADER = "nhn-source-system"

class MshInternalClient(
    private val baseUrl: String,
    private val sourceSystem: String,
    private val defaultTokenParams: HelseIdTokenParameters? = null,
    private val client: HttpClient = buildDefaultClient(),
    helseIdClient: HelseIdClient,
    proofBuilder: ProofBuilder,
) {

    private val httpHelper = DpopHttpRequestHelper(helseIdClient, proofBuilder)

    @JvmOverloads
    suspend fun getAppRecInfo(
        id: UUID,
        requestParams: RequestParameters? = null,
    ): List<ApprecInfo> {
        val endpoint = Endpoint(HttpMethod.GET, "$baseUrl/Messages/$id/apprec")
        return client.get(endpoint.url) {
            headers.set(endpoint, requestParams)
        }.body()
    }

    @JvmOverloads
    suspend fun getMessages(
        receiverHerId: Int,
        includeMetadata: Boolean = false,
        requestParams: RequestParameters? = null,
    ): List<Message> {
        val endpoint = Endpoint(HttpMethod.GET, "$baseUrl/Messages")
        return client.get(endpoint.url) {
            headers.set(endpoint, requestParams)
            parameter("receiverHerIds", receiverHerId)
        }.body()
    }

    @JvmOverloads
    suspend fun postMessage(
        request: PostMessageRequest,
        requestParams: RequestParameters? = null,
    ): UUID {
        val endpoint = Endpoint(HttpMethod.POST, "$baseUrl/Messages")
        return UUID.fromString(
            client.post(endpoint.url) {
                headers.set(endpoint, requestParams)
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        )
    }

    @JvmOverloads
    suspend fun getMessage(
        id: UUID,
        requestParams: RequestParameters? = null,
    ): Message {
        val endpoint = Endpoint(HttpMethod.GET, "$baseUrl/Messages/$id")
        return client.get(endpoint.url) {
            headers.set(endpoint, requestParams)
        }.body()
    }

    @JvmOverloads
    suspend fun getBusinessDocument(
        id: UUID,
        requestParams: RequestParameters? = null,
    ): GetBusinessDocumentResponse {
        val endpoint = Endpoint(HttpMethod.GET, "$baseUrl/Messages/$id/business-document")
        return client.get(endpoint.url) {
            headers.set(endpoint, requestParams)
        }.body()
    }

    @JvmOverloads
    suspend fun getStatus(
        id: UUID,
        requestParams: RequestParameters? = null,
    ): List<StatusInfo> {
        val endpoint = Endpoint(HttpMethod.GET, "$baseUrl/Messages/$id/status")
        return client.get(endpoint.url) {
            headers.set(endpoint, requestParams)
        }.body()
    }

    @JvmOverloads
    suspend fun postAppRec(
        id: UUID,
        senderHerId: Int,
        request: PostAppRecRequest,
        requestParams: RequestParameters? = null,
    ): UUID {
        val endpoint = Endpoint(HttpMethod.POST, "$baseUrl/Messages/$id/apprec/$senderHerId")
        return UUID.fromString(
            client.post(endpoint.url) {
                headers.set(endpoint, requestParams)
                contentType(ContentType.Application.Json)
                accept(ContentType.Text.Plain)
                setBody(request)
            }.bodyAsText()
        )
    }

    @JvmOverloads
    suspend fun markMessageRead(
        id: UUID,
        senderHerId: Int,
        requestParams: RequestParameters? = null,
    ) {
        val endpoint = Endpoint(HttpMethod.PUT, "$baseUrl/Messages/$id/read/$senderHerId")
        client.put(endpoint.url) {
            headers.set(endpoint, requestParams)
        }
    }

    private fun HeadersBuilder.set(endpoint: Endpoint, requestParams: RequestParameters?) {
        append(API_VERSION_HEADER, API_VERSION)
        append(SOURCE_SYSTEM_HEADER, sourceSystem)
        appendHelseId(endpoint, requestParams)
    }

    private fun HeadersBuilder.appendHelseId(endpoint: Endpoint, requestParams: RequestParameters?) {
        val accessTokenRequestBuilder = buildBaseAccessTokenRequest(requestParams, defaultTokenParams)

        if (accessTokenRequestBuilder == null) {
            httpHelper.addAuthorizationHeader(endpoint) { name, value ->
                append(name, value)
            }
        } else {
            httpHelper.addAuthorizationHeader(endpoint, accessTokenRequestBuilder) { name, value ->
                append(name, value)
            }
        }
    }

    private fun buildBaseAccessTokenRequest(
        requestParams: RequestParameters?,
        defaultTokenParams: HelseIdTokenParameters?
    ): AccessTokenRequestBuilder? {
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

private fun buildDefaultClient() =
    HttpClient(CIO) {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
                registerModule(SimpleModule().apply {
                    // API returns ISO 8601 dates without offset that can't be parsed by the default deserializer
                    addDeserializer(OffsetDateTime::class.java, object : JsonDeserializer<OffsetDateTime>() {
                        override fun deserialize(
                            parser: JsonParser,
                            context: DeserializationContext
                        ): OffsetDateTime {
                            val local = LocalDateTime.parse(parser.text)
                            return OffsetDateTime.of(local, ZoneOffset.UTC)
                        }
                    })
                })
            }
        }
    }
