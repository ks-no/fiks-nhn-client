package no.ks.fiks.nhn.msh

import feign.Feign
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import no.ks.fiks.helseid.dpop.Endpoint
import no.ks.fiks.helseid.dpop.HttpMethod
import no.ks.fiks.helseid.http.HttpRequestHelper
import no.ks.fiks.nhn.ar.AdresseregisteretClient
import no.ks.fiks.nhn.flr.FastlegeregisteretClient
import no.nhn.msh.v2.api.MessagesControllerApi
import no.nhn.msh.v2.model.PostMessageRequest
import java.util.*
import no.ks.fiks.helseid.Configuration as HelseIdConfiguration

private const val API_VERSION = "2"

private const val CONTENT_TYPE = "application/xml"
private const val CONTENT_TRANSFER_ENCODING = "base64"

private val messagesEndpoint = Endpoint(HttpMethod.POST, "https://api.tjener.test.melding.nhn.no/Messages")

// Meldingstjener, MSH (Message Service Handler)
class Client(
    configuration: Configuration,
) {

    private val flrClient = FastlegeregisteretClient(
        environment = configuration.environments.fastlegeregisteretEnvironment,
        credentials = configuration.getFastlegeregisteretCredentials(),
    )
    private val arClient = AdresseregisteretClient(
        environment = configuration.environments.adresseregisteretEnvironment,
        credentials = configuration.getAdresseregisteretCredentials(),
    )
    private val receiverBuilder = FastlegeForPersonReceiverBuilder(flrClient, arClient)

    private val httpHelper = HttpRequestHelper(
        HelseIdConfiguration(
            environment = configuration.environments.helseIdEnvironment,
            clientId = configuration.clientId,
            jwk = configuration.jwk,
            jwtRequestExpirationTime = configuration.jwtRequestExpirationTime,
        )
    )
    private val meldingstjenerEnvironment = configuration.environments.mshEnvironment
    private val sourceSystem = configuration.sourceSystem

    fun sendMessageToFastlegeForPerson(message: FastlegeForPersonMessage) {
        sendMessage(
            Message(
                type = message.type,
                sender = message.sender,
                receiver = receiverBuilder.buildFastlegeForPersonReceiver(message.personFnr),
                vedlegg = message.vedlegg,
            )
        )
    }

    fun sendMessage(message: Message) {
        buildClient(messagesEndpoint)
            .postMessage(
                API_VERSION, sourceSystem, PostMessageRequest()
                .contentType(CONTENT_TYPE)
                .contentTransferEncoding(CONTENT_TRANSFER_ENCODING)
                .businessDocument(Base64.getEncoder().encodeToString(MessageBuilder.buildNhnMessage(message).toByteArray())))
    }

    private fun buildClient(endpoint: Endpoint) = Feign.builder()
        .encoder(JacksonEncoder())
        .decoder(JacksonDecoder())
        .requestInterceptor {
            httpHelper.addDpopAuthorizationHeader(endpoint) { name, value ->
                it.header(name, value)
            }
        }
        .target(MessagesControllerApi::class.java, meldingstjenerEnvironment.url)

}