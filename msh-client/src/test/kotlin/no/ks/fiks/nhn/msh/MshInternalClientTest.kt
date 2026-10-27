package no.ks.fiks.nhn.msh

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.*
import io.kotest.assertions.asClue
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeEmpty
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
import no.ks.fiks.nhn.readResourceContentAsString
import no.nhn.msh.v3.model.AppRecStatus as NhnAppRecStatus
import no.nhn.msh.v3.model.DeleteNotificationsRequest as NhnDeleteNotificationsRequest
import no.nhn.msh.v3.model.DeliveryState as NhnDeliveryState
import no.nhn.msh.v3.model.GetUnreadNotificationsResponse as NhnGetUnreadNotificationsResponse
import no.nhn.msh.v3.model.MarkAsDownloadedRequest as NhnMarkAsDownloadedRequest
import no.nhn.msh.v3.model.MshConfiguration as NhnMshConfiguration
import no.nhn.msh.v3.model.MshConfigurationRejectMessageFilters as NhnMshConfigurationRejectMessageFilters
import no.nhn.msh.v3.model.NotificationType as NhnNotificationType
import no.nhn.msh.v3.model.PostAppRecRequest
import no.nhn.msh.v3.model.PostMessageRequest
import no.nhn.msh.v3.model.ReceiveNotificationChannel as NhnReceiveNotificationChannel
import no.nhn.msh.v3.model.SetMshConfigurationsRequest as NhnSetMshConfigurationsRequest
import no.nhn.msh.v3.model.UnreadNotification as NhnUnreadNotification

import org.wiremock.integrations.testcontainers.WireMockContainer
import java.time.OffsetDateTime
import java.util.*
import java.util.concurrent.Executors
import kotlin.random.Random.Default.nextInt
import kotlin.time.Duration.Companion.seconds

class MshInternalClientTest : FreeSpec() {

    private val wireMock = WireMockContainer("wiremock/wiremock:3.12.1")
        .also { it.start() }
    private val wireMockClient = WireMock(wireMock.host, wireMock.port)
    private val objectMapper = ObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    private fun Any.toJson(): String = objectMapper.writeValueAsString(this)

