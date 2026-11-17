package no.ks.fiks.nhn.msh

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.stubbing.Scenario
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import no.ks.fiks.hdir.StatusForMottakAvMelding
import no.ks.fiks.helseid.Environment
import no.nhn.msh.v3.model.PostApprecResponse
import org.wiremock.integrations.testcontainers.WireMockContainer
import java.util.UUID

class ClientFactoryApplicationMetadataTest : StringSpec() {

    private val wireMock = WireMockContainer("wiremock/wiremock:3.12.1")
        .also { it.start() }
    private val wireMockClient = WireMock(wireMock.host, wireMock.port)
    private val objectMapper = ObjectMapper()

    private fun Any.toJson(): String = objectMapper.writeValueAsString(this)

    init {
        "Application metadata name and version must not be blank" {
            shouldThrow<IllegalArgumentException> {
                ApplicationMetadata("", "1.0")
            }.message shouldBe "Application name must not be blank"

            shouldThrow<IllegalArgumentException> {
                ApplicationMetadata("application", "  ")
            }.message shouldBe "Application version must not be blank"
        }

        "Both factory clients use the same application metadata in the header and receipt body" {
            val baseUrl = wireMock.baseUrl
            wireMockClient.register(
                get(urlEqualTo("/.well-known/openid-configuration"))
                    .willReturn(
                        okJson(mapOf(
                            "issuer" to baseUrl,
                            "authorization_endpoint" to "$baseUrl/connect/authorize",
                            "token_endpoint" to "$baseUrl/connect/token",
                            "jwks_uri" to "$baseUrl/.well-known/jwks",
                            "response_types_supported" to listOf("code"),
                            "subject_types_supported" to listOf("public"),
                            "id_token_signing_alg_values_supported" to listOf("RS256"),
                        ).toJson())
                    )
            )
            listOf(false, true).forEach { withFastlegeLookup ->
                val clientId = UUID.randomUUID().toString()
                val messageId = UUID.randomUUID().toString()
                val receiptId = UUID.randomUUID().toString()
                val metadata = ApplicationMetadata("example-app-$clientId", "4.5.6")
                mockTokenExchange(clientId)
                wireMockClient.register(
                    post(urlEqualTo("/messages/$messageId/apprec"))
                        .willReturn(
                            status(202)
                                .withHeader("Content-Type", "application/json")
                                .withBody(PostApprecResponse().id(receiptId).toJson())
                        )
                )
                val helseId = HelseIdConfiguration(
                    environment = Environment(issuer = baseUrl, audience = "audience"),
                    clientId = clientId,
                    jwk = RSAKeyGenerator(2048).generate().toRSAKey().toString(),
                )
                val client = if (withFastlegeLookup) {
                    ClientFactory.createClientWithFastlegeLookup(
                        ConfigurationWithFastlegeLookup(
                            helseId = helseId,
                            adresseregister = AdresseregisterConfiguration(baseUrl),
                            fastlegeregister = FastlegeregisterConfiguration(baseUrl, Credentials("user", "password")),
                            mshBaseUrl = baseUrl,
                            applicationMetadata = metadata,
                        )
                    )
                } else {
                    ClientFactory.createClient(Configuration(helseId, baseUrl, metadata))
                }

                client.sendApplicationReceipt(
                    OutgoingApplicationReceipt(messageId, 123, StatusForMottakAvMelding.OK, emptyList())
                ) shouldBe receiptId
                wireMockClient.verifyThat(
                    1,
                    postRequestedFor(urlEqualTo("/messages/$messageId/apprec"))
                        .withHeader("nhn-source-system", equalTo("${metadata.applicationName} ${metadata.applicationVersion}"))
                        .withRequestBody(matchingJsonPath("$.applicationName", equalTo(metadata.applicationName)))
                        .withRequestBody(matchingJsonPath("$.applicationVersion", equalTo(metadata.applicationVersion)))
                )
            }
        }
    }

    private fun mockTokenExchange(clientId: String) {
        wireMockClient.register(
            post(urlEqualTo("/connect/token"))
                .withRequestBody(containing("client_id=$clientId"))
                .inScenario("DPoP token exchange $clientId")
                .whenScenarioStateIs(Scenario.STARTED)
                .willSetStateTo("Nonce issued")
                .willReturn(
                    status(400)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("DPoP-Nonce", "token-nonce")
                        .withBody(mapOf("error" to "use_dpop_nonce").toJson())
                )
        )
        wireMockClient.register(
            post(urlEqualTo("/connect/token"))
                .withRequestBody(containing("client_id=$clientId"))
                .inScenario("DPoP token exchange $clientId")
                .whenScenarioStateIs("Nonce issued")
                .willReturn(
                    okJson(mapOf(
                        "access_token" to "access-token",
                        "expires_in" to 3600,
                        "token_type" to "DPoP",
                        "scope" to "scope",
                    ).toJson())
                )
        )
    }
}
