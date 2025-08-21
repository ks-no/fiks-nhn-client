package no.ks.fiks.nhn.msh

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.put
import com.github.tomakehurst.wiremock.client.WireMock.status
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import io.kotest.assertions.asClue
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifySequence
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import no.ks.fiks.helseid.AccessTokenRequest
import no.ks.fiks.helseid.HelseIdClient
import no.ks.fiks.helseid.MultiTenantAccessTokenRequest
import no.ks.fiks.helseid.SingleTenantAccessTokenRequest
import no.ks.fiks.helseid.StandardAccessTokenRequest
import no.ks.fiks.helseid.TokenResponse
import no.ks.fiks.helseid.TokenType
import no.ks.fiks.helseid.dpop.Endpoint
import no.ks.fiks.helseid.dpop.HttpMethod
import no.ks.fiks.helseid.dpop.ProofBuilder
import no.ks.fiks.nhn.randomHerId
import no.nhn.msh.v2.model.PostAppRecRequest
import no.nhn.msh.v2.model.PostMessageRequest
import org.wiremock.integrations.testcontainers.WireMockContainer
import java.util.UUID
import java.util.concurrent.Executors
import kotlin.random.Random.Default.nextInt
import kotlin.time.Duration.Companion.seconds

class MshInternalClientTest : StringSpec() {


    private val wireMock = WireMockContainer("wiremock/wiremock:3.12.1")
        .also { it.start() }
    private val wireMockClient = WireMock(wireMock.host, wireMock.port)

