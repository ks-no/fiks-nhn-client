package no.ks.fiks.nhn.msh

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.*
import io.kotest.assertions.asClue
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifySequence
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import no.ks.fiks.helseid.*
import no.ks.fiks.helseid.dpop.Endpoint
import no.ks.fiks.helseid.dpop.HttpMethod
import no.ks.fiks.helseid.dpop.ProofBuilder
import no.ks.fiks.nhn.randomHerId
import no.nhn.msh.v2.model.PostAppRecRequest
import no.nhn.msh.v2.model.PostMessageRequest
import org.wiremock.integrations.testcontainers.WireMockContainer
import java.util.*
import java.util.concurrent.Executors
import kotlin.random.Random.Default.nextInt
import kotlin.time.Duration.Companion.seconds

class MshInternalClientTest : FreeSpec() {


    private val wireMock = WireMockContainer("wiremock/wiremock:3.12.1")
        .also { it.start() }
    private val wireMockClient = WireMock(wireMock.host, wireMock.port)

    init {

        "Get app rec info" - {
            "Should make expected calls" {
                val id = UUID.randomUUID()
                mockGetAppRec(id)

                val accessToken = UUID.randomUUID().toString()
                val helseIdClient = mockk<HelseIdClient> { every { getAccessToken(any()) } returns buildTokenResponse(accessToken) }
                val proofBuilder = mockk<ProofBuilder> { every { buildProof(any(), any(), any()) } returns UUID.randomUUID().toString() }
                MshInternalClient(
                    baseUrl = wireMock.baseUrl,
                    sourceSystem = UUID.randomUUID().toString(),
                    helseIdClient = helseIdClient,
                    proofBuilder = proofBuilder,
                ).getAppRecInfo(id)

                verifySequence {
                    helseIdClient.getAccessToken(StandardAccessTokenRequest(TokenType.DPOP))
                    proofBuilder.buildProof(
                        Endpoint(HttpMethod.GET, "${wireMock.baseUrl}/Messages/$id/apprec"),
                        null,
                        accessToken
                    )
                }
            }

            "Not OK response should throw exception" {
                val id = UUID.randomUUID()
                val expected = UUID.randomUUID().toString()
                mockGetAppRec(id, 500, expected)

                shouldThrow<HttpException> {
                    MshInternalClient(
                        baseUrl = wireMock.baseUrl,
                        sourceSystem = UUID.randomUUID().toString(),
                        helseIdClient = mockk<HelseIdClient> { every { getAccessToken(any()) } returns buildTokenResponse(UUID.randomUUID().toString()) },
                        proofBuilder = mockk<ProofBuilder> { every { buildProof(any(), any(), any()) } returns UUID.randomUUID().toString() },
                    ).getAppRecInfo(id)
                }.asClue {
                    it.status shouldBe 500
                    it.body shouldBe expected
                    it.message shouldBe "Got HTTP status 500: $expected"
                }
            }
        }

        "Get messages" - {
            "Should make expected calls" {
                val receiverHerId = randomHerId()
                mockGetMessages(receiverHerId)

                val accessToken = UUID.randomUUID().toString()
                val helseIdClient = mockk<HelseIdClient> { every { getAccessToken(any()) } returns buildTokenResponse(accessToken) }
                val proofBuilder = mockk<ProofBuilder> { every { buildProof(any(), any(), any()) } returns UUID.randomUUID().toString() }
                MshInternalClient(
                    baseUrl = wireMock.baseUrl,
                    sourceSystem = UUID.randomUUID().toString(),
                    helseIdClient = helseIdClient,
                    proofBuilder = proofBuilder,
                ).getMessages(receiverHerId)

                verifySequence {
                    helseIdClient.getAccessToken(StandardAccessTokenRequest(TokenType.DPOP))
                    proofBuilder.buildProof(Endpoint(HttpMethod.GET, "${wireMock.baseUrl}/Messages"), null, accessToken)
                }
            }

            "Not OK response should throw exception" {
                val receiverHerId = randomHerId()
                val expected = UUID.randomUUID().toString()
                mockGetMessages(receiverHerId, 400, expected)

                shouldThrow<HttpException> {
                    MshInternalClient(
                        baseUrl = wireMock.baseUrl,
                        sourceSystem = UUID.randomUUID().toString(),
                        helseIdClient = mockk<HelseIdClient> { every { getAccessToken(any()) } returns buildTokenResponse(UUID.randomUUID().toString()) },
                        proofBuilder = mockk<ProofBuilder> { every { buildProof(any(), any(), any()) } returns UUID.randomUUID().toString() },
                    ).getMessages(receiverHerId)
                }.asClue {
                    it.status shouldBe 400
                    it.body shouldBe expected
                    it.message shouldBe "Got HTTP status 400: $expected"
                }
            }
        }

        "Send message" - {
            "Should make expected calls" {
                mockSendMessage()

                val accessToken = UUID.randomUUID().toString()
                val helseIdClient = mockk<HelseIdClient> { every { getAccessToken(any()) } returns buildTokenResponse(accessToken) }
                val proofBuilder = mockk<ProofBuilder> { every { buildProof(any(), any(), any()) } returns UUID.randomUUID().toString() }
                MshInternalClient(
                    baseUrl = wireMock.baseUrl,
                    sourceSystem = UUID.randomUUID().toString(),
                    helseIdClient = helseIdClient,
                    proofBuilder = proofBuilder,
                ).postMessage(PostMessageRequest())

                verifySequence {
                    helseIdClient.getAccessToken(StandardAccessTokenRequest(TokenType.DPOP))
                    proofBuilder.buildProof(Endpoint(HttpMethod.POST, "${wireMock.baseUrl}/Messages"), null, accessToken)
                }
            }

            "Not OK response should throw exception" {
                val expected = UUID.randomUUID().toString()
                mockSendMessage(401, expected)

                shouldThrow<HttpException> {
                    MshInternalClient(
                        baseUrl = wireMock.baseUrl,
                        sourceSystem = UUID.randomUUID().toString(),
                        helseIdClient = mockk<HelseIdClient> { every { getAccessToken(any()) } returns buildTokenResponse(UUID.randomUUID().toString()) },
                        proofBuilder = mockk<ProofBuilder> { every { buildProof(any(), any(), any()) } returns UUID.randomUUID().toString() },
                    ).postMessage(PostMessageRequest())
                }.asClue {
                    it.status shouldBe 401
                    it.body shouldBe expected
                    it.message shouldBe "Got HTTP status 401: $expected"
                }
            }
        }

        "Get message" - {
            "Should make expected calls" {
                val id = UUID.randomUUID()
                mockGetMessage(id)

                val accessToken = UUID.randomUUID().toString()
                val helseIdClient = mockk<HelseIdClient> { every { getAccessToken(any()) } returns buildTokenResponse(accessToken) }
                val proofBuilder = mockk<ProofBuilder> { every { buildProof(any(), any(), any()) } returns UUID.randomUUID().toString() }
                MshInternalClient(
                    baseUrl = wireMock.baseUrl,
                    sourceSystem = UUID.randomUUID().toString(),
                    helseIdClient = helseIdClient,
                    proofBuilder = proofBuilder,
                ).getMessage(id)

                verifySequence {
                    helseIdClient.getAccessToken(StandardAccessTokenRequest(TokenType.DPOP))
                    proofBuilder.buildProof(Endpoint(HttpMethod.GET, "${wireMock.baseUrl}/Messages/$id"), null, accessToken)
                }
            }

            "Not OK response should throw exception" {
                val id = UUID.randomUUID()
                val expected = UUID.randomUUID().toString()
                mockGetMessage(id, 403, expected)

                shouldThrow<HttpException> {
                    MshInternalClient(
                        baseUrl = wireMock.baseUrl,
                        sourceSystem = UUID.randomUUID().toString(),
                        helseIdClient = mockk<HelseIdClient> { every { getAccessToken(any()) } returns buildTokenResponse(UUID.randomUUID().toString()) },
                        proofBuilder = mockk<ProofBuilder> { every { buildProof(any(), any(), any()) } returns UUID.randomUUID().toString() },
                    ).getMessage(id)
                }.asClue {
                    it.status shouldBe 403
                    it.body shouldBe expected
                    it.message shouldBe "Got HTTP status 403: $expected"
                }
            }
        }

        "Get business document" - {
            "Should make expected calls" {
                val id = UUID.randomUUID()
                mockGetBusinessDocument(id)

                val accessToken = UUID.randomUUID().toString()
                val helseIdClient = mockk<HelseIdClient> { every { getAccessToken(any()) } returns buildTokenResponse(accessToken) }
                val proofBuilder = mockk<ProofBuilder> { every { buildProof(any(), any(), any()) } returns UUID.randomUUID().toString() }
                MshInternalClient(
                    baseUrl = wireMock.baseUrl,
                    sourceSystem = UUID.randomUUID().toString(),
                    helseIdClient = helseIdClient,
                    proofBuilder = proofBuilder,
                ).getBusinessDocument(id)

                verifySequence {
                    helseIdClient.getAccessToken(StandardAccessTokenRequest(TokenType.DPOP))
                    proofBuilder.buildProof(
                        Endpoint(HttpMethod.GET, "${wireMock.baseUrl}/Messages/$id/business-document"),
                        null,
                        accessToken
                    )
                }
            }

            "Not OK response should throw exception" {
                val id = UUID.randomUUID()
                val expected = UUID.randomUUID().toString()
                mockGetBusinessDocument(id, 503, expected)

                shouldThrow<HttpException> {
                    MshInternalClient(
                        baseUrl = wireMock.baseUrl,
                        sourceSystem = UUID.randomUUID().toString(),
                        helseIdClient = mockk<HelseIdClient> { every { getAccessToken(any()) } returns buildTokenResponse(UUID.randomUUID().toString()) },
                        proofBuilder = mockk<ProofBuilder> { every { buildProof(any(), any(), any()) } returns UUID.randomUUID().toString() },
                    ).getBusinessDocument(id)
                }.asClue {
                    it.status shouldBe 503
                    it.body shouldBe expected
                    it.message shouldBe "Got HTTP status 503: $expected"
                }
            }
        }

        "Get status" - {
            "Should make expected calls" {
                val id = UUID.randomUUID()
                mockGetStatus(id)

                val accessToken = UUID.randomUUID().toString()
                val helseIdClient = mockk<HelseIdClient> { every { getAccessToken(any()) } returns buildTokenResponse(accessToken) }
                val proofBuilder = mockk<ProofBuilder> { every { buildProof(any(), any(), any()) } returns UUID.randomUUID().toString() }
                MshInternalClient(
                    baseUrl = wireMock.baseUrl,
                    sourceSystem = UUID.randomUUID().toString(),
                    helseIdClient = helseIdClient,
                    proofBuilder = proofBuilder,
                ).getStatus(id)

                verifySequence {
                    helseIdClient.getAccessToken(StandardAccessTokenRequest(TokenType.DPOP))
                    proofBuilder.buildProof(
                        Endpoint(HttpMethod.GET, "${wireMock.baseUrl}/Messages/$id/status"),
                        null,
                        accessToken
                    )
                }
            }

            "Not OK response should throw exception" {
                val id = UUID.randomUUID()
                val expected = UUID.randomUUID().toString()
                mockGetStatus(id, 504, expected)

                shouldThrow<HttpException> {
                    MshInternalClient(
                        baseUrl = wireMock.baseUrl,
                        sourceSystem = UUID.randomUUID().toString(),
                        helseIdClient = mockk<HelseIdClient> { every { getAccessToken(any()) } returns buildTokenResponse(UUID.randomUUID().toString()) },
                        proofBuilder = mockk<ProofBuilder> { every { buildProof(any(), any(), any()) } returns UUID.randomUUID().toString() },
                    ).getStatus(id)
                }.asClue {
                    it.status shouldBe 504
                    it.body shouldBe expected
                    it.message shouldBe "Got HTTP status 504: $expected"
                }
            }
        }

        "Post app rec" - {
            "Should make expected calls" {
                val id = UUID.randomUUID()
                val senderHerId = nextInt(1, 1000000)
                mockPostAppRec(id, senderHerId)

                val accessToken = UUID.randomUUID().toString()
                val helseIdClient = mockk<HelseIdClient> { every { getAccessToken(any()) } returns buildTokenResponse(accessToken) }
                val proofBuilder = mockk<ProofBuilder> { every { buildProof(any(), any(), any()) } returns UUID.randomUUID().toString() }
                MshInternalClient(
                    baseUrl = wireMock.baseUrl,
                    sourceSystem = UUID.randomUUID().toString(),
                    helseIdClient = helseIdClient,
                    proofBuilder = proofBuilder,
                ).postAppRec(id, senderHerId, PostAppRecRequest())

                verifySequence {
                    helseIdClient.getAccessToken(StandardAccessTokenRequest(TokenType.DPOP))
                    proofBuilder.buildProof(
                        Endpoint(
                            HttpMethod.POST,
                            "${wireMock.baseUrl}/Messages/$id/apprec/$senderHerId"
                        ), null, accessToken
                    )
                }
            }

            "Not OK response should throw exception" {
                val id = UUID.randomUUID()
                val senderHerId = nextInt(1, 1000000)
                val expected = UUID.randomUUID().toString()
                mockPostAppRec(id, senderHerId, 500, expected)

                shouldThrow<HttpException> {
                    MshInternalClient(
                        baseUrl = wireMock.baseUrl,
                        sourceSystem = UUID.randomUUID().toString(),
                        helseIdClient = mockk<HelseIdClient> { every { getAccessToken(any()) } returns buildTokenResponse(UUID.randomUUID().toString()) },
                        proofBuilder = mockk<ProofBuilder> { every { buildProof(any(), any(), any()) } returns UUID.randomUUID().toString() },
                    ).postAppRec(id, senderHerId, PostAppRecRequest())
                }.asClue {
                    it.status shouldBe 500
                    it.body shouldBe expected
                    it.message shouldBe "Got HTTP status 500: $expected"
                }
            }
        }

        "Mark message read" - {
            "Should make expected calls" {
                val id = UUID.randomUUID()
                val senderHerId = nextInt(1, 1000000)
                mockMarkMessageRead(id, senderHerId)

                val accessToken = UUID.randomUUID().toString()
                val helseIdClient = mockk<HelseIdClient> { every { getAccessToken(any()) } returns buildTokenResponse(accessToken) }
                val proofBuilder = mockk<ProofBuilder> { every { buildProof(any(), any(), any()) } returns UUID.randomUUID().toString() }
                MshInternalClient(
                    baseUrl = wireMock.baseUrl,
                    sourceSystem = UUID.randomUUID().toString(),
                    helseIdClient = helseIdClient,
                    proofBuilder = proofBuilder,
                ).markMessageRead(id, senderHerId)

                verifySequence {
                    helseIdClient.getAccessToken(StandardAccessTokenRequest(TokenType.DPOP))
                    proofBuilder.buildProof(
                        Endpoint(HttpMethod.PUT, "${wireMock.baseUrl}/Messages/$id/read/$senderHerId"),
                        null,
                        accessToken
                    )
                }
            }

            "Not OK response should throw exception" {
                val id = UUID.randomUUID()
                val senderHerId = nextInt(1, 1000000)
                val expected = UUID.randomUUID().toString()
                mockMarkMessageRead(id, senderHerId, 500, expected)

                shouldThrow<HttpException> {
                    MshInternalClient(
                        baseUrl = wireMock.baseUrl,
                        sourceSystem = UUID.randomUUID().toString(),
                        helseIdClient = mockk<HelseIdClient> { every { getAccessToken(any()) } returns buildTokenResponse(UUID.randomUUID().toString()) },
                        proofBuilder = mockk<ProofBuilder> { every { buildProof(any(), any(), any()) } returns UUID.randomUUID().toString() },
                    ).markMessageRead(id, senderHerId)
                }.asClue {
                    it.status shouldBe 500
                    it.body shouldBe expected
                    it.message shouldBe "Got HTTP status 500: $expected"
                }
            }
        }

        "Test HelseID parameters" - {
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

    }

    private fun mockGetAppRec(id: UUID, status: Int = 200, body: String = "[]") {
        wireMockClient.register(
            get("/Messages/$id/apprec")
                .willReturn(
                    status(status)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)
                )
        )
    }

