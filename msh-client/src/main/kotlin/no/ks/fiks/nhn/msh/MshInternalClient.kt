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
import io.ktor.client.statement.HttpResponse
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

private const val RECEIVER_HER_ID_PARAM = "receiverHerIds"
private const val INCLUDE_METADATA_PARAM = "includeMetadata"

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
    ): List<ApprecInfo> = client.getWithParams("$baseUrl/Messages/$id/apprec", requestParams).body()

    @JvmOverloads
    suspend fun getMessages(
        receiverHerId: Int,
        includeMetadata: Boolean = false,
        requestParams: RequestParameters? = null,
    ): List<Message> = client.getWithParams("$baseUrl/Messages", requestParams) {
        parameter(RECEIVER_HER_ID_PARAM, receiverHerId)
        parameter(INCLUDE_METADATA_PARAM, includeMetadata)
    }.body()

    @JvmOverloads
    suspend fun postMessage(
        request: PostMessageRequest,
        requestParams: RequestParameters? = null,
    ): UUID = UUID.fromString(client.postWithParams("$baseUrl/Messages", request, requestParams).bodyAsText())

    @JvmOverloads
    suspend fun getMessage(
        id: UUID,
        requestParams: RequestParameters? = null,
    ): Message = client.getWithParams("$baseUrl/Messages/$id", requestParams).body()

    @JvmOverloads
    suspend fun getBusinessDocument(
        id: UUID,
        requestParams: RequestParameters? = null,
    ): GetBusinessDocumentResponse = client.getWithParams("$baseUrl/Messages/$id/business-document", requestParams).body()

    @JvmOverloads
    suspend fun getStatus(
        id: UUID,
        requestParams: RequestParameters? = null,
    ): List<StatusInfo> = client.getWithParams("$baseUrl/Messages/$id/status", requestParams).body()

    @JvmOverloads
    suspend fun postAppRec(
        id: UUID,
        senderHerId: Int,
        request: PostAppRecRequest,
        requestParams: RequestParameters? = null,
    ): UUID = UUID.fromString(client.postWithParams("$baseUrl/Messages/$id/apprec/$senderHerId", request, requestParams).bodyAsText())

    @JvmOverloads
    suspend fun markMessageRead(
        id: UUID,
        senderHerId: Int,
        requestParams: RequestParameters? = null,
    ) {
        client.putWithParams("$baseUrl/Messages/$id/read/$senderHerId", requestParams)
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

    private suspend fun HttpClient.getWithParams(
        url: String,
        requestParams: RequestParameters? = null,
        customConfig: HttpRequestBuilder.() -> Unit = {}
    ): HttpResponse = get(url) {
        headers.set(Endpoint(HttpMethod.GET, url), requestParams)
        customConfig()
    }

    private suspend fun HttpClient.postWithParams(
        url: String,
        body: Any,
        requestParams: RequestParameters? = null,
        customConfig: HttpRequestBuilder.() -> Unit = {}
    ): HttpResponse = post(url) {
        headers.set(Endpoint(HttpMethod.POST, url), requestParams)
        contentType(ContentType.Application.Json)
        setBody(body)
        customConfig()
    }

    private suspend fun HttpClient.putWithParams(
        url: String,
        requestParams: RequestParameters? = null,
        customConfig: HttpRequestBuilder.() -> Unit = {}
    ): HttpResponse = post(url) {
        headers.set(Endpoint(HttpMethod.PUT, url), requestParams)
        contentType(ContentType.Application.Json)
        customConfig()
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
