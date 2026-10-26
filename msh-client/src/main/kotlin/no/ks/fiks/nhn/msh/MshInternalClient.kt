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
import no.ks.fiks.nhn.parseOffsetDateTimeOrNull
import no.nhn.msh.v3.model.GetBusinessDocumentResponse as NhnGetBusinessDocumentResponse
import no.nhn.msh.v3.model.GetMessageResponse as NhnGetMessageResponse
import no.nhn.msh.v3.model.GetStatusResponse as NhnGetStatusResponse
import no.nhn.msh.v3.model.GetUnreadNotificationsResponse as NhnGetUnreadNotificationsResponse
import no.nhn.msh.v3.model.MarkAsDownloadedRequest as NhnMarkAsDownloadedRequest
import no.nhn.msh.v3.model.MshConfiguration as NhnMshConfiguration
import no.nhn.msh.v3.model.PostAppRecRequest as NhnPostAppRecRequest
import no.nhn.msh.v3.model.PostApprecResponse as NhnPostApprecResponse
import no.nhn.msh.v3.model.PostMessageRequest as NhnPostMessageRequest
import no.nhn.msh.v3.model.PostMessageResponse as NhnPostMessageResponse
import no.nhn.msh.v3.model.SetMshConfigurationsRequest as NhnSetMshConfigurationsRequest
import no.nhn.msh.v3.model.StatusInfo as NhnStatusInfo
import no.nhn.msh.v3.model.UnreadNotification as NhnUnreadNotification
import java.time.OffsetDateTime
import java.util.UUID

private const val API_VERSION_HEADER = "api-version"
private const val API_VERSION_NHN = "3"

private const val SOURCE_SYSTEM_HEADER = "nhn-source-system"
private const val HER_IDS_PARAM = "HerIds"
private const val NOTIFICATIONS_TO_FETCH_PARAM = "NotificationsToFetch"

private val log = KotlinLogging.logger { }