    init {

        "Notifications" - {

            "Get unread notifications should make expected calls and parse response" {
                val herIds = listOf(42, 1337)
                val notificationsToFetch = 10
                mockGetUnreadNotifications(herIds, notificationsToFetch)

                val accessToken = UUID.randomUUID().toString()
                val helseIdClient = mockHelseIdClient(accessToken)
                val proofBuilder = mockProofBuilder()
                val client = MshInternalClient(
                    baseUrl = wireMock.baseUrl,
                    sourceSystem = UUID.randomUUID().toString(),
                    helseIdClient = helseIdClient,
                    proofBuilder = proofBuilder,
                )
                client.getUnreadNotifications(herIds, notificationsToFetch)
                    .asClue {
                        it shouldHaveSize 2
                        with(it[0]) {
                            notificationId shouldBe "17aaeaa7-fa1e-4b60-a8e9-bdc31718dfc9"
                            relatedMessageId shouldBe "0d5c8d05-ab57-4b2b-b480-c46c69cdf789"
                            type shouldBe NhnNotificationType.NEW_MESSAGE
                            notificationReceiverHerId shouldBe 6789
                            notificationTriggeredByHerId shouldBe 9876
                            description shouldBe "New message received (EPIKRISE)."
                        }
                    }

                verifySequence {
                    helseIdClient.getAccessToken(StandardAccessTokenRequest(TokenType.DPOP))
                    proofBuilder.buildProof(Endpoint(HttpMethod.GET, "${wireMock.baseUrl}/notifications/unread"), null, accessToken)
                }
            }

            "Unread notification without createdAt should be parsed" {
                val herIds = listOf(randomHerId())
                mockGetUnreadNotifications(
                    herIds,
                    body = NhnGetUnreadNotificationsResponse()
                        .unreadNotifications(
                            listOf(
                                NhnUnreadNotification()
                                    .notificationId("17aaeaa7-fa1e-4b60-a8e9-bdc31718dfc9")
                                    .type(NhnNotificationType.NEW_MESSAGE)
                                    .notificationReceiverHerId(6789),
                            )
                        )
                        .toJson(),
                )

                buildDefaultClient().getUnreadNotifications(herIds).asClue {
                    it shouldHaveSize 1
                    with(it.single()) {
                        notificationId shouldBe "17aaeaa7-fa1e-4b60-a8e9-bdc31718dfc9"
                        type shouldBe NhnNotificationType.NEW_MESSAGE
                        notificationReceiverHerId shouldBe 6789
                        createdAt shouldBe null
                    }
                }
            }

            "Server error when getting unread notifications should throw exception" {
                val herIds = listOf(randomHerId())
                val expected = UUID.randomUUID().toString()
                mockGetUnreadNotifications(herIds, status = 500, body = expected)

                shouldThrow<HttpServerException> {
                    buildDefaultClient().getUnreadNotifications(herIds)
                }.asClue {
                    it.status shouldBe 500
                    it.body shouldBe expected
                    it.message shouldBe "Got HTTP status 500: $expected"
                }
            }

            "Delete notifications should make expected calls" {
                val notificationIds = listOf(
                    UUID.fromString("17aaeaa7-fa1e-4b60-a8e9-bdc31718dfc9"),
                    UUID.fromString("c5e6b600-78ef-4d2a-ad5e-e4b64e3f5230"),
                )
                mockDeleteNotifications(
                    expectedBody = NhnDeleteNotificationsRequest()
                        .notificationIds(notificationIds.map { it.toString() }.toSet())
                )

                val accessToken = UUID.randomUUID().toString()
                val helseIdClient = mockHelseIdClient(accessToken)
                val proofBuilder = mockProofBuilder()
                MshInternalClient(
                    baseUrl = wireMock.baseUrl,
                    sourceSystem = UUID.randomUUID().toString(),
                    helseIdClient = helseIdClient,
                    proofBuilder = proofBuilder,
                ).deleteNotifications(notificationIds)

                verifySequence {
                    helseIdClient.getAccessToken(StandardAccessTokenRequest(TokenType.DPOP))
                    proofBuilder.buildProof(Endpoint(HttpMethod.POST, "${wireMock.baseUrl}/notifications/delete"), null, accessToken)
                }
            }

            "Client error when deleting notifications should throw exception" {
                val expected = UUID.randomUUID().toString()
                mockDeleteNotifications(status = 404, body = expected)

                shouldThrow<HttpClientException> {
                    buildDefaultClient().deleteNotifications(listOf(UUID.randomUUID()))
                }.asClue {
                    it.status shouldBe 404
                    it.body shouldBe expected
                    it.message shouldBe "Got HTTP status 404: $expected"
                }
            }
        }

        "Send message" - {
            "Should make expected calls and parse response" {
                mockSendMessage()

                val accessToken = UUID.randomUUID().toString()
                val helseIdClient = mockHelseIdClient(accessToken)
                val proofBuilder = mockProofBuilder()
                val client = MshInternalClient(
                    baseUrl = wireMock.baseUrl,
                    sourceSystem = UUID.randomUUID().toString(),
                    helseIdClient = helseIdClient,
                    proofBuilder = proofBuilder,
                )
                client.postMessage(PostMessageRequest())
                    .asClue {
                        it shouldBe UUID.fromString("0556bccf-ccf9-4597-9673-14cbd1a96d99")
                    }

                verifySequence {
                    helseIdClient.getAccessToken(StandardAccessTokenRequest(TokenType.DPOP))
                    proofBuilder.buildProof(Endpoint(HttpMethod.POST, "${wireMock.baseUrl}/messages"), null, accessToken)
                }
            }

            "Not accepted response should throw exception" {
                val expected = UUID.randomUUID().toString()
                mockSendMessage(200, expected)

                shouldThrow<HttpException> {
                    buildDefaultClient().postMessage(PostMessageRequest())
                }.asClue {
                    it.status shouldBe 200
                    it.body shouldBe expected
                    it.message shouldBe "Got HTTP status 200 when expecting 202: $expected"
                }
            }

            "Redirect response should throw exception" {
                val expected = UUID.randomUUID().toString()
                mockSendMessage(302, expected)

                shouldThrow<HttpRedirectException> {
                    buildDefaultClient().postMessage(PostMessageRequest())
                }.asClue {
                    it.status shouldBe 302
                    it.body shouldBe expected
                    it.message shouldBe "Got HTTP status 302: $expected"
                }
            }

            "Client error response should throw exception" {
                val expected = UUID.randomUUID().toString()
                mockSendMessage(404, expected)

                shouldThrow<HttpClientException> {
                    buildDefaultClient().postMessage(PostMessageRequest())
                }.asClue {
                    it.status shouldBe 404
                    it.body shouldBe expected
                    it.message shouldBe "Got HTTP status 404: $expected"
                }
            }

            "Server error response should throw exception" {
                val expected = UUID.randomUUID().toString()
                mockSendMessage(504, expected)

                shouldThrow<HttpServerException> {
                    buildDefaultClient().postMessage(PostMessageRequest())
                }.asClue {
                    it.status shouldBe 504
                    it.body shouldBe expected
                    it.message shouldBe "Got HTTP status 504: $expected"
                }
            }
        }

        "Get message" - {
            "Should make expected calls and parse response" {
                val id = UUID.randomUUID()
                mockGetMessage(id)

                val accessToken = UUID.randomUUID().toString()
                val helseIdClient = mockHelseIdClient(accessToken)
                val proofBuilder = mockProofBuilder()
                val client = MshInternalClient(
                    baseUrl = wireMock.baseUrl,
                    sourceSystem = UUID.randomUUID().toString(),
                    helseIdClient = helseIdClient,
                    proofBuilder = proofBuilder,
                )
                client
                    .getMessage(id)
                    .asClue {
                        it.id shouldBe "80383714-f0de-4cf8-900f-c79ba3af028c"
                        it.contentType shouldBe "application/xml"
                        it.receiverHerIds?.first() shouldBe 8143060
                        it.senderHerId shouldBe 8094866
                        it.businessDocumentId shouldBe "c88389b7-f8ad-40b2-81e1-090e715b7530"
                        it.businessDocumentGenDate shouldBe "2025-08-21T11:39:57"
                        it.businessDocumentMsgType shouldBe null
                    }

                verifySequence {
                    helseIdClient.getAccessToken(StandardAccessTokenRequest(TokenType.DPOP))
                    proofBuilder.buildProof(Endpoint(HttpMethod.GET, "${wireMock.baseUrl}/messages/$id"), null, accessToken)
                }
            }

            "Should be able to parse apprec response" {
                val id = UUID.randomUUID()
                mockGetMessage(id, body = readResourceContentAsString("msh/get-message-apprec-response.json"))

                val accessToken = UUID.randomUUID().toString()
                buildDefaultClient(accessToken)
                    .getMessage(id)
                    .asClue {
                        it.id shouldBe "0c8f3d36-f9e7-4e3f-9d5c-c820fa9b5773"
                        it.contentType shouldBe "application/xml"
                        it.receiverHerIds?.first() shouldBe 8143060
                        it.senderHerId shouldBe 8094866
                        it.businessDocumentId shouldBe "9ebaaf44-7317-41b7-892a-2a568acd5111"
                        it.businessDocumentGenDate shouldBe "2025-08-21T11:21:16.0004155"
                        it.businessDocumentMsgType shouldBe "APPREC"
                    }
            }

            "Not OK response should throw exception" {
                val id = UUID.randomUUID()
                val expected = UUID.randomUUID().toString()
                mockGetMessage(id, 201, expected)

                shouldThrow<HttpException> {
                    buildDefaultClient().getMessage(id)
                }.asClue {
                    it.status shouldBe 201
                    it.body shouldBe expected
                    it.message shouldBe "Got HTTP status 201 when expecting 200: $expected"
                }
            }

            "Redirect response should throw exception" {
                val id = UUID.randomUUID()
                val expected = UUID.randomUUID().toString()
                mockGetMessage(id, 307, expected)

                shouldThrow<HttpRedirectException> {
                    buildDefaultClient().getMessage(id)
                }.asClue {
                    it.status shouldBe 307
                    it.body shouldBe expected
                    it.message shouldBe "Got HTTP status 307: $expected"
                }
            }

            "Client error response should throw exception" {
                val id = UUID.randomUUID()
                val expected = UUID.randomUUID().toString()
                mockGetMessage(id, 403, expected)

                shouldThrow<HttpClientException> {
                    buildDefaultClient().getMessage(id)
                }.asClue {
                    it.status shouldBe 403
                    it.body shouldBe expected
                    it.message shouldBe "Got HTTP status 403: $expected"
                }
            }

            "Client server response should throw exception" {
                val id = UUID.randomUUID()
                val expected = UUID.randomUUID().toString()
                mockGetMessage(id, 500, expected)

                shouldThrow<HttpServerException> {
                    buildDefaultClient().getMessage(id)
                }.asClue {
                    it.status shouldBe 500
                    it.body shouldBe expected
                    it.message shouldBe "Got HTTP status 500: $expected"
                }
            }

            "Unknown JSON properties should be ignored" {
                val id = UUID.randomUUID()
                mockGetMessage(id, body = readResourceContentAsString("msh/get-message-response-with-unknown-property.json"))

                val client = buildDefaultClient()
                client
                    .getMessage(id)
                    .asClue {
                        it.id shouldBe "80383714-f0de-4cf8-900f-c79ba3af028c"
                        it.contentType shouldBe "application/xml"
                        it.receiverHerIds?.first() shouldBe 8143060
                        it.senderHerId shouldBe 8094866
                        it.businessDocumentId shouldBe "c88389b7-f8ad-40b2-81e1-090e715b7530"
                        it.businessDocumentGenDate shouldBe "2025-08-21T11:39:57"
                        it.businessDocumentMsgType shouldBe null
                    }
            }
        }

        "Get business document" - {
            "Should make expected calls and parse response" {
                val id = UUID.randomUUID()
                mockGetBusinessDocument(id)

                val accessToken = UUID.randomUUID().toString()
                val helseIdClient = mockHelseIdClient(accessToken)
                val proofBuilder = mockProofBuilder()
                val client = MshInternalClient(
                    baseUrl = wireMock.baseUrl,
                    sourceSystem = UUID.randomUUID().toString(),
                    helseIdClient = helseIdClient,
                    proofBuilder = proofBuilder,
                )
                client
                    .getBusinessDocument(id)
                    .asClue {
                        it.businessDocument shouldBe expectedBusinessDocument("msh/get-business-document-response.json")
                        it.contentType shouldBe "application/xml"
                        it.contentTransferEncoding shouldBe "base64"
                    }

                verifySequence {
                    helseIdClient.getAccessToken(StandardAccessTokenRequest(TokenType.DPOP))
                    proofBuilder.buildProof(
                        Endpoint(HttpMethod.GET, "${wireMock.baseUrl}/messages/$id/business-document"),
                        null,
                        accessToken
                    )
                }
            }

            "Should be able to parse apprec response" {
                val id = UUID.randomUUID()
                mockGetBusinessDocument(id, body = readResourceContentAsString("msh/get-business-document-apprec-response.json"))

                val accessToken = UUID.randomUUID().toString()
               buildDefaultClient(accessToken)
                    .getBusinessDocument(id)
                    .asClue {
                        it.businessDocument shouldBe expectedBusinessDocument("msh/get-business-document-apprec-response.json")
                        it.contentType shouldBe "application/xml"
                        it.contentTransferEncoding shouldBe "base64"
                    }
            }

            "Not OK response should throw exception" {
                val id = UUID.randomUUID()
                val expected = UUID.randomUUID().toString()
                mockGetBusinessDocument(id, 201, expected)

                shouldThrow<HttpException> {
                    buildDefaultClient().getBusinessDocument(id)
                }.asClue {
                    it.status shouldBe 201
                    it.body shouldBe expected
                    it.message shouldBe "Got HTTP status 201 when expecting 200: $expected"
                }
            }

            "Redirect response should throw exception" {
                val id = UUID.randomUUID()
                val expected = UUID.randomUUID().toString()
                mockGetBusinessDocument(id, 302, expected)

                shouldThrow<HttpRedirectException> {
                    buildDefaultClient().getBusinessDocument(id)
                }.asClue {
                    it.status shouldBe 302
                    it.body shouldBe expected
                    it.message shouldBe "Got HTTP status 302: $expected"
                }
            }

            "Client error response should throw exception" {
                val id = UUID.randomUUID()
                val expected = UUID.randomUUID().toString()
                mockGetBusinessDocument(id, 403, expected)

                shouldThrow<HttpClientException> {
                    buildDefaultClient().getBusinessDocument(id)
                }.asClue {
                    it.status shouldBe 403
                    it.body shouldBe expected
                    it.message shouldBe "Got HTTP status 403: $expected"
                }
            }

            "Server error response should throw exception" {
                val id = UUID.randomUUID()
                val expected = UUID.randomUUID().toString()
                mockGetBusinessDocument(id, 503, expected)

                shouldThrow<HttpServerException> {
                    buildDefaultClient().getBusinessDocument(id)
                }.asClue {
                    it.status shouldBe 503
                    it.body shouldBe expected
                    it.message shouldBe "Got HTTP status 503: $expected"
                }
            }
        }

        "Get status" - {
            "Should make expected calls and parse response" {
                val id = UUID.randomUUID()
                mockGetStatus(id)

                val accessToken = UUID.randomUUID().toString()
                val helseIdClient = mockHelseIdClient(accessToken)
                val proofBuilder = mockProofBuilder()
                val client = MshInternalClient(
                    baseUrl = wireMock.baseUrl,
                    sourceSystem = UUID.randomUUID().toString(),
                    helseIdClient = helseIdClient,
                    proofBuilder = proofBuilder,
                )
                client
                    .getStatus(id)
                    .asClue {
                        it.size shouldBe 1
                        with(it.single()) {
                            receiverHerId shouldBe 8094866
                            transportDeliveryState shouldBe NhnDeliveryState.UNCONFIRMED
                            apprecInfo?.appRecStatus shouldBe null
                        }
                    }

                verifySequence {
                    helseIdClient.getAccessToken(StandardAccessTokenRequest(TokenType.DPOP))
                    proofBuilder.buildProof(
                        Endpoint(HttpMethod.GET, "${wireMock.baseUrl}/messages/$id/status"),
                        null,
                        accessToken
                    )
                }
            }

            "Should be able to parse acknowledged response" {
                val id = UUID.randomUUID()
                mockGetStatus(id, body = readResourceContentAsString("msh/get-status-acked.json"))

                val accessToken = UUID.randomUUID().toString()
                buildDefaultClient(accessToken)
                    .getStatus(id)
                    .asClue {
                        it.size shouldBe 1
                        with(it.single()) {
                            receiverHerId shouldBe 8094866
                            transportDeliveryState shouldBe NhnDeliveryState.ACKNOWLEDGED
                            apprecInfo?.appRecStatus shouldBe NhnAppRecStatus.OK
                        }
                    }
            }

            "Not OK response should throw exception" {
                val id = UUID.randomUUID()
                val expected = UUID.randomUUID().toString()
                mockGetStatus(id, 201, expected)

                shouldThrow<HttpException> {
                    buildDefaultClient().getStatus(id)
                }.asClue {
                    it.status shouldBe 201
                    it.body shouldBe expected
                    it.message shouldBe "Got HTTP status 201 when expecting 200: $expected"
                }
            }

            "Redirect response should throw exception" {
                val id = UUID.randomUUID()
                val expected = UUID.randomUUID().toString()
                mockGetStatus(id, 307, expected)

                shouldThrow<HttpRedirectException> {
                    buildDefaultClient().getStatus(id)
                }.asClue {
                    it.status shouldBe 307
                    it.body shouldBe expected
                    it.message shouldBe "Got HTTP status 307: $expected"
                }
            }

            "Client error response should throw exception" {
                val id = UUID.randomUUID()
                val expected = UUID.randomUUID().toString()
                mockGetStatus(id, 400, expected)

                shouldThrow<HttpClientException> {
                    buildDefaultClient().getStatus(id)
                }.asClue {
                    it.status shouldBe 400
                    it.body shouldBe expected
                    it.message shouldBe "Got HTTP status 400: $expected"
                }
            }

            "Server error response should throw exception" {
                val id = UUID.randomUUID()
                val expected = UUID.randomUUID().toString()
                mockGetStatus(id, 504, expected)

                shouldThrow<HttpServerException> {
                    buildDefaultClient().getStatus(id)
                }.asClue {
                    it.status shouldBe 504
                    it.body shouldBe expected
                    it.message shouldBe "Got HTTP status 504: $expected"
                }
            }
        }

        "Post app rec" - {
            "Should make expected calls and parse response" {
                val id = UUID.randomUUID()
                val senderHerId = nextInt(1, 1000000)
                mockPostAppRec(id, senderHerId)

                val accessToken = UUID.randomUUID().toString()
                val helseIdClient = mockHelseIdClient(accessToken)
                val proofBuilder = mockProofBuilder()
                val client = MshInternalClient(
                    baseUrl = wireMock.baseUrl,
                    sourceSystem = UUID.randomUUID().toString(),
                    helseIdClient = helseIdClient,
                    proofBuilder = proofBuilder,
                )
                client
                    .postAppRec(id, PostAppRecRequest().appRecSenderHerId(senderHerId))
                    .asClue {
                        it shouldBe UUID.fromString("680bb102-4541-4ca9-8210-07251b3206a3")
                    }

                verifySequence {
                    helseIdClient.getAccessToken(StandardAccessTokenRequest(TokenType.DPOP))
                    proofBuilder.buildProof(
                        Endpoint(
                            HttpMethod.POST,
                            "${wireMock.baseUrl}/messages/$id/apprec"
                        ), null, accessToken
                    )
                }
            }

            "Not CREATED response should throw exception" {
                val id = UUID.randomUUID()
                val senderHerId = nextInt(1, 1000000)
                val expected = UUID.randomUUID().toString()
                mockPostAppRec(id, senderHerId, 200, expected)

                shouldThrow<HttpException> {
                    buildDefaultClient().postAppRec(id, PostAppRecRequest().appRecSenderHerId(senderHerId))
                }.asClue {
                    it.status shouldBe 200
                    it.body shouldBe expected
                    it.message shouldBe "Got HTTP status 200 when expecting 202: $expected"
                }
            }

            "Redirect response should throw exception" {
                val id = UUID.randomUUID()
                val senderHerId = nextInt(1, 1000000)
                val expected = UUID.randomUUID().toString()
                mockPostAppRec(id, senderHerId, 301, expected)

                shouldThrow<HttpRedirectException> {
                    buildDefaultClient().postAppRec(id, PostAppRecRequest().appRecSenderHerId(senderHerId))
                }.asClue {
                    it.status shouldBe 301
                    it.body shouldBe expected
                    it.message shouldBe "Got HTTP status 301: $expected"
                }
            }

            "Client error response should throw exception" {
                val id = UUID.randomUUID()
                val senderHerId = nextInt(1, 1000000)
                val expected = UUID.randomUUID().toString()
                mockPostAppRec(id, senderHerId, 401, expected)

                shouldThrow<HttpClientException> {
                    buildDefaultClient().postAppRec(id, PostAppRecRequest().appRecSenderHerId(senderHerId))
                }.asClue {
                    it.status shouldBe 401
                    it.body shouldBe expected
                    it.message shouldBe "Got HTTP status 401: $expected"
                }
            }

            "Server error response should throw exception" {
                val id = UUID.randomUUID()
                val senderHerId = nextInt(1, 1000000)
                val expected = UUID.randomUUID().toString()
                mockPostAppRec(id, senderHerId, 500, expected)

                shouldThrow<HttpServerException> {
                    buildDefaultClient().postAppRec(id, PostAppRecRequest().appRecSenderHerId(senderHerId))
                }.asClue {
                    it.status shouldBe 500
                    it.body shouldBe expected
                    it.message shouldBe "Got HTTP status 500: $expected"
                }
            }
        }

        "Mark message downloaded" - {
            "Should make expected calls" {
                val id = UUID.randomUUID()
                val receiverHerId = nextInt(1, 1000000)
                mockMarkMessageDownloaded(id, receiverHerId)

                val accessToken = UUID.randomUUID().toString()
                val helseIdClient = mockHelseIdClient(accessToken)
                val proofBuilder = mockProofBuilder()
                MshInternalClient(
                    baseUrl = wireMock.baseUrl,
                    sourceSystem = UUID.randomUUID().toString(),
                    helseIdClient = helseIdClient,
                    proofBuilder = proofBuilder,
                ).markMessageDownloaded(id, receiverHerId)

                verifySequence {
                    helseIdClient.getAccessToken(StandardAccessTokenRequest(TokenType.DPOP))
                    proofBuilder.buildProof(
                        Endpoint(HttpMethod.PUT, "${wireMock.baseUrl}/messages/$id/downloaded"),
                        null,
                        accessToken
                    )
                }
            }

            "Not NO_CONTENT response should throw exception" {
                val id = UUID.randomUUID()
                val receiverHerId = nextInt(1, 1000000)
                val expected = UUID.randomUUID().toString()
                mockMarkMessageDownloaded(id, receiverHerId, 201, expected)

                shouldThrow<HttpException> {
                    buildDefaultClient().markMessageDownloaded(id, receiverHerId)
                }.asClue {
                    it.status shouldBe 201
                    it.body shouldBe expected
                    it.message shouldBe "Got HTTP status 201 when expecting 204: $expected"
                }
            }

            "Redirect response should throw exception" {
                val id = UUID.randomUUID()
                val receiverHerId = nextInt(1, 1000000)
                val expected = UUID.randomUUID().toString()
                mockMarkMessageDownloaded(id, receiverHerId, 302, expected)

                shouldThrow<HttpRedirectException> {
                    buildDefaultClient().markMessageDownloaded(id, receiverHerId)
                }.asClue {
                    it.status shouldBe 302
                    it.body shouldBe expected
                    it.message shouldBe "Got HTTP status 302: $expected"
                }
            }

            "Client error response should throw exception" {
                val id = UUID.randomUUID()
                val receiverHerId = nextInt(1, 1000000)
                val expected = UUID.randomUUID().toString()
                mockMarkMessageDownloaded(id, receiverHerId, 404, expected)

                shouldThrow<HttpClientException> {
                    buildDefaultClient().markMessageDownloaded(id, receiverHerId)
                }.asClue {
                    it.status shouldBe 404
                    it.body shouldBe expected
                    it.message shouldBe "Got HTTP status 404: $expected"
                }
            }

            "Server error response should throw exception" {
                val id = UUID.randomUUID()
                val receiverHerId = nextInt(1, 1000000)
                val expected = UUID.randomUUID().toString()
                mockMarkMessageDownloaded(id, receiverHerId, 501, expected)

                shouldThrow<HttpServerException> {
                    buildDefaultClient().markMessageDownloaded(id, receiverHerId)
                }.asClue {
                    it.status shouldBe 501
                    it.body shouldBe expected
                    it.message shouldBe "Got HTTP status 501: $expected"
                }
            }
        }

        "MSH configurations" - {
            "Set configurations should make expected calls" {
                val configuration = NhnMshConfiguration()
                    .herId(123456)
                    .receiveNotificationChannel(NhnReceiveNotificationChannel.API)
                    .clientLocked(true)
                    .rejectMessageFilters(
                        NhnMshConfigurationRejectMessageFilters().messageFunction(listOf("DIALOG_HELSEFAGLIG"))
                    )
                mockSetMshConfigurations(
                    expectedBody = NhnSetMshConfigurationsRequest().configurations(listOf(configuration))
                )

                val accessToken = UUID.randomUUID().toString()
                val helseIdClient = mockHelseIdClient(accessToken)
                val proofBuilder = mockProofBuilder()
                MshInternalClient(
                    baseUrl = wireMock.baseUrl,
                    sourceSystem = UUID.randomUUID().toString(),
                    helseIdClient = helseIdClient,
                    proofBuilder = proofBuilder,
                ).setMshConfigurations(listOf(configuration))

                verifySequence {
                    helseIdClient.getAccessToken(StandardAccessTokenRequest(TokenType.DPOP))
                    proofBuilder.buildProof(
                        Endpoint(HttpMethod.PUT, "${wireMock.baseUrl}/mshconfigurations"),
                        null,
                        accessToken
                    )
                }
            }

            "Unexpected response when setting configurations should throw exception" {
                val expected = UUID.randomUUID().toString()
                mockSetMshConfigurations(201, expected)

                shouldThrow<HttpException> {
                    buildDefaultClient().setMshConfigurations(listOf(
                        NhnMshConfiguration().herId(
                            randomHerId()).receiveNotificationChannel(NhnReceiveNotificationChannel.API)
                    ))
                }.asClue {
                    it.status shouldBe 201
                    it.body shouldBe expected
                    it.message shouldBe "Got HTTP status 201 when expecting 204: $expected"
                }
            }

            "Delete configurations should make expected calls" {
                val herIds = listOf(42, 1337)
                mockDeleteMshConfigurations(herIds = herIds)

                val accessToken = UUID.randomUUID().toString()
                val helseIdClient = mockHelseIdClient(accessToken)
                val proofBuilder = mockProofBuilder()
                MshInternalClient(
                    baseUrl = wireMock.baseUrl,
                    sourceSystem = UUID.randomUUID().toString(),
                    helseIdClient = helseIdClient,
                    proofBuilder = proofBuilder,
                ).deleteMshConfigurations(herIds)

                verifySequence {
                    helseIdClient.getAccessToken(StandardAccessTokenRequest(TokenType.DPOP))
                    proofBuilder.buildProof(
                        Endpoint(HttpMethod.DELETE, "${wireMock.baseUrl}/mshconfigurations"),
                        null,
                        accessToken
                    )
                }
            }

            "Client error response when deleting configurations should throw exception" {
                val expected = UUID.randomUUID().toString()
                mockDeleteMshConfigurations(status = 404, body = expected)

                shouldThrow<HttpClientException> {
                    buildDefaultClient().deleteMshConfigurations(listOf(randomHerId()))
                }.asClue {
                    it.status shouldBe 404
                    it.body shouldBe expected
                    it.message shouldBe "Got HTTP status 404: $expected"
                }
            }
        }

        "Test HelseID parameters" - {
            "If no token params are passed, should get token with standard DPoP token request" {
                val id = UUID.randomUUID()
                mockGetMessage(id)

                val helseIdClient = mockHelseIdClient("")
                MshInternalClient(
                    baseUrl = wireMock.baseUrl,
                    sourceSystem = UUID.randomUUID().toString(),
                    helseIdClient = helseIdClient,
                    proofBuilder = mockProofBuilder(),
                ).getMessage(id)

                verifySequence {
                    helseIdClient.getAccessToken(StandardAccessTokenRequest(TokenType.DPOP))
                }
            }

            "Should support single tenant access tokens" {
                val id = UUID.randomUUID()
                mockGetMessage(id)
                val childOrganization = randomOrganizationNumber()

                val helseIdClient = mockHelseIdClient("")
                MshInternalClient(
                    baseUrl = wireMock.baseUrl,
                    sourceSystem = UUID.randomUUID().toString(),
                    helseIdClient = helseIdClient,
                    proofBuilder = mockProofBuilder(),
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

                val helseIdClient = mockHelseIdClient("")
                MshInternalClient(
                    baseUrl = wireMock.baseUrl,
                    sourceSystem = UUID.randomUUID().toString(),
                    helseIdClient = helseIdClient,
                    proofBuilder = mockProofBuilder(),
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

                val helseIdClient = mockHelseIdClient("")
                MshInternalClient(
                    baseUrl = wireMock.baseUrl,
                    sourceSystem = UUID.randomUUID().toString(),
                    helseIdClient = helseIdClient,
                    proofBuilder = mockProofBuilder(),
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
                    proofBuilder = mockProofBuilder(),
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
                    proofBuilder = mockProofBuilder(),
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
                    helseIdClient = mockHelseIdClient(accessToken),
                    proofBuilder = mockk { every { buildProof(any(), any(), any()) } returns dpopProof },
                ).getMessage(id)

                wireMockClient.find(getRequestedFor(urlEqualTo("/messages/$id"))).asClue { requests ->
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
                    proofBuilder = mockProofBuilder(),
                    defaultTokenParams = HelseIdTokenParameters(
                        tenant = staticParams,
                    ),
                )
                requestParameters.forEachIndexed { i, params ->
                    launch(Executors.newFixedThreadPool(8).asCoroutineDispatcher()) {
                        client.getMessage(requestIds[i], RequestParameters(HelseIdTokenParameters(params)))
                    }
                }

                eventually(5.seconds) {
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

    private fun expectedBusinessDocument(resource: String): String =
        objectMapper
            .readTree(readResourceContentAsString(resource))
            .get("businessDocument")
            .asText()

    private fun buildDefaultClient(accessToken: String = UUID.randomUUID().toString()): MshInternalClient = MshInternalClient(
        baseUrl = wireMock.baseUrl,
        sourceSystem = UUID.randomUUID().toString(),
        helseIdClient = mockHelseIdClient(accessToken),
        proofBuilder = mockProofBuilder(),
    )

    private fun mockProofBuilder(): ProofBuilder = mockk<ProofBuilder> { every { buildProof(any(), any(), any()) } returns UUID.randomUUID().toString() }

    private fun mockHelseIdClient(accessToken: String): HelseIdClient = mockk<HelseIdClient> { every { getAccessToken(any()) } returns buildTokenResponse(accessToken) }


    private fun mockGetUnreadNotifications(
        herIds: Collection<Int>,
        notificationsToFetch: Int? = null,
        status: Int = 200,
        body: String = NhnGetUnreadNotificationsResponse()
            .unreadNotifications(
                listOf(
                    NhnUnreadNotification()
                        .notificationId("17aaeaa7-fa1e-4b60-a8e9-bdc31718dfc9")
                        .relatedMessageId("0d5c8d05-ab57-4b2b-b480-c46c69cdf789")
                        .type(NhnNotificationType.NEW_MESSAGE)
                        .notificationReceiverHerId(6789)
                        .notificationTriggeredByHerId(9876)
                        .description("New message received (EPIKRISE).")
                        .createdAt(OffsetDateTime.parse("2026-04-14T06:17:00+00:00")),
                    NhnUnreadNotification()
                        .notificationId("c5e6b600-78ef-4d2a-ad5e-e4b64e3f5230")
                        .relatedMessageId("aa2fb53f-8454-4eed-9c13-30f7e61fee23")
                        .type(NhnNotificationType.REFUSED_MESSAGE)
                        .notificationReceiverHerId(1234)
                        .notificationTriggeredByHerId(5678)
                        .description("Receiver does not support the message type. (EPIKRISE).")
                        .createdAt(OffsetDateTime.parse("2026-04-14T06:15:20+00:00")),
                )
            )
            .toJson(),
    ) {
        wireMockClient.register(
            get(urlPathEqualTo("/notifications/unread"))
                .apply {
                    herIds.forEach { withQueryParam("HerIds", equalTo(it.toString())) }
                    if (notificationsToFetch != null) withQueryParam("NotificationsToFetch", equalTo(notificationsToFetch.toString()))
                }
                .willReturn(
                    status(status)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)
                )
        )
    }

    private fun mockGetMessage(id: UUID, status: Int = 200, body: String = readResourceContentAsString("msh/get-message-response.json")) {
        wireMockClient.register(
            get("/messages/$id")
                .willReturn(
                    status(status)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)
                )
        )
    }

    private fun mockGetBusinessDocument(id: UUID, status: Int = 200, body: String = readResourceContentAsString("msh/get-business-document-response.json")) {
        wireMockClient.register(
            get("/messages/$id/business-document")
                .willReturn(
                    status(status)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)
                )
        )
    }

    private fun mockGetStatus(id: UUID, status: Int = 200, body: String = readResourceContentAsString("msh/get-status.json")) {
        wireMockClient.register(
            get("/messages/$id/status")
                .willReturn(
                    status(status)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)
                )
        )
    }

    private fun  mockSendMessage(status: Int = 202, body: String = readResourceContentAsString("msh/post-message-response.json")) {
        wireMockClient.register(
            post("/messages")
                .willReturn(
                    status(status)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)
                )
        )
    }

    private fun mockPostAppRec(appRecId: UUID, senderHerId: Int, status: Int = 202, body: String = readResourceContentAsString("msh/post-apprec-response.json")) {
        wireMockClient.register(
            post("/messages/$appRecId/apprec")
                .willReturn(
                    status(status)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)
                )
        )
    }