    init {

        "Test getting app rec info" {
            val id = UUID.randomUUID()
            mockGetAppRec(id)

            val accessToken = UUID.randomUUID().toString()
            val proofBuilder =
                mockk<ProofBuilder> { every { buildProof(any(), any(), any()) } returns UUID.randomUUID().toString() }
            MshInternalClient(
                baseUrl = wireMock.baseUrl,
                sourceSystem = UUID.randomUUID().toString(),
                helseIdClient = mockk { every { getAccessToken(any()) } returns buildTokenResponse(accessToken) },
                proofBuilder = proofBuilder,
            ).getAppRecInfo(id)

            verifySequence {
                proofBuilder.buildProof(
                    Endpoint(HttpMethod.GET, "${wireMock.baseUrl}/Messages/$id/apprec"),
                    null,
                    accessToken
                )
            }
        }

        "Test getting messages" {
            val receiverHerId = randomHerId()
            mockGetMessages(receiverHerId)

            val accessToken = UUID.randomUUID().toString()
            val proofBuilder =
                mockk<ProofBuilder> { every { buildProof(any(), any(), any()) } returns UUID.randomUUID().toString() }
            MshInternalClient(
                baseUrl = wireMock.baseUrl,
                sourceSystem = UUID.randomUUID().toString(),
                helseIdClient = mockk { every { getAccessToken(any()) } returns buildTokenResponse(accessToken) },
                proofBuilder = proofBuilder,
            ).getMessages(receiverHerId)

            verifySequence {
                proofBuilder.buildProof(Endpoint(HttpMethod.GET, "${wireMock.baseUrl}/Messages"), null, accessToken)
            }
        }

        "Test send message" {
            mockSendMessage()

            val accessToken = UUID.randomUUID().toString()
            val proofBuilder =
                mockk<ProofBuilder> { every { buildProof(any(), any(), any()) } returns UUID.randomUUID().toString() }
            MshInternalClient(
                baseUrl = wireMock.baseUrl,
                sourceSystem = UUID.randomUUID().toString(),
                helseIdClient = mockk { every { getAccessToken(any()) } returns buildTokenResponse(accessToken) },
                proofBuilder = proofBuilder,
            ).postMessage(PostMessageRequest())

            verifySequence {
                proofBuilder.buildProof(Endpoint(HttpMethod.POST, "${wireMock.baseUrl}/Messages"), null, accessToken)
            }
        }

        "Test get message" {
            val id = UUID.randomUUID()
            mockGetMessage(id)

            val accessToken = UUID.randomUUID().toString()
            val proofBuilder =
                mockk<ProofBuilder> { every { buildProof(any(), any(), any()) } returns UUID.randomUUID().toString() }
            MshInternalClient(
                baseUrl = wireMock.baseUrl,
                sourceSystem = UUID.randomUUID().toString(),
                helseIdClient = mockk { every { getAccessToken(any()) } returns buildTokenResponse(accessToken) },
                proofBuilder = proofBuilder,
            ).getMessage(id)

            verifySequence {
                proofBuilder.buildProof(Endpoint(HttpMethod.GET, "${wireMock.baseUrl}/Messages/$id"), null, accessToken)
            }
        }

        "Test get business document" {
            val id = UUID.randomUUID()
            mockGetBusinessDocument(id)

            val accessToken = UUID.randomUUID().toString()
            val proofBuilder =
                mockk<ProofBuilder> { every { buildProof(any(), any(), any()) } returns UUID.randomUUID().toString() }
            MshInternalClient(
                baseUrl = wireMock.baseUrl,
                sourceSystem = UUID.randomUUID().toString(),
                helseIdClient = mockk { every { getAccessToken(any()) } returns buildTokenResponse(accessToken) },
                proofBuilder = proofBuilder,
            ).getBusinessDocument(id)

            verifySequence {
                proofBuilder.buildProof(
                    Endpoint(HttpMethod.GET, "${wireMock.baseUrl}/Messages/$id/business-document"),
                    null,
                    accessToken
                )
            }
        }

        "Test get status" {
            val id = UUID.randomUUID()
            mockGetStatus(id)

            val accessToken = UUID.randomUUID().toString()
            val proofBuilder =
                mockk<ProofBuilder> { every { buildProof(any(), any(), any()) } returns UUID.randomUUID().toString() }
            MshInternalClient(
                baseUrl = wireMock.baseUrl,
                sourceSystem = UUID.randomUUID().toString(),
                helseIdClient = mockk { every { getAccessToken(any()) } returns buildTokenResponse(accessToken) },
                proofBuilder = proofBuilder,
            ).getStatus(id)

            verifySequence {
                proofBuilder.buildProof(
                    Endpoint(HttpMethod.GET, "${wireMock.baseUrl}/Messages/$id/status"),
                    null,
                    accessToken
                )
            }
        }

        "Test post app rec" {
            val id = UUID.randomUUID()
            val senderHerId = nextInt(1, 1000000)
            mockPostAppRec(id, senderHerId)

            val accessToken = UUID.randomUUID().toString()
            val proofBuilder =
                mockk<ProofBuilder> { every { buildProof(any(), any(), any()) } returns UUID.randomUUID().toString() }
            MshInternalClient(
                baseUrl = wireMock.baseUrl,
                sourceSystem = UUID.randomUUID().toString(),
                helseIdClient = mockk { every { getAccessToken(any()) } returns buildTokenResponse(accessToken) },
                proofBuilder = proofBuilder,
            ).postAppRec(id, senderHerId, PostAppRecRequest())

            verifySequence {
                proofBuilder.buildProof(
                    Endpoint(
                        HttpMethod.POST,
                        "${wireMock.baseUrl}/Messages/$id/apprec/$senderHerId"
                    ), null, accessToken
                )
            }
        }

        "Test mark message read" {
            val id = UUID.randomUUID()
            val senderHerId = nextInt(1, 1000000)
            mockMarkMessageRead(id, senderHerId)

            val accessToken = UUID.randomUUID().toString()
            val proofBuilder =
                mockk<ProofBuilder> { every { buildProof(any(), any(), any()) } returns UUID.randomUUID().toString() }
            MshInternalClient(
                baseUrl = wireMock.baseUrl,
                sourceSystem = UUID.randomUUID().toString(),
                helseIdClient = mockk { every { getAccessToken(any()) } returns buildTokenResponse(accessToken) },
                proofBuilder = proofBuilder,
            ).markMessageRead(id, senderHerId)

            verifySequence {
                proofBuilder.buildProof(
                    Endpoint(HttpMethod.PUT, "${wireMock.baseUrl}/Messages/$id/read/$senderHerId"),
                    null,
                    accessToken
                )
            }
        }
        "If no token params are passed, should get token with standard DPoP token request" {
            val id = UUID.randomUUID()
            mockGetMessage(id)

            val helseIdClient = mockk<HelseIdClient> { every { getAccessToken(any()) } returns buildTokenResponse("") }
            MshInternalClient(
                baseUrl = wireMock.baseUrl,
                sourceSystem = UUID.randomUUID().toString(),
                helseIdClient = helseIdClient,
                proofBuilder = mockk { every { buildProof(any(), any(), any()) } returns UUID.randomUUID().toString() },
            ).getMessage(id)

            verifySequence {
                helseIdClient.getAccessToken(StandardAccessTokenRequest(TokenType.DPOP))
            }
        }

        "Should support single tenant access tokens" {
            val id = UUID.randomUUID()
            mockGetMessage(id)
            val childOrganization = randomOrganizationNumber()

            val helseIdClient = mockk<HelseIdClient> { every { getAccessToken(any()) } returns buildTokenResponse("") }
            MshInternalClient(
                baseUrl = wireMock.baseUrl,
                sourceSystem = UUID.randomUUID().toString(),
                helseIdClient = helseIdClient,
                proofBuilder = mockk { every { buildProof(any(), any(), any()) } returns UUID.randomUUID().toString() },
                defaultTokenParams = HelseIdTokenParameters(
                    tenant = SingleTenantHelseIdTokenParameters(
                        childOrganization = childOrganization,
                    )
                ),
            ).getMessage(id)

            verifySequence {
                helseIdClient.getAccessToken(SingleTenantAccessTokenRequest(childOrganization, TokenType.DPOP))
            }
        }

        "Should support multi tenant access tokens with parent and child" {
            val id = UUID.randomUUID()
            mockGetMessage(id)
            val parentOrganization = randomOrganizationNumber()
            val childOrganization = randomOrganizationNumber()

            val helseIdClient = mockk<HelseIdClient> { every { getAccessToken(any()) } returns buildTokenResponse("") }
            MshInternalClient(
                baseUrl = wireMock.baseUrl,
                sourceSystem = UUID.randomUUID().toString(),
                helseIdClient = helseIdClient,
                proofBuilder = mockk { every { buildProof(any(), any(), any()) } returns UUID.randomUUID().toString() },
                defaultTokenParams = HelseIdTokenParameters(
                    tenant = MultiTenantHelseIdTokenParameters(
                        parentOrganization = parentOrganization,
                        childOrganization = childOrganization,
                    )
                ),
            ).getMessage(id)

            verifySequence {
                helseIdClient.getAccessToken(
                    MultiTenantAccessTokenRequest(
                        parentOrganization,
                        childOrganization,
                        TokenType.DPOP
                    )
                )
            }
        }

        "Should support multi tenant access tokens with only parent" {
            val id = UUID.randomUUID()
            mockGetMessage(id)
            val parentOrganization = randomOrganizationNumber()

            val helseIdClient = mockk<HelseIdClient> { every { getAccessToken(any()) } returns buildTokenResponse("") }
            MshInternalClient(
                baseUrl = wireMock.baseUrl,
                sourceSystem = UUID.randomUUID().toString(),
                helseIdClient = helseIdClient,
                proofBuilder = mockk { every { buildProof(any(), any(), any()) } returns UUID.randomUUID().toString() },
                defaultTokenParams = HelseIdTokenParameters(
                    tenant = MultiTenantHelseIdTokenParameters(
                        parentOrganization = parentOrganization,
                    )
                ),
            ).getMessage(id)

            verifySequence {
                helseIdClient.getAccessToken(MultiTenantAccessTokenRequest(parentOrganization, null, TokenType.DPOP))
            }
        }

        "Should support requesting single tenant access tokens using thread local configuration which overrides static configuration" {
            val id = UUID.randomUUID()
            mockGetMessage(id)
            val configuredClientChildOrganization = randomOrganizationNumber()
            val requestChildOrganization = randomOrganizationNumber()

            val helseIdClient = mockk<HelseIdClient> {
                every { getAccessToken(any()) } returns TokenResponse(
                    UUID.randomUUID().toString(), 0, "", ""
                )
            }
            MshInternalClient(
                baseUrl = wireMock.baseUrl,
                sourceSystem = UUID.randomUUID().toString(),
                helseIdClient = helseIdClient,
                proofBuilder = mockk { every { buildProof(any(), any(), any()) } returns UUID.randomUUID().toString() },
                defaultTokenParams = HelseIdTokenParameters(
                    tenant = SingleTenantHelseIdTokenParameters(
                        childOrganization = configuredClientChildOrganization,
                    )
                ),
            ).getMessage(
                id,
                RequestParameters(HelseIdTokenParameters(SingleTenantHelseIdTokenParameters(requestChildOrganization)))
            )

            verifySequence {
                helseIdClient.getAccessToken(SingleTenantAccessTokenRequest(requestChildOrganization, TokenType.DPOP))
            }
        }

        "Should support requesting multi tenant access tokens using thread local configuration which overrides static configuration" {
            val id = UUID.randomUUID()
            mockGetMessage(id)
            val configuredClientParentOrganization = randomOrganizationNumber()
            val configuredClientChildOrganization = randomOrganizationNumber()
            val requestParentOrganization = randomOrganizationNumber()
            val requestChildOrganization = randomOrganizationNumber()


            val helseIdClient = mockk<HelseIdClient> {
                every { getAccessToken(any()) } returns TokenResponse(
                    UUID.randomUUID().toString(), 0, "", ""
                )
            }
            MshInternalClient(
                baseUrl = wireMock.baseUrl,
                sourceSystem = UUID.randomUUID().toString(),
                helseIdClient = helseIdClient,
                proofBuilder = mockk { every { buildProof(any(), any(), any()) } returns UUID.randomUUID().toString() },
                defaultTokenParams = HelseIdTokenParameters(
                    tenant = MultiTenantHelseIdTokenParameters(
                        parentOrganization = configuredClientParentOrganization,
                        childOrganization = configuredClientChildOrganization,
                    )
                ),
            ).getMessage(
                id,
                RequestParameters(
                    HelseIdTokenParameters(
                        MultiTenantHelseIdTokenParameters(
                            requestParentOrganization,
                            requestChildOrganization
                        )
                    )
                )
            )

            verifySequence {
                helseIdClient.getAccessToken(
                    MultiTenantAccessTokenRequest(
                        requestParentOrganization,
                        requestChildOrganization,
                        TokenType.DPOP
                    )
                )
            }
        }

        "Should add access token and DPoP proof headers" {
            val id = UUID.randomUUID()
            mockGetMessage(id)
            val accessToken = UUID.randomUUID().toString()
            val dpopProof = UUID.randomUUID().toString()

            MshInternalClient(
                baseUrl = wireMock.baseUrl,
                sourceSystem = UUID.randomUUID().toString(),
                helseIdClient = mockk<HelseIdClient> {
                    every { getAccessToken(any()) } returns buildTokenResponse(
                        accessToken
                    )
                },
                proofBuilder = mockk { every { buildProof(any(), any(), any()) } returns dpopProof },
            ).getMessage(id)

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

        "Should handle requests from many parallel threads with different configurations" {
            val staticParams = MultiTenantHelseIdTokenParameters(
                parentOrganization = randomOrganizationNumber(),
                childOrganization = randomOrganizationNumber(),
            )
            val requestParameters = List(nextInt(50, 100)) {
                when (nextInt(0, 3)) {
                    0 -> SingleTenantHelseIdTokenParameters(randomOrganizationNumber())
                    1 -> MultiTenantHelseIdTokenParameters(randomOrganizationNumber(), randomOrganizationNumber())
                    else -> null
                }
            }
            val requestIds = List(requestParameters.size) { UUID.randomUUID() }.onEach {
                mockGetMessage(it)
            }

            val capturedTokenRequests = mutableListOf<AccessTokenRequest>()
            val helseIdClient = mockk<HelseIdClient> {
                every { getAccessToken(capture(capturedTokenRequests)) } returns TokenResponse(
                    UUID.randomUUID().toString(),
                    0,
                    "",
                    ""
                )
            }
            val client = MshInternalClient(
                baseUrl = wireMock.baseUrl,
                sourceSystem = UUID.randomUUID().toString(),
                helseIdClient = helseIdClient,
                proofBuilder = mockk { every { buildProof(any(), any(), any()) } returns UUID.randomUUID().toString() },
                defaultTokenParams = HelseIdTokenParameters(
                    tenant = staticParams,
                ),
            )
            requestParameters.forEachIndexed { i, params ->
                launch(Executors.newFixedThreadPool(8).asCoroutineDispatcher()) {
                    client.getMessage(requestIds[i], RequestParameters(HelseIdTokenParameters(params)))
                }
            }

            eventually(3.seconds) {
                capturedTokenRequests.size shouldBe requestParameters.size
            }

            requestParameters.forEach {
                val expected = when (it) {
                    null -> MultiTenantAccessTokenRequest(
                        staticParams.parentOrganization,
                        staticParams.childOrganization,
                        TokenType.DPOP
                    )

                    is SingleTenantHelseIdTokenParameters -> SingleTenantAccessTokenRequest(
                        it.childOrganization,
                        TokenType.DPOP
                    )

                    is MultiTenantHelseIdTokenParameters -> MultiTenantAccessTokenRequest(
                        it.parentOrganization,
                        it.childOrganization,
                        TokenType.DPOP
                    )
                }
                capturedTokenRequests shouldContain expected
            }
            capturedTokenRequests.count {
                it == MultiTenantAccessTokenRequest(
                    staticParams.parentOrganization,
                    staticParams.childOrganization,
                    TokenType.DPOP
                )
            } shouldBe requestParameters.count { it == null }
        }

    }

    private fun mockGetAppRec(id: UUID) {
        wireMockClient.register(
            get("/Messages/$id/apprec")
                .willReturn(
                    status(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")
                )
        )
    }

    private fun mockGetMessages(receiverHerId: Int) {
        wireMockClient.register(
            get(urlPathEqualTo("/Messages"))
                .withQueryParam("receiverHerIds", equalTo(receiverHerId.toString()))
                .willReturn(
                    status(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")
                )
        )
    }

    private fun mockGetMessage(id: UUID) {
        wireMockClient.register(
            get("/Messages/$id")
                .willReturn(
                    status(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{}")
                )
        )
    }

    private fun mockGetBusinessDocument(id: UUID) {
        wireMockClient.register(
            get("/Messages/$id/business-document")
                .willReturn(
                    status(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{}")
                )
        )
    }

    private fun mockGetStatus(id: UUID) {
        wireMockClient.register(
            get("/Messages/$id/status")
                .willReturn(
                    status(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")
                )
        )
    }

    private fun mockSendMessage() {
        wireMockClient.register(
            post("/Messages")
                .willReturn(
                    status(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(UUID.randomUUID().toString())
                )
        )
    }

    private fun mockPostAppRec(appRecId: UUID, appRecSenderHerId: Int) {
        wireMockClient.register(
            post("/Messages/$appRecId/apprec/$appRecSenderHerId")
                .willReturn(
                    status(200)
                        .withHeader("Content-Type", "text/plain")
                        .withBody(UUID.randomUUID().toString())
                )
        )
    }

    private fun mockMarkMessageRead(markAsReadId: UUID, markAsReadHerId: Int) {
        wireMockClient.register(
            put("/Messages/$markAsReadId/read/$markAsReadHerId")
                .willReturn(status(200))
        )
    }

}

private fun buildTokenResponse(accessToken: String) =
    TokenResponse(accessToken, 0, UUID.randomUUID().toString(), UUID.randomUUID().toString())

private fun randomOrganizationNumber() = nextInt(100000000, 1000000000).toString()
