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

class GetPatientGPHttpTest : StringSpec() {

    private val wireMock = WireMockContainer("wiremock/wiremock:3.12.1")
        .also { it.start() }
    private val wireMockClient = WireMock(wireMock.host, wireMock.port)

    init {
        "Test lookup for person with GP" {
            val patientId = UUID.randomUUID().toString()
            stubResponse(patientId, "get-patient-gp.xml")

            buildClient().getPatientGP(patientId)
                .asClue {
                    it shouldNot beNull()
                    it!!.patientId shouldBe "12345678999"
                    it.gpHerId shouldBe 4657988
                }
        }

        "Test lookup for person that does not have GP" {
            val patientId = UUID.randomUUID().toString()
            stubResponse(patientId, "get-patient-gp-not-found.xml")

            shouldThrow<FastlegeregisteretApiException> { buildClient().getPatientGP(patientId) }
                .asClue {
                    it.errorCode shouldBe "Feil"
                    it.faultMessage shouldBe "ArgumentException: Personen er ikke tilknyttet fastlegekontrakt"
                    it.message shouldBe "ArgumentException: Personen er ikke tilknyttet fastlegekontrakt (reason)"
                }
        }

        "Test lookup with invalid patientId" {
            val patientId = UUID.randomUUID().toString()
            stubResponse(patientId, "get-patient-gp-invalid-nin.xml")

            val client = FastlegeregisteretClient(
                FastlegeregisteretService(
                    url = wireMock.baseUrl,
                    credentials = Credentials(
                        username = UUID.randomUUID().toString(),
                        password = UUID.randomUUID().toString(),
                    ),
                )
            )

            shouldThrow<FastlegeregisteretApiException> { client.getPatientGP(patientId) }
                .asClue {
                    it.errorCode shouldBe "Feil"
                    it.faultMessage shouldBe "ArgumentException: patientNin er ugyldig"
                    it.message shouldBe "ArgumentException: patientNin er ugyldig (reason)"
                }
        }

        "Test that configuration is applied correctly" {
            val username = UUID.randomUUID().toString()
            val password = UUID.randomUUID().toString()

            val patientId = UUID.randomUUID().toString()
            stubResponse(patientId, "get-patient-gp.xml")

            buildClient(username, password)
                .getPatientGP(patientId)

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

    private fun buildClient(
        username: String = UUID.randomUUID().toString(),
        password: String = UUID.randomUUID().toString(),
    ) = FastlegeregisteretClient(
        FastlegeregisteretService(
            url = wireMock.baseUrl,
            credentials = Credentials(
                username = username,
                password = password,
            ),
        )
    )

    private fun stubResponse(patientId: String, fileName: String) {
        wireMockClient.register(
            post("/")
                .withRequestBody(containing(patientId))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/soap+xml; charset=utf-8")
                        .withBody(ClassLoader.getSystemResource(fileName).readBytes())
                )
        )
    }

}
