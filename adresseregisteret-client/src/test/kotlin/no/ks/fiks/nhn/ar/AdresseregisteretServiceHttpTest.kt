package no.ks.fiks.nhn.ar

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.*
import io.kotest.assertions.asClue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.beNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import io.kotest.matchers.types.shouldBeInstanceOf
import org.wiremock.integrations.testcontainers.WireMockContainer
import java.nio.charset.Charset
import java.util.*
import kotlin.random.Random.Default.nextInt

class AdresseregisteretServiceHttpTest : StringSpec() {

    private val wireMock = WireMockContainer("wiremock/wiremock:3.12.1")
        .also { it.start() }
    private val wireMockClient = WireMock(wireMock.host, wireMock.port)

    init {

        "Test that configuration is applied correctly" {
            val username = UUID.randomUUID().toString()
            val password = UUID.randomUUID().toString()
            val url = wireMock.baseUrl

            val herId = nextInt(1, 100000)
            wireMockClient.register(
                post("/")
                    .withRequestBody(containing("<herId>$herId</herId>"))
                    .willReturn(
                        aResponse()
                            .withHeader("Content-Type", "application/soap+xml; charset=utf-8")
                            .withBody(ClassLoader.getSystemResource("get-communication-party-details-no-parent-response.xml").readBytes())
                    )
            )

            AdresseregisteretService(url, Credentials(username, password))
                .getCommunicationPartyDetails(herId)

            val requests = wireMockClient.find(
                postRequestedFor(urlEqualTo("/"))
                    .withRequestBody(containing("<herId>$herId</herId>"))
            )
            requests shouldHaveSize 1
            requests.single().asClue { request ->
                request.absoluteUrl shouldBe "${url}/"
                val usernamePassword = Base64.getDecoder()
                    .decode(
                        request.header("Authorization").firstValue()
                            .removePrefix("Basic ")
                    )
                    .toString(Charset.defaultCharset())
                usernamePassword shouldBe "$username:$password"
            }
        }

    }

}
