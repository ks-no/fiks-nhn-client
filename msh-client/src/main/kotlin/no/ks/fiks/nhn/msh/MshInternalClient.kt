package no.ks.fiks.nhn.msh

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import mu.KotlinLogging
import no.ks.fiks.helseid.AccessTokenRequestBuilder
import no.ks.fiks.helseid.HelseIdClient
import no.ks.fiks.helseid.TenancyType
import no.ks.fiks.helseid.dpop.Endpoint
import no.ks.fiks.helseid.dpop.HttpMethod
import no.ks.fiks.helseid.dpop.ProofBuilder
import no.ks.fiks.helseid.http.DpopHttpRequestHelper
import no.nhn.msh.v2.model.*
import no.nhn.msh.v2.model.Message
import no.nhn.msh.v2.model.StatusInfo
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeParseException
import java.util.*

private const val API_VERSION_HEADER = "api-version"
private const val API_VERSION = "2"

private const val SOURCE_SYSTEM_HEADER = "nhn-source-system"

private const val RECEIVER_HER_ID_PARAM = "receiverHerIds"
private const val INCLUDE_METADATA_PARAM = "includeMetadata"

private val log = KotlinLogging.logger { }

class MshInternalClient(
    private val baseUrl: String,
    private val sourceSystem: String,
    private val defaultTokenParams: HelseIdTokenParameters? = null,
    private val client: HttpClient = buildDefaultClient(),
    helseIdClient: HelseIdClient,
    proofBuilder: ProofBuilder,
) {

    private val httpHelper = DpopHttpRequestHelper(helseIdClient, proofBuilder)

    suspend fun getAppRecInfo(
        id: UUID,
        requestParams: RequestParameters? = null,
    ): List<ApprecInfo> =
        invokeWithErrorHandling {
            client.getWithParams("$baseUrl/Messages/$id/apprec", requestParams)
        }.let {
            if (it.status != HttpStatusCode.OK) throw it.toHttpException(expectedStatus = HttpStatusCode.OK.value)
            it.body()
        }

    suspend fun getMessages(
        receiverHerId: Int,
        includeMetadata: Boolean = false,
        requestParams: RequestParameters? = null,
    ): List<Message> =
        invokeWithErrorHandling {
            client.getWithParams("$baseUrl/Messages", requestParams) {
                parameter(RECEIVER_HER_ID_PARAM, receiverHerId)
                parameter(INCLUDE_METADATA_PARAM, includeMetadata)
            }
        }.let {
            if (it.status != HttpStatusCode.OK) throw it.toHttpException(expectedStatus = HttpStatusCode.OK.value)
            it.body()
        }

    suspend fun postMessage(
        request: PostMessageRequest,
        requestParams: RequestParameters? = null,
    ): UUID =
        invokeWithErrorHandling {
            client.postWithParams("$baseUrl/Messages", request, requestParams)
        }.let {
            if (it.status != HttpStatusCode.Created) throw it.toHttpException(expectedStatus = HttpStatusCode.Created.value)
            it.bodyAsText().toUuid()
        }

    suspend fun getMessage(
        id: UUID,
        requestParams: RequestParameters? = null,
    ): Message =
        invokeWithErrorHandling {
            client.getWithParams("$baseUrl/Messages/$id", requestParams)
        }.let {
            if (it.status != HttpStatusCode.OK) throw it.toHttpException(expectedStatus = HttpStatusCode.OK.value)
            it.body()
        }

    suspend fun getBusinessDocument(
        id: UUID,
        requestParams: RequestParameters? = null,
    ): GetBusinessDocumentResponse =
        invokeWithErrorHandling {
            client.getWithParams("$baseUrl/Messages/$id/business-document", requestParams)
        }.let {
            if (it.status != HttpStatusCode.OK) throw it.toHttpException(expectedStatus = HttpStatusCode.OK.value)
            it.body()
        }

    suspend fun getStatus(
        id: UUID,
        requestParams: RequestParameters? = null,
    ): List<StatusInfo> =
        invokeWithErrorHandling {
            client.getWithParams("$baseUrl/Messages/$id/status", requestParams)
        }.let {
            if (it.status != HttpStatusCode.OK) throw it.toHttpException(expectedStatus = HttpStatusCode.OK.value)
            it.body()
        }

    suspend fun postAppRec(
        id: UUID,
        senderHerId: Int,
        request: PostAppRecRequest,
        requestParams: RequestParameters? = null,
    ): UUID =
        invokeWithErrorHandling {
            client.postWithParams("$baseUrl/Messages/$id/apprec/$senderHerId", request, requestParams)
        }.let {
            if (it.status != HttpStatusCode.Created) throw it.toHttpException(expectedStatus = HttpStatusCode.Created.value)
            it.bodyAsText().toUuid()
        }

    suspend fun markMessageRead(
        id: UUID,
        senderHerId: Int,
        requestParams: RequestParameters? = null,
    ) {
        invokeWithErrorHandling {
            client.putWithParams("$baseUrl/Messages/$id/read/$senderHerId", requestParams)
        }.also {
            if (it.status != HttpStatusCode.NoContent) throw it.toHttpException(expectedStatus = HttpStatusCode.NoContent.value)
        }
    }

    private suspend fun <T> invokeWithErrorHandling(operation: suspend () -> T): T =
        try {
            operation.invoke()
        } catch (e: Exception) {
            throw MshException("Unknown error from MSH API", e)
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
    ): HttpResponse = put(url) {
        headers.set(Endpoint(HttpMethod.PUT, url), requestParams)
        contentType(ContentType.Application.Json)
        customConfig()
    }

    // Replaces leading and/or trailing quote
    private fun String.toUuid() = UUID.fromString(replace(Regex("^\"|\"$"), ""))

    private suspend fun HttpResponse.toHttpException(expectedStatus: Int) =
        when (status.value) {
            in (300 until 400) -> HttpRedirectException(status = status.value, body = bodyAsText())
            in (400 until 500) -> HttpClientException(status = status.value, body = bodyAsText())
            in (500 until 600) -> HttpServerException(status = status.value, body = bodyAsText())
            else -> HttpException(status = status.value, expectedStatus = expectedStatus, body = bodyAsText())
        }

}

private fun buildDefaultClient() =
    HttpClient(CIO) {
        install(ContentNegotiation) {
            jackson {
                disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                registerModule(JavaTimeModule())
                registerModule(SimpleModule().apply {
                    addDeserializer(OffsetDateTime::class.java, object : JsonDeserializer<OffsetDateTime?>() {
                        override fun deserialize(
                            parser: JsonParser,
                            context: DeserializationContext
                        ): OffsetDateTime? = parser.text.tryParseOffsetDateTime()
                    })
                })
            }
        }
    }

private fun String.tryParseOffsetDateTime()  = try {
    OffsetDateTime.parse(this)
} catch (_: DateTimeParseException) {
    tryParseOffsetDateTimeWithoutZone()
}

private fun String.tryParseOffsetDateTimeWithoutZone() = try {
    val local = LocalDateTime.parse(this)
    OffsetDateTime.of(local, ZoneOffset.UTC)
} catch (_: DateTimeParseException) {
    log.error { "Unable to parse date: $this" }
    null
}