    private fun mockGetMessages(receiverHerId: Int, status: Int = 200, body: String = "[]") {
        wireMockClient.register(
            get(urlPathEqualTo("/Messages"))
                .withQueryParam("receiverHerIds", equalTo(receiverHerId.toString()))
                .willReturn(
                    status(status)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)
                )
        )
    }

    private fun mockGetMessage(id: UUID, status: Int = 200, body: String = "{}") {
        wireMockClient.register(
            get("/Messages/$id")
                .willReturn(
                    status(status)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)
                )
        )
    }

    private fun mockGetBusinessDocument(id: UUID, status: Int = 200, body: String = "{}") {
        wireMockClient.register(
            get("/Messages/$id/business-document")
                .willReturn(
                    status(status)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)
                )
        )
    }

    private fun mockGetStatus(id: UUID, status: Int = 200, body: String = "[]") {
        wireMockClient.register(
            get("/Messages/$id/status")
                .willReturn(
                    status(status)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)
                )
        )
    }

    private fun mockSendMessage(status: Int = 201, body: String = "\"${UUID.randomUUID()}\"") {
        wireMockClient.register(
            post("/Messages")
                .willReturn(
                    status(status)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)
                )
        )
    }

    private fun mockPostAppRec(appRecId: UUID, appRecSenderHerId: Int, status: Int = 201, body: String = "\"${UUID.randomUUID()}\"") {
        wireMockClient.register(
            post("/Messages/$appRecId/apprec/$appRecSenderHerId")
                .willReturn(
                    status(status)
                        .withHeader("Content-Type", "text/plain")
                        .withBody(body)
                )
        )
    }

    private fun mockMarkMessageRead(markAsReadId: UUID, markAsReadHerId: Int, status: Int = 204, body: String = "") {
        wireMockClient.register(
            put("/Messages/$markAsReadId/read/$markAsReadHerId")
                .willReturn(status(status)
                    .withBody(body))
        )
    }

}

private fun buildTokenResponse(accessToken: String) =
    TokenResponse(accessToken, 0, UUID.randomUUID().toString(), UUID.randomUUID().toString())

private fun randomOrganizationNumber() = nextInt(100000000, 1000000000).toString()
