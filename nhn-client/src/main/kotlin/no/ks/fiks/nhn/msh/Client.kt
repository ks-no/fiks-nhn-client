package no.ks.fiks.nhn.msh

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import feign.Feign
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import no.ks.fiks.helseid.dpop.Endpoint
import no.ks.fiks.helseid.dpop.HttpMethod
import no.ks.fiks.helseid.http.HttpRequestHelper
import no.ks.fiks.nhn.ar.AdresseregisteretClient
import no.ks.fiks.nhn.edi.BusinessDocumentDeserializer
import no.ks.fiks.nhn.edi.BusinessDocumentSerializer
import no.ks.fiks.nhn.flr.FastlegeregisteretClient
import no.nhn.msh.v2.api.MessagesControllerApi
import no.nhn.msh.v2.model.AppRecStatus
import no.nhn.msh.v2.model.PostAppRecRequest
import no.nhn.msh.v2.model.PostMessageRequest
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*
import no.ks.fiks.helseid.Configuration as HelseIdConfiguration
import no.ks.fiks.nhn.ar.Credentials as ArCredentials
import no.ks.fiks.nhn.flr.Credentials as FlrCredentials
import no.nhn.msh.v2.model.Message as NhnMessage

private const val API_VERSION = "2"

private const val CONTENT_TYPE = "application/xml"
private const val CONTENT_TRANSFER_ENCODING = "base64"

// Meldingstjener, MSH (Message Service Handler)
class Client(
    configuration: Configuration,
) {

    private val baseUrl = configuration.environments.mshEnvironment.url

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

    private val flrClient = FastlegeregisteretClient(
        environment = configuration.environments.fastlegeregisteretEnvironment,
        credentials = configuration.fastlegeregisteret.let {
            FlrCredentials(
                username = it.username,
                password = it.password,
            )
        },
    )
    private val arClient = AdresseregisteretClient(
        environment = configuration.environments.adresseregisteretEnvironment,
        credentials = configuration.adresseregisteret.let {
            ArCredentials(
                username = it.username,
                password = it.password,
            )
        },
    )
    private val receiverBuilder = GpForPersonReceiverBuilder(flrClient, arClient)

    private val httpHelper = HttpRequestHelper(
        HelseIdConfiguration(
            environment = configuration.environments.helseIdEnvironment,
            clientId = configuration.helseId.clientId,
            jwk = configuration.helseId.jwk,
        )
    )
    private val meldingstjenerEnvironment = configuration.environments.mshEnvironment
    private val sourceSystem = configuration.sourceSystem

    fun sendMessageToGPForPerson(businessDocument: GPForPersonOutgoingBusinessDocument) {
        sendMessage(
            OutgoingBusinessDocument(
                id = businessDocument.id,
                sender = businessDocument.sender,
                receiver = receiverBuilder.buildGpForPersonReceiver(businessDocument.person),
                message = businessDocument.message,
                vedlegg = businessDocument.vedlegg,
                version = businessDocument.version,
            )
        )
    }

    fun sendMessage(businessDocument: OutgoingBusinessDocument) {
        buildClient(buildPostMessagesEndpoint())
            .postMessage(
                API_VERSION, sourceSystem, PostMessageRequest()
                    .contentType(CONTENT_TYPE)
                    .contentTransferEncoding(CONTENT_TRANSFER_ENCODING)
                    .businessDocument(Base64.getEncoder().encodeToString(BusinessDocumentSerializer.serializeNhnMessage(businessDocument).toByteArray()))
            )
    }

    fun getMessages(receiverHerId: Int): List<Message> {
        return buildClient(buildGetMessagesEndpoint())
            .getMessages(
                API_VERSION, sourceSystem, MessagesControllerApi.GetMessagesQueryParams()
                    .receiverHerIds(setOf(receiverHerId))
            )
            .map { it.toMessageInfo() }

    }

    fun getMessagesWithMetadata(receiverHerId: Int): List<MessageWithMetadata> {
        return buildClient(buildGetMessagesEndpoint())
            .getMessages(
                API_VERSION, sourceSystem, MessagesControllerApi.GetMessagesQueryParams()
                    .receiverHerIds(setOf(receiverHerId))
                    .includeMetadata(true)
            )
            .map { it.toMessageInfoWithMetadata() }

    }

    fun getMessage(id: UUID): MessageWithMetadata {
        return buildClient(buildGetMessageByIdEndpoint(id))
            .getMessage(id, API_VERSION, sourceSystem)
            .toMessageInfoWithMetadata()
    }

    fun getBusinessDocument(id: UUID): IncomingBusinessDocument {
        return buildClient(buildGetBusinessDocumentEndpoint(id))
            .getBusinessDocument(id, API_VERSION, sourceSystem)
            .let {
                BusinessDocumentDeserializer.deserializeMsgHead(String(Base64.getDecoder().decode(it.businessDocument)))
            }
    }

    fun getApplicationReceipt(id: UUID): ApplicationReceipt {
        return buildClient(buildGetBusinessDocumentEndpoint(id))
            .getBusinessDocument(id, API_VERSION, sourceSystem)
            .let {
                BusinessDocumentDeserializer.deserializeAppRec(String(Base64.getDecoder().decode(it.businessDocument)))
            }
    }

    fun sendApplicationReceipt(id: UUID, senderHerId: Int) {
        buildClient(buildPostAppRecEndpoint(id, senderHerId))
            .postAppRec(
                id, senderHerId, API_VERSION, sourceSystem, PostAppRecRequest()
                    .appRecStatus(AppRecStatus.OK) // TODO: Support different status and error messages
            )
    }

    fun markMessageRead(id: UUID, receiverHerId: Int) {
        buildClient(buildPutMessageReadEndpoint(id, receiverHerId))
            .markMessageAsRead(id, receiverHerId, API_VERSION, sourceSystem)
    }

    private fun NhnMessage.toMessageInfo() = Message(
        id = id,
        receiverHerId = receiverHerId,
    )

    private fun NhnMessage.toMessageInfoWithMetadata() = MessageWithMetadata(
        id = id,
        contentType = contentType,
        receiverHerId = receiverHerId,
        senderHerId = senderHerId,
        businessDocumentId = businessDocumentId,
        businessDocumentDate = businessDocumentGenDate,
        isAppRec = isAppRec,
    )

    private fun buildClient(endpoint: Endpoint) = Feign.builder()
        .encoder(JacksonEncoder(mapper))
        .decoder(JacksonDecoder(mapper))
        .requestInterceptor {
            httpHelper.addDpopAuthorizationHeader(endpoint) { name, value ->
                it.header(name, value)
            }
        }
        .target(MessagesControllerApi::class.java, meldingstjenerEnvironment.url)

    private fun buildGetMessagesEndpoint() = Endpoint(HttpMethod.GET, "$baseUrl/Messages")
    private fun buildPostMessagesEndpoint() = Endpoint(HttpMethod.POST, "$baseUrl/Messages")
    private fun buildGetMessageByIdEndpoint(id: UUID) = Endpoint(HttpMethod.GET, "$baseUrl/Messages/$id")
    private fun buildGetBusinessDocumentEndpoint(id: UUID) = Endpoint(HttpMethod.GET, "$baseUrl/Messages/$id/business-document")
    private fun buildPostAppRecEndpoint(id: UUID, senderHerId: Int) = Endpoint(HttpMethod.POST, "$baseUrl/Messages/$id/apprec/$senderHerId")
    private fun buildPutMessageReadEndpoint(id: UUID, receiverHerId: Int) = Endpoint(HttpMethod.PUT, "$baseUrl/Messages/$id/read/$receiverHerId")

}