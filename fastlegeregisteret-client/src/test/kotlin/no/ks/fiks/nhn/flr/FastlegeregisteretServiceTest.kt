package no.ks.fiks.nhn.flr

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.*
import io.kotest.assertions.asClue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.beNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import org.wiremock.integrations.testcontainers.WireMockContainer
import java.nio.charset.Charset
import java.util.*

class FastlegeregisteretServiceTest : StringSpec() {

    private val wireMock = WireMockContainer("wiremock/wiremock:3.12.1")
        .also { it.start() }
    private val wireMockClient = WireMock(wireMock.host, wireMock.port)

    init {
        "Test that configuration is applied correctly" {
            val username = UUID.randomUUID().toString()
            val password = UUID.randomUUID().toString()

            val patientId = UUID.randomUUID().toString()
            wireMockClient.register(
                post("/")
                    .withRequestBody(containing(patientId))
                    .willReturn(
                        aResponse()
                            .withHeader("Content-Type", "application/soap+xml; charset=utf-8")
                            .withBody(ClassLoader.getSystemResource("get-patient-gp.xml").readBytes())
                    )
            )

            FastlegeregisteretService(
                url = wireMock.baseUrl,
                credentials = Credentials(
                    username = username,
                    password = password,
                ),
            ).getPatientGPDetails(patientId)

            val requests = wireMockClient.find(
                postRequestedFor(urlEqualTo("/"))
                    .withRequestBody(containing(patientId))
            )
            requests shouldHaveSize 1
            requests.single().asClue { request ->
                request.absoluteUrl shouldBe "${wireMock.baseUrl}/"
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