    private fun mockDeleteNotifications(status: Int = 204, body: String = "", expectedBody: NhnDeleteNotificationsRequest? = null) {
        wireMockClient.register(
            post(urlPathEqualTo("/notifications/delete"))
                .apply {
                    if (expectedBody != null) withRequestBody(equalToJson(expectedBody.toJson()))
                }
                .willReturn(
                    status(status)
                        .withBody(body)
                )
        )
    }

    private fun mockMarkMessageDownloaded(markAsReadId: UUID, markAsReadHerId: Int, status: Int = 204, body: String = "") {
        wireMockClient.register(
            put(urlPathEqualTo("/messages/$markAsReadId/downloaded"))
                .withRequestBody(equalToJson(NhnMarkAsDownloadedRequest().receiverHerId(markAsReadHerId).toJson()))
                .willReturn(
                    status(status)
                        .withBody(body)
                )
        )
    }

    private fun mockSetMshConfigurations(status: Int = 204, body: String = "", expectedBody: NhnSetMshConfigurationsRequest? = null) {
        wireMockClient.register(
            put(urlPathEqualTo("/mshconfigurations"))
                .apply {
                    if (expectedBody != null) withRequestBody(equalToJson(expectedBody.toJson()))
                }
                .willReturn(
                    status(status)
                        .withBody(body)
                )
        )
    }

    private fun mockDeleteMshConfigurations(herIds: Collection<Int> = emptyList(), status: Int = 204, body: String = "") {
        wireMockClient.register(
            delete(urlPathEqualTo("/mshconfigurations"))
                .apply {
                    herIds.forEach { herId -> withQueryParam("HerIds", equalTo(herId.toString())) }
                }
                .willReturn(
                    status(status)
                        .withBody(body)
                )
        )
    }

}

private fun buildTokenResponse(accessToken: String) =
    TokenResponse(accessToken, 0, UUID.randomUUID().toString(), UUID.randomUUID().toString())

private fun randomOrganizationNumber() = nextInt(100000000, 1000000000).toString()
