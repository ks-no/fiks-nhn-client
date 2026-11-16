package no.ks.fiks.nhn.msh

import com.fasterxml.jackson.databind.ObjectMapper
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.sun.net.httpserver.HttpServer
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import no.ks.fiks.hdir.StatusForMottakAvMelding
import no.ks.fiks.helseid.Environment
import no.ks.fiks.helseid.HelseIdClient
import no.ks.fiks.helseid.TokenResponse
import java.net.InetSocketAddress
import java.util.concurrent.LinkedBlockingQueue

class ClientFactoryApplicationMetadataTest : StringSpec({
    "Both factory clients use the same application metadata in the header and receipt body" {
        val requests = LinkedBlockingQueue<Pair<String?, String>>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/messages") { exchange ->
            val header = exchange.requestHeaders.getFirst("nhn-source-system")
            val body = exchange.requestBody.bufferedReader().use { it.readText() }
            requests.add(header to body)
            val response = """{"id":"receipt-id"}""".toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(202, response.size.toLong())
            exchange.responseBody.use { it.write(response) }
        }
        server.start()
        mockkConstructor(HelseIdClient::class)
        try {
            every { anyConstructed<HelseIdClient>().getAccessToken(any()) } returns
                TokenResponse("access-token", 0, "scope", "DPoP")
            val baseUrl = "http://127.0.0.1:${server.address.port}"
            val helseId = HelseIdConfiguration(
                environment = Environment(issuer = baseUrl, audience = "audience"),
                clientId = "client-id",
                jwk = RSAKeyGenerator(2048).generate().toRSAKey().toString(),
            )
            val metadata = ApplicationMetadata("example-app", "4.5.6")
            val clients = listOf(
                ClientFactory.createClient(Configuration(helseId, baseUrl, metadata)),
                ClientFactory.createClientWithFastlegeLookup(
                    ConfigurationWithFastlegeLookup(
                        helseId = helseId,
                        adresseregister = AdresseregisterConfiguration(baseUrl),
                        fastlegeregister = FastlegeregisterConfiguration(baseUrl, Credentials("user", "password")),
                        mshBaseUrl = baseUrl,
                        applicationMetadata = metadata,
                    )
                ),
            )
            val mapper = ObjectMapper()
            clients.forEach { client ->
                client.sendApplicationReceipt(
                    OutgoingApplicationReceipt("message-id", 123, StatusForMottakAvMelding.OK, emptyList())
                ) shouldBe "receipt-id"
                val (header, body) = requests.remove()
                header shouldBe "example-app 4.5.6"
                val json = mapper.readTree(body)
                json["applicationName"].asText() shouldBe metadata.applicationName
                json["applicationVersion"].asText() shouldBe metadata.applicationVersion
            }
        } finally {
            unmockkConstructor(HelseIdClient::class)
            server.stop(0)
        }
    }
})