class MshInternalClient @JvmOverloads constructor(
    private val baseUrl: String,
    private val sourceSystem: String,
    private val defaultTokenParams: HelseIdTokenParameters? = null,
    private val client: HttpClient = buildDefaultClient(),
    helseIdClient: HelseIdClient,
    proofBuilder: ProofBuilder,
) {

    private val httpHelper = DpopHttpRequestHelper(helseIdClient, proofBuilder)

    suspend fun postMessage(
        request: NhnPostMessageRequest,
        requestParams: RequestParameters? = null,
    ): UUID =
        invokeWithErrorHandling {
            client.postWithParams("$baseUrl/messages", request, requestParams)
        }.let { response ->
            if (response.status != HttpStatusCode.Accepted) throw response.toHttpException(expectedStatus = HttpStatusCode.Accepted.value)
            response.body<NhnPostMessageResponse>().id?.toUuid() ?: throw IllegalArgumentException("PostMessageResponse is missing id field")
        }

    suspend fun getMessage(
        id: UUID,
        requestParams: RequestParameters? = null,
    ): NhnGetMessageResponse =
        invokeWithErrorHandling {
            client.getWithParams("$baseUrl/messages/$id", requestParams)
        }.let {
            if (it.status != HttpStatusCode.OK) throw it.toHttpException(expectedStatus = HttpStatusCode.OK.value)
            it.body()
        }

    suspend fun getBusinessDocument(
        id: UUID,
        requestParams: RequestParameters? = null,
    ): NhnGetBusinessDocumentResponse =
        invokeWithErrorHandling {
            client.getWithParams("$baseUrl/messages/$id/business-document", requestParams)
        }.let {
            if (it.status != HttpStatusCode.OK) throw it.toHttpException(expectedStatus = HttpStatusCode.OK.value)
            it.body()
        }

    suspend fun getStatus(
        id: UUID,
        requestParams: RequestParameters? = null,
    ): List<NhnStatusInfo> =
        invokeWithErrorHandling {
            client.getWithParams("$baseUrl/messages/$id/status", requestParams)
        }.let { response ->
            if (response.status != HttpStatusCode.OK) throw response.toHttpException(expectedStatus = HttpStatusCode.OK.value)
            response.body<NhnGetStatusResponse>().statusList ?: emptyList()
        }

    suspend fun postAppRec(
        id: UUID,
        request: NhnPostAppRecRequest,
        requestParams: RequestParameters? = null,
    ): UUID {
        return invokeWithErrorHandling {
            client.postWithParams("$baseUrl/messages/$id/apprec", request, requestParams)
        }.let {
            if (it.status != HttpStatusCode.Accepted) throw it.toHttpException(expectedStatus = HttpStatusCode.Accepted.value)
            it.body<NhnPostApprecResponse>().id?.toUuid() ?: throw IllegalArgumentException("PostApprecResponse is missing id field")
        }
    }

    suspend fun markMessageDownloaded(
        id: UUID,
        receiverHerId: Int,
        requestParams: RequestParameters? = null,
    ) {
        invokeWithErrorHandling {
            client.putWithParams(
                "$baseUrl/messages/$id/downloaded",
                NhnMarkAsDownloadedRequest().receiverHerId(receiverHerId),
                requestParams,
            )
        }.also {
            if (it.status != HttpStatusCode.NoContent) throw it.toHttpException(expectedStatus = HttpStatusCode.NoContent.value)
        }
    }


    suspend fun getUnreadNotifications(
        herIds: Collection<Int>,
        notificationsToFetch: Int? = null,
        requestParams: RequestParameters? = null,
    ): List<NhnUnreadNotification> =
        invokeWithErrorHandling {
            client.getWithParams("$baseUrl/notifications/unread", requestParams) {
                herIds.forEach { herId -> parameter(HER_IDS_PARAM, herId) }
                notificationsToFetch?.let { parameter(NOTIFICATIONS_TO_FETCH_PARAM, it) }
            }
        }.let {
            if (it.status != HttpStatusCode.OK) throw it.toHttpException(expectedStatus = HttpStatusCode.OK.value)
            it.body<NhnGetUnreadNotificationsResponse>().unreadNotifications
        }

    suspend fun deleteNotifications(
        notificationIds: Collection<UUID>,
        requestParams: RequestParameters? = null,
    ) {
        invokeWithErrorHandling {
            client.postWithParams(
                "$baseUrl/notifications/delete",
                no.nhn.msh.v3.model.DeleteNotificationsRequest().also { req ->
                    notificationIds.forEach { req.addNotificationIdsItem(it.toString()) }
                },
                requestParams,
            )
        }.also {
            if (it.status != HttpStatusCode.NoContent) throw it.toHttpException(expectedStatus = HttpStatusCode.NoContent.value)
        }
    }

    suspend fun setMshConfigurations(
        configurations: List<NhnMshConfiguration>,
        requestParams: RequestParameters? = null,
    ) {
        invokeWithErrorHandling {
            client.putWithParams(
                "$baseUrl/mshconfigurations",
                NhnSetMshConfigurationsRequest().also { req ->
                    configurations.forEach { req.addConfigurationsItem(it) }
                },
                requestParams,
            )
        }.also {
            if (it.status != HttpStatusCode.NoContent) throw it.toHttpException(expectedStatus = HttpStatusCode.NoContent.value)
        }
    }

    suspend fun deleteMshConfigurations(
        herIds: Collection<Int>,
        requestParams: RequestParameters? = null,
    ) {
        invokeWithErrorHandling {
            client.deleteWithParams("$baseUrl/mshconfigurations", requestParams) {
                herIds.forEach { herId ->
                    parameter(HER_IDS_PARAM, herId)
                }
            }
        }.also {
            if (it.status != HttpStatusCode.OK && it.status != HttpStatusCode.NoContent) {
                throw it.toHttpException(expectedStatus = HttpStatusCode.NoContent.value)
            }
        }
    }

    private suspend fun <T> invokeWithErrorHandling(operation: suspend () -> T): T =
        try {
            operation.invoke()
        } catch (e: Exception) {
            throw MshException("Unknown error from MSH API", e)
        }

    private fun HeadersBuilder.set(endpoint: Endpoint, requestParams: RequestParameters?) {
        append(API_VERSION_HEADER, API_VERSION_NHN)
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
        body: Any,
        requestParams: RequestParameters? = null,
        customConfig: HttpRequestBuilder.() -> Unit = {}
    ): HttpResponse = put(url) {
        headers.set(Endpoint(HttpMethod.PUT, url), requestParams)
        contentType(ContentType.Application.Json)
        setBody(body)
        customConfig()
    }

    private suspend fun HttpClient.deleteWithParams(
        url: String,
        requestParams: RequestParameters? = null,
        customConfig: HttpRequestBuilder.() -> Unit = {}
    ): HttpResponse = delete(url) {
        headers.set(Endpoint(HttpMethod.DELETE, url), requestParams)
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
                        ): OffsetDateTime? = parser.text.parseOffsetDateTimeOrNull()
                    })
                })
            }
        }
    }
