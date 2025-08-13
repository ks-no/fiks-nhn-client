package no.ks.fiks.nhn.msh

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.*
import io.kotest.assertions.asClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifySequence
import no.ks.fiks.helseid.*
import no.ks.fiks.helseid.dpop.Endpoint
import no.ks.fiks.helseid.dpop.HttpMethod
import no.ks.fiks.helseid.dpop.ProofBuilder
import no.nhn.msh.v2.model.PostAppRecRequest
import org.wiremock.integrations.testcontainers.WireMockContainer
import java.util.*
import kotlin.random.Random.Default.nextInt

class FeignApiBuilderTest : StringSpec() {


    private val wireMock = WireMockContainer("wiremock/wiremock:3.12.1")
        .also { it.start() }
    private val wireMockClient = WireMock(wireMock.host, wireMock.port)

    init {

        "If no token params are passed, should get token with standard token request" {
            val id = UUID.randomUUID()

            wireMockClient.register(get("/Messages/$id"))

            val helseIdClient = mockk<HelseIdClient> { every { getAccessToken(any()) } returns buildTokenResponse("") }
            FeignApiFactory.createApi(
                mshBaseUrl = wireMock.baseUrl,
                helseIdClient = helseIdClient,
                proofBuilder = mockk { every { buildProof(any(), any(), any()) } returns UUID.randomUUID().toString() },
                tokenParams = null,
            ).getMessage(id, UUID.randomUUID().toString(), UUID.randomUUID().toString())

            verifySequence {
                helseIdClient.getAccessToken(StandardAccessTokenRequest(TokenType.DPOP))
            }
        }

        "Should support single tenant access tokens" {
            val id = UUID.randomUUID()
            val childOrganization = randomOrganizationNumber()

            wireMockClient.register(get("/Messages/$id"))

            val helseIdClient = mockk<HelseIdClient> { every { getAccessToken(any()) } returns buildTokenResponse("") }
            FeignApiFactory.createApi(
                mshBaseUrl = wireMock.baseUrl,
                helseIdClient = helseIdClient,
                proofBuilder = mockk { every { buildProof(any(), any(), any()) } returns UUID.randomUUID().toString() },
                tokenParams = HelseIdTokenParameters(
                    tenant = SingleTenantHelseIdTokenParameters(
                        childOrganization = childOrganization,
                    )
                ),
            ).getMessage(id, UUID.randomUUID().toString(), UUID.randomUUID().toString())

            verifySequence {
                helseIdClient.getAccessToken(SingleTenantAccessTokenRequest(childOrganization, TokenType.DPOP))
            }
        }

        "Should support multi tenant access tokens with parent and child" {
            val id = UUID.randomUUID()
            val parentOrganization = randomOrganizationNumber()
            val childOrganization = randomOrganizationNumber()

            wireMockClient.register(get("/Messages/$id"))

            val helseIdClient = mockk<HelseIdClient> { every { getAccessToken(any()) } returns buildTokenResponse("") }
            FeignApiFactory.createApi(
                mshBaseUrl = wireMock.baseUrl,
                helseIdClient = helseIdClient,
                proofBuilder = mockk { every { buildProof(any(), any(), any()) } returns UUID.randomUUID().toString() },
                tokenParams = HelseIdTokenParameters(
                    tenant = MultiTenantHelseIdTokenParameters(
                        parentOrganization = parentOrganization,
                        childOrganization = childOrganization,
                    )
                ),
            ).getMessage(id, UUID.randomUUID().toString(), UUID.randomUUID().toString())

            verifySequence {
                helseIdClient.getAccessToken(MultiTenantAccessTokenRequest(parentOrganization, childOrganization, TokenType.DPOP))
            }
        }

        "Should support multi tenant access tokens with only parent" {
            val id = UUID.randomUUID()
            val parentOrganization = randomOrganizationNumber()

            wireMockClient.register(get("/Messages/$id"))

            val helseIdClient = mockk<HelseIdClient> { every { getAccessToken(any()) } returns buildTokenResponse("") }
            FeignApiFactory.createApi(
                mshBaseUrl = wireMock.baseUrl,
                helseIdClient = helseIdClient,
                proofBuilder = mockk { every { buildProof(any(), any(), any()) } returns UUID.randomUUID().toString() },
                tokenParams = HelseIdTokenParameters(
                    tenant = MultiTenantHelseIdTokenParameters(
                        parentOrganization = parentOrganization,
                    )
                ),
            ).getMessage(id, UUID.randomUUID().toString(), UUID.randomUUID().toString())

            verifySequence {
                helseIdClient.getAccessToken(MultiTenantAccessTokenRequest(parentOrganization, null, TokenType.DPOP))
            }
        }

        "Should call proof builder with correct endpoint and access token" {
            val getMessageId = UUID.randomUUID()
            wireMockClient.register(get("/Messages/$getMessageId"))

            val appRecId = UUID.randomUUID()
            val appRecSenderHerId = nextInt(1, 1000000)
            wireMockClient.register(post("/Messages/$appRecId/apprec/$appRecSenderHerId"))

            val markAsReadId = UUID.randomUUID()
            val markAsReadHerId = nextInt(1, 1000000)
            wireMockClient.register(put("/Messages/$markAsReadId/read/$markAsReadHerId"))

            val accessToken = UUID.randomUUID().toString()
            val proofBuilder = mockk<ProofBuilder> { every { buildProof(any(), any(), any()) } returns UUID.randomUUID().toString() }
            val api = FeignApiFactory.createApi(
                mshBaseUrl = wireMock.baseUrl,
                helseIdClient = mockk { every { getAccessToken(any()) } returns buildTokenResponse(accessToken) },
                proofBuilder = proofBuilder,
            )

            api.getMessage(getMessageId, UUID.randomUUID().toString(), UUID.randomUUID().toString())
            api.postAppRec(appRecId, appRecSenderHerId, UUID.randomUUID().toString(), UUID.randomUUID().toString(), PostAppRecRequest())
            api.markMessageAsRead(markAsReadId, markAsReadHerId, UUID.randomUUID().toString(), UUID.randomUUID().toString())

            verifySequence {
                proofBuilder.buildProof(Endpoint(HttpMethod.GET, "${wireMock.baseUrl}/Messages/$getMessageId"), null, accessToken)
                proofBuilder.buildProof(Endpoint(HttpMethod.POST, "${wireMock.baseUrl}/Messages/$appRecId/apprec/$appRecSenderHerId"), null, accessToken)
                proofBuilder.buildProof(Endpoint(HttpMethod.PUT, "${wireMock.baseUrl}/Messages/$markAsReadId/read/$markAsReadHerId"), null, accessToken)
            }
        }

        "Should add access token and DPoP proof headers" {
            val id = UUID.randomUUID()
            val accessToken = UUID.randomUUID().toString()
            val dpopProof = UUID.randomUUID().toString()

            wireMockClient.register(get("/Messages/$id"))

            FeignApiFactory.createApi(
                mshBaseUrl = wireMock.baseUrl,
                helseIdClient = mockk<HelseIdClient> { every { getAccessToken(any()) } returns buildTokenResponse(accessToken) },
                proofBuilder = mockk<ProofBuilder> { every { buildProof(any(), any(), any()) } returns dpopProof },
            ).getMessage(id, UUID.randomUUID().toString(), UUID.randomUUID().toString())

            wireMockClient.find(getRequestedFor(urlEqualTo("/Messages/$id"))).asClue { requests ->
                requests shouldHaveSize 1
                requests.single().asClue { request ->
                    request.header("Authorization").asClue { authHeaders ->
                        authHeaders.values shouldHaveSize 1
                        authHeaders.values.single().asClue {
                            it shouldBe "DPoP $accessToken"
                        }
                    }
                    request.header("DPoP").asClue { dpopHeaders ->
                        dpopHeaders.values shouldHaveSize 1
                        dpopHeaders.values.single().asClue {
                            it shouldBe dpopProof
                        }
                    }
                }
            }
        }

    }

    private fun buildTokenResponse(accessToken: String) = TokenResponse(accessToken, 0, UUID.randomUUID().toString(), UUID.randomUUID().toString())

}

private fun randomOrganizationNumber() = nextInt(100000000, 1000000000).toString()
