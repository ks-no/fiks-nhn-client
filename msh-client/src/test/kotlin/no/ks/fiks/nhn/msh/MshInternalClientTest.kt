package no.ks.fiks.nhn.msh

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.*
import io.kotest.assertions.asClue
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.beNull
import io.kotest.matchers.should
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
import no.nhn.msh.v2.model.AppRecStatus
import no.nhn.msh.v2.model.DeliveryState
import no.nhn.msh.v2.model.PostAppRecRequest
import no.nhn.msh.v2.model.PostMessageRequest
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

    init {

        "Get app rec info" - {
            "Should make expected calls and parse response" {
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
                    .asClue {
                        it shouldHaveSize 1
                        with(it.single()) {
                            receiverHerId shouldBe 8094866
                            appRecStatus shouldBe AppRecStatus.OK
                            appRecErrorList.shouldBeEmpty()
                        }
                    }

                verifySequence {
                    helseIdClient.getAccessToken(StandardAccessTokenRequest(TokenType.DPOP))
                    proofBuilder.buildProof(
                        Endpoint(HttpMethod.GET, "${wireMock.baseUrl}/Messages/$id/apprec"),
                        null,
                        accessToken
                    )
                }
            }

            "Should handle rejected" {
                val id = UUID.randomUUID()
                mockGetAppRec(id, body = readResourceContentAsString("msh/get-apprecinfo-rejected.json"))

                MshInternalClient(
                    baseUrl = wireMock.baseUrl,
                    sourceSystem = UUID.randomUUID().toString(),
                    helseIdClient = mockk<HelseIdClient> { every { getAccessToken(any()) } returns buildTokenResponse(UUID.randomUUID().toString()) },
                    proofBuilder = mockk<ProofBuilder> { every { buildProof(any(), any(), any()) } returns UUID.randomUUID().toString() },
                ).getAppRecInfo(id)
                    .asClue {
                        it shouldHaveSize 1
                        with(it.single()) {
                            receiverHerId shouldBe 42
                            appRecStatus shouldBe AppRecStatus.REJECTED
                            appRecErrorList shouldHaveSize 1
                            with(appRecErrorList!!.single()) {
                                errorCode shouldBe "E10"
                                details shouldBe "Ugyldig meldingsidentifikator"
                            }
                        }
                    }
            }

            "Should handle ok error in message part" {
                val id = UUID.randomUUID()
                mockGetAppRec(id, body = readResourceContentAsString("msh/get-apprecinfo-okerrorinmessagepart.json"))

                MshInternalClient(
                    baseUrl = wireMock.baseUrl,
                    sourceSystem = UUID.randomUUID().toString(),
                    helseIdClient = mockk<HelseIdClient> { every { getAccessToken(any()) } returns buildTokenResponse(UUID.randomUUID().toString()) },
                    proofBuilder = mockk<ProofBuilder> { every { buildProof(any(), any(), any()) } returns UUID.randomUUID().toString() },
                ).getAppRecInfo(id)
                    .asClue {
                        it shouldHaveSize 1
                        with(it.single()) {
                            receiverHerId shouldBe 123456
                            appRecStatus shouldBe AppRecStatus.OK_ERROR_IN_MESSAGE_PART
                            appRecErrorList shouldHaveSize 2
                            with(appRecErrorList!![0]) {
                                errorCode shouldBe "E31"
                                details shouldBe "Pasientens fødselsnummer er feil"
                            }
                            with(appRecErrorList!![1]) {
                                errorCode shouldBe "E20"
                                details shouldBe "Noe gikk galt"
                            }
                        }
                    }
            }

            "Should handle multiple apprecs in same response" {
                val id = UUID.randomUUID()
                mockGetAppRec(id, body = readResourceContentAsString("msh/get-apprecinfo-multi.json"))

                MshInternalClient(
                    baseUrl = wireMock.baseUrl,
                    sourceSystem = UUID.randomUUID().toString(),
                    helseIdClient = mockk<HelseIdClient> { every { getAccessToken(any()) } returns buildTokenResponse(UUID.randomUUID().toString()) },
                    proofBuilder = mockk<ProofBuilder> { every { buildProof(any(), any(), any()) } returns UUID.randomUUID().toString() },
                ).getAppRecInfo(id)
                    .asClue {
                        it shouldHaveSize 2
                        with(it[0]) {
                            receiverHerId shouldBe 1111
                            appRecStatus shouldBe AppRecStatus.REJECTED
                            appRecErrorList shouldHaveSize 1
                            with(appRecErrorList!!.single()) {
                                errorCode shouldBe "E10"
                                details shouldBe "Ugyldig meldingsidentifikator"
                            }
                        }
                        with(it[1]) {
                            receiverHerId shouldBe 2222
                            appRecStatus shouldBe AppRecStatus.OK
                            appRecErrorList shouldHaveSize 0
                        }
                    }
            }

            "Not OK response should throw exception" {
                val id = UUID.randomUUID()
                val expected = UUID.randomUUID().toString()
                mockGetAppRec(id, 500, expected)

                shouldThrow<HttpServerException> {
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
            "Should make expected calls and parse response" {
                val herId = randomHerId()
                mockGetMessages(herId)

                val accessToken = UUID.randomUUID().toString()
                val helseIdClient = mockk<HelseIdClient> { every { getAccessToken(any()) } returns buildTokenResponse(accessToken) }
                val proofBuilder = mockk<ProofBuilder> { every { buildProof(any(), any(), any()) } returns UUID.randomUUID().toString() }
                val client = MshInternalClient(
                    baseUrl = wireMock.baseUrl,
                    sourceSystem = UUID.randomUUID().toString(),
                    helseIdClient = helseIdClient,
                    proofBuilder = proofBuilder,
                )
                client
                    .getMessages(herId)
                    .asClue {
                        it.size shouldBe 2
                        with(it[0]) {
                            id shouldBe UUID.fromString("0c8f3d36-f9e7-4e3f-9d5c-c820fa9b5773")
                            receiverHerId shouldBe 8143060
                        }
                        with(it[1]) {
                            id shouldBe UUID.fromString("99607d81-18bd-45ee-a96f-85e3687a5e05")
                            receiverHerId shouldBe 8143060
                        }
                    }

                verifySequence {
                    helseIdClient.getAccessToken(StandardAccessTokenRequest(TokenType.DPOP))
                    proofBuilder.buildProof(Endpoint(HttpMethod.GET, "${wireMock.baseUrl}/Messages"), null, accessToken)
                }
            }

            "Not OK response should throw exception" {
                val receiverHerId = randomHerId()
                val expected = UUID.randomUUID().toString()
                mockGetMessages(receiverHerId, 400, expected)

                shouldThrow<HttpClientException> {
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

            "Redirect response should throw exception" {
                val receiverHerId = randomHerId()
                val expected = UUID.randomUUID().toString()
                mockGetMessages(receiverHerId, 301, expected)

                shouldThrow<HttpRedirectException> {
                    MshInternalClient(
                        baseUrl = wireMock.baseUrl,
                        sourceSystem = UUID.randomUUID().toString(),
                        helseIdClient = mockk<HelseIdClient> { every { getAccessToken(any()) } returns buildTokenResponse(UUID.randomUUID().toString()) },
                        proofBuilder = mockk<ProofBuilder> { every { buildProof(any(), any(), any()) } returns UUID.randomUUID().toString() },
                    ).getMessages(receiverHerId)
                }.asClue {
                    it.status shouldBe 301
                    it.body shouldBe expected
                    it.message shouldBe "Got HTTP status 301: $expected"
                }
            }
        }

        "Send message" - {
            "Should make expected calls and parse response" {
                mockSendMessage()

                val accessToken = UUID.randomUUID().toString()
                val helseIdClient = mockk<HelseIdClient> { every { getAccessToken(any()) } returns buildTokenResponse(accessToken) }
                val proofBuilder = mockk<ProofBuilder> { every { buildProof(any(), any(), any()) } returns UUID.randomUUID().toString() }
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
                    proofBuilder.buildProof(Endpoint(HttpMethod.POST, "${wireMock.baseUrl}/Messages"), null, accessToken)
                }
            }

            "Not OK response should throw exception" {
                val expected = UUID.randomUUID().toString()
                mockSendMessage(401, expected)

                shouldThrow<HttpClientException> {
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
            "Should make expected calls and parse response" {
                val id = UUID.randomUUID()
                mockGetMessage(id)

                val accessToken = UUID.randomUUID().toString()
                val helseIdClient = mockk<HelseIdClient> { every { getAccessToken(any()) } returns buildTokenResponse(accessToken) }
                val proofBuilder = mockk<ProofBuilder> { every { buildProof(any(), any(), any()) } returns UUID.randomUUID().toString() }
                val client = MshInternalClient(
                    baseUrl = wireMock.baseUrl,
                    sourceSystem = UUID.randomUUID().toString(),
                    helseIdClient = helseIdClient,
                    proofBuilder = proofBuilder,
                )
                client
                    .getMessage(id)
                    .asClue {
                        it.id shouldBe UUID.fromString("80383714-f0de-4cf8-900f-c79ba3af028c")
                        it.contentType shouldBe "application/xml"
                        it.receiverHerId shouldBe 8143060
                        it.senderHerId shouldBe 8094866
                        it.businessDocumentId shouldBe "c88389b7-f8ad-40b2-81e1-090e715b7530"
                        it.businessDocumentGenDate shouldBe OffsetDateTime.parse("2025-08-21T11:39:57Z")
                        it.isAppRec shouldBe false
                    }

                verifySequence {
                    helseIdClient.getAccessToken(StandardAccessTokenRequest(TokenType.DPOP))
                    proofBuilder.buildProof(Endpoint(HttpMethod.GET, "${wireMock.baseUrl}/Messages/$id"), null, accessToken)
                }
            }

            "Should be able to parse apprec response" {
                val id = UUID.randomUUID()
                mockGetMessage(id, body = readResourceContentAsString("msh/get-message-apprec-response.json"))

                val accessToken = UUID.randomUUID().toString()
                val client = MshInternalClient(
                    baseUrl = wireMock.baseUrl,
                    sourceSystem = UUID.randomUUID().toString(),
                    helseIdClient = mockk<HelseIdClient> { every { getAccessToken(any()) } returns buildTokenResponse(accessToken) },
                    proofBuilder = mockk<ProofBuilder> { every { buildProof(any(), any(), any()) } returns UUID.randomUUID().toString() },
                )
                client
                    .getMessage(id)
                    .asClue {
                        it.id shouldBe UUID.fromString("0c8f3d36-f9e7-4e3f-9d5c-c820fa9b5773")
                        it.contentType shouldBe "application/xml"
                        it.receiverHerId shouldBe 8143060
                        it.senderHerId shouldBe 8094866
                        it.businessDocumentId shouldBe "9ebaaf44-7317-41b7-892a-2a568acd5111"
                        it.businessDocumentGenDate shouldBe OffsetDateTime.parse("2025-08-21T11:21:16.0004155Z")
                        it.isAppRec shouldBe true
                    }
            }

            "Not OK response should throw exception" {
                val id = UUID.randomUUID()
                val expected = UUID.randomUUID().toString()
                mockGetMessage(id, 403, expected)

                shouldThrow<HttpClientException> {
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

            "Unknown JSON properties should be ignored" {
                val id = UUID.randomUUID()
                mockGetMessage(id, body = readResourceContentAsString("msh/get-message-response-with-unknown-property.json"))

                val client = MshInternalClient(
                    baseUrl = wireMock.baseUrl,
                    sourceSystem = UUID.randomUUID().toString(),
                    helseIdClient = mockk<HelseIdClient> { every { getAccessToken(any()) } returns buildTokenResponse(UUID.randomUUID().toString()) },
                    proofBuilder = mockk<ProofBuilder> { every { buildProof(any(), any(), any()) } returns UUID.randomUUID().toString() },
                )
                client
                    .getMessage(id)
                    .asClue {
                        it.id shouldBe UUID.fromString("80383714-f0de-4cf8-900f-c79ba3af028c")
                        it.contentType shouldBe "application/xml"
                        it.receiverHerId shouldBe 8143060
                        it.senderHerId shouldBe 8094866
                        it.businessDocumentId shouldBe "c88389b7-f8ad-40b2-81e1-090e715b7530"
                        it.businessDocumentGenDate shouldBe OffsetDateTime.parse("2025-08-21T11:39:57Z")
                        it.isAppRec shouldBe false
                    }
            }
        }

        "Get business document" - {
            "Should make expected calls and parse response" {
                val id = UUID.randomUUID()
                mockGetBusinessDocument(id)

                val accessToken = UUID.randomUUID().toString()
                val helseIdClient = mockk<HelseIdClient> { every { getAccessToken(any()) } returns buildTokenResponse(accessToken) }
                val proofBuilder = mockk<ProofBuilder> { every { buildProof(any(), any(), any()) } returns UUID.randomUUID().toString() }
                val client = MshInternalClient(
                    baseUrl = wireMock.baseUrl,
                    sourceSystem = UUID.randomUUID().toString(),
                    helseIdClient = helseIdClient,
                    proofBuilder = proofBuilder,
                )
                client
                    .getBusinessDocument(id)
                    .asClue {
                        it.businessDocument shouldBe "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0idXRmLTgiPz4KPCEtLSBERVRURSBFUiBFTiBURVNUTUVMRElORyBNRUQgRklLVElWRSBQRVJTT05EQVRBIC0tPgo8IS0tIEVrc2VtcGVsIHDDpSBoZWxzZWZhZ2xpZyBkaWFsb2cgZXR0ZXJzZW5kaW5nIGF2IGluZm9ybWFzam9uIHZlZHIgaGVudmlzbmluZyAtLT4KPE1zZ0hlYWQgeG1sbnM9Imh0dHA6Ly93d3cua2l0aC5uby94bWxzdGRzL21zZ2hlYWQvMjAwNi0wNS0yNCIgeG1sbnM6eHNkPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxL1hNTFNjaGVtYS54c2QiIHhtbG5zOmZrMT0iaHR0cDovL3d3dy5raXRoLm5vL3htbHN0ZHMvZmVsbGVza29tcG9uZW50MSIgeG1sbnM6eHNpPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxL1hNTFNjaGVtYS1pbnN0YW5jZSIgeHNpOnNjaGVtYUxvY2F0aW9uPSJodHRwOi8vd3d3LmtpdGgubm8veG1sc3Rkcy9tc2doZWFkLzIwMDYtMDUtMjQgTXNnSGVhZC12MV8yLnhzZCI+CiAgPE1zZ0luZm8+CiAgICA8VHlwZSBWPSJESUFMT0dfSEVMU0VGQUdMSUciIEROPSJIZWxzZWZhZ2xpZyBkaWFsb2ciIC8+CiAgICA8TUlHdmVyc2lvbj52MS4yIDIwMDYtMDUtMjQ8L01JR3ZlcnNpb24+CiAgICA8R2VuRGF0ZT4yMDI1LTA4LTIxVDExOjM5OjU3PC9HZW5EYXRlPgogICAgPE1zZ0lkPmM4ODM4OWI3LWY4YWQtNDBiMi04MWUxLTA5MGU3MTViNzUzMDwvTXNnSWQ+CiAgICA8UHJvY2Vzc2luZ1N0YXR1cyBWPSJEIiBETj0iRGVidWdnaW5nIiAvPgogICAgPFNlbmRlcj4KICAgICAgPE9yZ2FuaXNhdGlvbj4KICAgICAgICA8T3JnYW5pc2F0aW9uTmFtZT5OT1JTSyBIRUxTRU5FVFQgU0Y8L09yZ2FuaXNhdGlvbk5hbWU+CiAgICAgICAgPElkZW50PgogICAgICAgICAgPElkPjExMjM3NDwvSWQ+CiAgICAgICAgICA8VHlwZUlkIFY9IkhFUiIgRE49IkhFUi1pZCIgUz0iMi4xNi41NzguMS4xMi40LjEuMS45MDUxIiAvPgogICAgICAgIDwvSWRlbnQ+CiAgICAgICAgPE9yZ2FuaXNhdGlvbj4KICAgICAgICAgIDxPcmdhbmlzYXRpb25OYW1lPk1lbGRpbmdzdmFsaWRlcmluZzwvT3JnYW5pc2F0aW9uTmFtZT4KICAgICAgICAgIDxJZGVudD4KICAgICAgICAgICAgPElkPjgwOTQ4NjY8L0lkPgogICAgICAgICAgICA8VHlwZUlkIFY9IkhFUiIgRE49IkhFUi1pZCIgUz0iMi4xNi41NzguMS4xMi40LjEuMS45MDUxIiAvPgogICAgICAgICAgPC9JZGVudD4KICAgICAgICA8L09yZ2FuaXNhdGlvbj4KICAgICAgPC9PcmdhbmlzYXRpb24+CiAgICA8L1NlbmRlcj4KICAgIDxSZWNlaXZlcj4KICAgICAgPE9yZ2FuaXNhdGlvbj4KICAgICAgICA8T3JnYW5pc2F0aW9uTmFtZT5LUy1ESUdJVEFMRSBGRUxMRVNUSkVORVNURVIgQVM8L09yZ2FuaXNhdGlvbk5hbWU+CiAgICAgICAgPElkZW50PgogICAgICAgICAgPElkPjgxNDI5ODc8L0lkPgogICAgICAgICAgPFR5cGVJZCBWPSJIRVIiIEROPSJIRVItaWQiIFM9IjIuMTYuNTc4LjEuMTIuNC4xLjEuOTA1MSIgLz4KICAgICAgICA8L0lkZW50PgogICAgICAgIDxPcmdhbmlzYXRpb24+CiAgICAgICAgICA8T3JnYW5pc2F0aW9uTmFtZT5TYWtzYmVoYW5kbGluZyBwYXNpZW50b3BwbHlzbmluZ2VyPC9PcmdhbmlzYXRpb25OYW1lPgogICAgICAgICAgPElkZW50PgogICAgICAgICAgICA8SWQ+ODE0MzA2MDwvSWQ+CiAgICAgICAgICAgIDxUeXBlSWQgUz0iMi4xNi41NzguMS4xMi40LjEuMS45MDUxIiBWPSJIRVIiIEROPSJIRVItaWQiIC8+CiAgICAgICAgICA8L0lkZW50PgogICAgICAgIDwvT3JnYW5pc2F0aW9uPgogICAgICA8L09yZ2FuaXNhdGlvbj4KICAgIDwvUmVjZWl2ZXI+CiAgICA8UGF0aWVudD4KICAgICAgPEZhbWlseU5hbWU+TEVORVNUT0w8L0ZhbWlseU5hbWU+CiAgICAgIDxHaXZlbk5hbWU+QlJVTjwvR2l2ZW5OYW1lPgogICAgICA8RGF0ZU9mQmlydGg+MjAwMi0wNy0xNTwvRGF0ZU9mQmlydGg+CiAgICAgIDxJZGVudD4KICAgICAgICA8SWQ+MTU3MjAyNTUxNzg8L0lkPgogICAgICAgIDxUeXBlSWQgVj0iRk5SIiBETj0iRsO4ZHNlbHNudW1tZXIiIFM9IjIuMTYuNTc4LjEuMTIuNC4xLjEuODExNiIgLz4KICAgICAgPC9JZGVudD4KICAgIDwvUGF0aWVudD4KICA8L01zZ0luZm8+CiAgPERvY3VtZW50PgogICAgPFJlZkRvYz4KICAgICAgPElzc3VlRGF0ZSBWPSIyMDI1LTA4LTIxVDExOjM5OjU3IiAvPgogICAgICA8TXNnVHlwZSBWPSJYTUwiIEROPSJYTUwtaW5zdGFucyIgLz4KICAgICAgPENvbnRlbnQ+CiAgICAgICAgPERpYWxvZ21lbGRpbmcgeG1sbnM9Imh0dHA6Ly93d3cua2l0aC5uby94bWxzdGRzL2RpYWxvZy8yMDEzLTAxLTIzIiB4bWxuczp4c2Q9Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvWE1MU2NoZW1hLnhzZCIgeG1sbnM6eHNpPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxL1hNTFNjaGVtYS1pbnN0YW5jZSIgeHNpOnNjaGVtYUxvY2F0aW9uPSJodHRwOi8vd3d3LmtpdGgubm8veG1sc3Rkcy9kaWFsb2cvMjAxMy0wMS0yMyBkaWFsb2dtZWxkaW5nLXYxLjEueHNkIj4KICAgICAgICAgIDxOb3RhdD4KICAgICAgICAgICAgPFRlbWFLb2RldCBWPSI4IiBETj0iRm9yZXNww7hyc2VsIG9tIGhlbHNlb3BwbHlzbmluZ2VyIiBTPSIyLjE2LjU3OC4xLjEyLjQuMS4xLjczMjIiIC8+CiAgICAgICAgICAgIDxUZW1hPkVLRyB0YXR0IGkgZGFnPC9UZW1hPgogICAgICAgICAgICA8VGVrc3ROb3RhdElubmhvbGQ+UGFzaWVudGVuIGVyIGhlbnZpc3QgdGlsIGthcmRpb2xvZ2lzayBwb2xpa2xpbmlrayBmb3IgdXRyZWRuaW5nIG1lZCBzcMO4cnNtw6VsIG9tIGNhcmRpYWwgw6Vyc2FrIG9nIG11bGlnIGFuZ2luYSBwZWN0b3Jpcy4gRXR0ZXJzZW5kZXIgRUtHIHRhdHQgaSBkYWcgdXRlbiBmdW5uLiBTZSB2ZWRsZWdnLjwvVGVrc3ROb3RhdElubmhvbGQ+CiAgICAgICAgICAgIDxSb2xsZXJSZWxhdGVydE5vdGF0PgogICAgICAgICAgICAgIDxSb2xlVG9QYXRpZW50IEROPSJGYXN0bGVnZSIgVj0iNiIgUz0iMi4xNi41NzguMS4xMi40LjEuMS45MDM0IiAvPgogICAgICAgICAgICAgIDxIZWFsdGhjYXJlUHJvZmVzc2lvbmFsPgogICAgICAgICAgICAgICAgPEZhbWlseU5hbWU+TGVnZTwvRmFtaWx5TmFtZT4KICAgICAgICAgICAgICAgIDxHaXZlbk5hbWU+S3Vuc3RpZzwvR2l2ZW5OYW1lPgogICAgICAgICAgICAgICAgPElkZW50PgogICAgICAgICAgICAgICAgICA8ZmsxOklkPjkxNDQ5MDA8L2ZrMTpJZD4KICAgICAgICAgICAgICAgICAgPGZrMTpUeXBlSWQgVj0iSFBSIiBETj0iSFBSLW51bW1lciIgUz0iMi4xNi41NzguMS4xMi40LjEuMS44MTE2IiAvPgogICAgICAgICAgICAgICAgPC9JZGVudD4KICAgICAgICAgICAgICAgIDxUZWxlQ29tPgogICAgICAgICAgICAgICAgICA8ZmsxOlRlbGVBZGRyZXNzIFY9InRlbDoxMjM0NTY3OCI+PC9mazE6VGVsZUFkZHJlc3M+CiAgICAgICAgICAgICAgICA8L1RlbGVDb20+CiAgICAgICAgICAgICAgPC9IZWFsdGhjYXJlUHJvZmVzc2lvbmFsPgogICAgICAgICAgICA8L1JvbGxlclJlbGF0ZXJ0Tm90YXQ+CiAgICAgICAgICA8L05vdGF0PgogICAgICAgIDwvRGlhbG9nbWVsZGluZz4KICAgICAgPC9Db250ZW50PgogICAgPC9SZWZEb2M+CiAgPC9Eb2N1bWVudD4KICA8RG9jdW1lbnQ+CiAgICA8UmVmRG9jPgogICAgICA8SXNzdWVEYXRlIFY9IjIwMjUtMDgtMjFUMTE6Mzk6NTcuNjgxNTE0MVoiIC8+CiAgICAgIDxNc2dUeXBlIFY9IkEiIEROPSJWZWRsZWdnIiAvPgogICAgICA8TWltZVR5cGU+YXBwbGljYXRpb24vcGRmPC9NaW1lVHlwZT4KICAgICAgPERlc2NyaXB0aW9uPnNtYWxsLnBkZjwvRGVzY3JpcHRpb24+CiAgICAgIDxDb250ZW50PgogICAgICAgIDxCYXNlNjRDb250YWluZXIgeG1sbnM6eHNpPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxL1hNTFNjaGVtYWluc3RhbmNlIiB4bWxucz0iaHR0cDovL3d3dy5raXRoLm5vL3htbHN0ZHMvYmFzZTY0Y29udGFpbmVyIj5KVkJFUmkweExqSWdDaVhpNDgvVENpQUtPU0F3SUc5aWFnbzhQQW92VEdWdVozUm9JREV3SURBZ1Vnb3ZSbWxzZEdWeUlDOUdiR0YwWlVSbFkyOWtaU0FLUGo0S2MzUnlaV0Z0Q2tpSnpaRFJTc013RklhZklPL3dlNmV5WnVja1RaUHRidElXQmkwVWpZS1FHeEZiSm1wbGl1TGIyNlFNOFg2Q0pCZkp5Zjk5eWNtRkY2eEphZ1dyck14endKZUNFTWQrZ0ZqV0JDMWRMUGVDSkZrYmwvZlRLZnduVHF0MUNLMHhJWnlFd0ZZWjJUK2Z3VDhLbm1JeFVtSmluTktKeVVpeVc3bVpWRVE2STU0bTJLM1p6Rml1cHZnUGFlZTdKSEZ1WnF5RHZ4dUdCYlpkdThEMXkrN2pZZisyZS8vQzJLT0ptOWR4ZkVxcVRITVJYWmxSMGhSSnVLd1phdTZFSmErTU9kanBZTi9ncHJxOHhWVzdhUnAwWlkxNjJ5U2JrdG9XdnhwUFpVTEd4SkxTcitHNFV1WCtRSHJjbC9yei8yZXF2UGdHUFBXaHFncGxibVJ6ZEhKbFlXMEtaVzVrYjJKcUNqRXdJREFnYjJKcUNqSTBOZ3BsYm1Sdlltb0tOQ0F3SUc5aWFnbzhQQW92Vkhsd1pTQXZVR0ZuWlFvdlVHRnlaVzUwSURVZ01DQlNDaTlTWlhOdmRYSmpaWE1nUER3S0wwWnZiblFnUER3S0wwWXdJRFlnTUNCU0lBb3ZSakVnTnlBd0lGSWdDajQrQ2k5UWNtOWpVMlYwSURJZ01DQlNDajQrQ2k5RGIyNTBaVzUwY3lBNUlEQWdVZ28rUGdwbGJtUnZZbW9LTmlBd0lHOWlhZ284UEFvdlZIbHdaU0F2Um05dWRBb3ZVM1ZpZEhsd1pTQXZWSEoxWlZSNWNHVUtMMDVoYldVZ0wwWXdDaTlDWVhObFJtOXVkQ0F2UVhKcFlXd0tMMFZ1WTI5a2FXNW5JQzlYYVc1QmJuTnBSVzVqYjJScGJtY0tQajRLWlc1a2IySnFDamNnTUNCdlltb0tQRHdLTDFSNWNHVWdMMFp2Ym5RS0wxTjFZblI1Y0dVZ0wxUnlkV1ZVZVhCbENpOU9ZVzFsSUM5R01Rb3ZRbUZ6WlVadmJuUWdMMEp2YjJ0QmJuUnBjWFZoTEVKdmJHUUtMMFpwY25OMFEyaGhjaUF6TVFvdlRHRnpkRU5vWVhJZ01qVTFDaTlYYVdSMGFITWdXeUEzTlRBZ01qVXdJREkzT0NBME1ESWdOakEySURVd01DQTRPRGtnT0RNeklESXlOeUF6TXpNZ016TXpJRFEwTkNBMk1EWWdNalV3SURNek15QXlOVEFnQ2pJNU5pQTFNREFnTlRBd0lEVXdNQ0ExTURBZ05UQXdJRFV3TUNBMU1EQWdOVEF3SURVd01DQTFNREFnTWpVd0lESTFNQ0EyTURZZ05qQTJJRFl3TmlBS05EUTBJRGMwTnlBM056Z2dOalkzSURjeU1pQTRNek1nTmpFeElEVTFOaUE0TXpNZ09ETXpJRE00T1NBek9Ea2dOemM0SURZeE1TQXhNREF3SURnek15QUtPRE16SURZeE1TQTRNek1nTnpJeUlEWXhNU0EyTmpjZ056YzRJRGMzT0NBeE1EQXdJRFkyTnlBMk5qY2dOalkzSURNek15QTJNRFlnTXpNeklEWXdOaUFLTlRBd0lETXpNeUExTURBZ05qRXhJRFEwTkNBMk1URWdOVEF3SURNNE9TQTFOVFlnTmpFeElETXpNeUF6TXpNZ05qRXhJRE16TXlBNE9Ea2dOakV4SUFvMU5UWWdOakV4SURZeE1TQXpPRGtnTkRRMElETXpNeUEyTVRFZ05UVTJJRGd6TXlBMU1EQWdOVFUySURVd01DQXpNVEFnTmpBMklETXhNQ0EyTURZZ0NqYzFNQ0ExTURBZ056VXdJRE16TXlBMU1EQWdOVEF3SURFd01EQWdOVEF3SURVd01DQXpNek1nTVRBd01DQTJNVEVnTXpnNUlERXdNREFnTnpVd0lEYzFNQ0FLTnpVd0lEYzFNQ0F5TnpnZ01qYzRJRFV3TUNBMU1EQWdOakEySURVd01DQXhNREF3SURNek15QTVPVGdnTkRRMElETTRPU0E0TXpNZ056VXdJRGMxTUNBS05qWTNJREkxTUNBeU56Z2dOVEF3SURVd01DQTJNRFlnTlRBd0lEWXdOaUExTURBZ016TXpJRGMwTnlBME16Z2dOVEF3SURZd05pQXpNek1nTnpRM0lBbzFNREFnTkRBd0lEVTBPU0F6TmpFZ016WXhJRE16TXlBMU56WWdOalF4SURJMU1DQXpNek1nTXpZeElEUTRPQ0ExTURBZ09EZzVJRGc1TUNBNE9Ea2dDalEwTkNBM056Z2dOemM0SURjM09DQTNOemdnTnpjNElEYzNPQ0F4TURBd0lEY3lNaUEyTVRFZ05qRXhJRFl4TVNBMk1URWdNemc1SURNNE9TQXpPRGtnQ2pNNE9TQTRNek1nT0RNeklEZ3pNeUE0TXpNZ09ETXpJRGd6TXlBNE16TWdOakEySURnek15QTNOemdnTnpjNElEYzNPQ0EzTnpnZ05qWTNJRFl4TVNBS05qRXhJRFV3TUNBMU1EQWdOVEF3SURVd01DQTFNREFnTlRBd0lEYzNPQ0EwTkRRZ05UQXdJRFV3TUNBMU1EQWdOVEF3SURNek15QXpNek1nTXpNeklBb3pNek1nTlRVMklEWXhNU0ExTlRZZ05UVTJJRFUxTmlBMU5UWWdOVFUySURVME9TQTFOVFlnTmpFeElEWXhNU0EyTVRFZ05qRXhJRFUxTmlBMk1URWdDalUxTmlCZENpOUZibU52WkdsdVp5QXZWMmx1UVc1emFVVnVZMjlrYVc1bkNpOUdiMjUwUkdWelkzSnBjSFJ2Y2lBNElEQWdVZ28rUGdwbGJtUnZZbW9LT0NBd0lHOWlhZ284UEFvdlZIbHdaU0F2Um05dWRFUmxjMk55YVhCMGIzSUtMMFp2Ym5ST1lXMWxJQzlDYjI5clFXNTBhWEYxWVN4Q2IyeGtDaTlHYkdGbmN5QXhOalF4T0FvdlJtOXVkRUpDYjNnZ1d5QXRNalV3SUMweU5qQWdNVEl6TmlBNU16QWdYUW92VFdsemMybHVaMWRwWkhSb0lEYzFNQW92VTNSbGJWWWdNVFEyQ2k5VGRHVnRTQ0F4TkRZS0wwbDBZV3hwWTBGdVoyeGxJREFLTDBOaGNFaGxhV2RvZENBNU16QUtMMWhJWldsbmFIUWdOalV4Q2k5QmMyTmxiblFnT1RNd0NpOUVaWE5qWlc1MElESTJNQW92VEdWaFpHbHVaeUF5TVRBS0wwMWhlRmRwWkhSb0lERXdNekFLTDBGMloxZHBaSFJvSURRMk1BbytQZ3BsYm1Sdlltb0tNaUF3SUc5aWFncGJJQzlRUkVZZ0wxUmxlSFFnSUYwS1pXNWtiMkpxQ2pVZ01DQnZZbW9LUER3S0wwdHBaSE1nV3pRZ01DQlNJRjBLTDBOdmRXNTBJREVLTDFSNWNHVWdMMUJoWjJWekNpOU5aV1JwWVVKdmVDQmJJREFnTUNBMk1USWdOemt5SUYwS1BqNEtaVzVrYjJKcUNqRWdNQ0J2WW1vS1BEd0tMME55WldGMGIzSWdLREUzTWpVdVptMHBDaTlEY21WaGRHbHZia1JoZEdVZ0tERXRTbUZ1TFRNZ01UZzZNVFZRVFNrS0wxUnBkR3hsSUNneE56STFMbEJFUmlrS0wwRjFkR2h2Y2lBb1ZXNXJibTkzYmlrS0wxQnliMlIxWTJWeUlDaEJZM0p2WW1GMElGQkVSbGR5YVhSbGNpQXpMakF5SUdadmNpQlhhVzVrYjNkektRb3ZTMlY1ZDI5eVpITWdLQ2tLTDFOMVltcGxZM1FnS0NrS1BqNEtaVzVrYjJKcUNqTWdNQ0J2WW1vS1BEd0tMMUJoWjJWeklEVWdNQ0JTQ2k5VWVYQmxJQzlEWVhSaGJHOW5DaTlFWldaaGRXeDBSM0poZVNBeE1TQXdJRklLTDBSbFptRjFiSFJTUjBJZ0lERXlJREFnVWdvK1BncGxibVJ2WW1vS01URWdNQ0J2WW1vS1d5OURZV3hIY21GNUNqdzhDaTlYYUdsMFpWQnZhVzUwSUZzd0xqazFNRFVnTVNBeExqQTRPVEVnWFFvdlIyRnRiV0VnTUM0eU5EWTRJQW8rUGdwZENtVnVaRzlpYWdveE1pQXdJRzlpYWdwYkwwTmhiRkpIUWdvOFBBb3ZWMmhwZEdWUWIybHVkQ0JiTUM0NU5UQTFJREVnTVM0d09Ea3hJRjBLTDBkaGJXMWhJRnN3TGpJME5qZ2dNQzR5TkRZNElEQXVNalEyT0NCZENpOU5ZWFJ5YVhnZ1d6QXVORE0yTVNBd0xqSXlNalVnTUM0d01UTTVJREF1TXpnMU1TQXdMamN4TmprZ01DNHdPVGN4SURBdU1UUXpNU0F3TGpBMk1EWWdNQzQzTVRReElGMEtQajRLWFFwbGJtUnZZbW9LZUhKbFpnb3dJREV6Q2pBd01EQXdNREF3TURBZ05qVTFNelVnWmdvd01EQXdNREF5TVRjeUlEQXdNREF3SUc0S01EQXdNREF3TWpBME5pQXdNREF3TUNCdUNqQXdNREF3TURJek5qTWdNREF3TURBZ2Jnb3dNREF3TURBd016YzFJREF3TURBd0lHNEtNREF3TURBd01qQTRNQ0F3TURBd01DQnVDakF3TURBd01EQTFNVGdnTURBd01EQWdiZ293TURBd01EQXdOak16SURBd01EQXdJRzRLTURBd01EQXdNVGMyTUNBd01EQXdNQ0J1Q2pBd01EQXdNREF3TWpFZ01EQXdNREFnYmdvd01EQXdNREF3TXpVeUlEQXdNREF3SUc0S01EQXdNREF3TWpRMk1DQXdNREF3TUNCdUNqQXdNREF3TURJMU5EZ2dNREF3TURBZ2JncDBjbUZwYkdWeUNqdzhDaTlUYVhwbElERXpDaTlTYjI5MElETWdNQ0JTQ2k5SmJtWnZJREVnTUNCU0NpOUpSQ0JiUERRM01UUTVOVEV3TkRNelpHUTBPRGd5WmpBMVpqaGpNVEkwTWpJek56TTBQancwTnpFME9UVXhNRFF6TTJSa05EZzRNbVl3TldZNFl6RXlOREl5TXpjek5ENWRDajQrQ25OMFlYSjBlSEpsWmdveU56STJDaVVsUlU5R0NnPT08L0Jhc2U2NENvbnRhaW5lcj4KICAgICAgPC9Db250ZW50PgogICAgPC9SZWZEb2M+CiAgPC9Eb2N1bWVudD4KPC9Nc2dIZWFkPgo="
                        it.contentType shouldBe "application/xml"
                        it.contentTransferEncoding shouldBe "base64"
                    }

                verifySequence {
                    helseIdClient.getAccessToken(StandardAccessTokenRequest(TokenType.DPOP))
                    proofBuilder.buildProof(
                        Endpoint(HttpMethod.GET, "${wireMock.baseUrl}/Messages/$id/business-document"),
                        null,
                        accessToken
                    )
                }
            }

            "Should be able to parse apprec response" {
                val id = UUID.randomUUID()
                mockGetBusinessDocument(id, body = readResourceContentAsString("msh/get-business-document-apprec-response.json"))

                val accessToken = UUID.randomUUID().toString()
                val client = MshInternalClient(
                    baseUrl = wireMock.baseUrl,
                    sourceSystem = UUID.randomUUID().toString(),
                    helseIdClient = mockk<HelseIdClient> { every { getAccessToken(any()) } returns buildTokenResponse(accessToken) },
                    proofBuilder = mockk<ProofBuilder> { every { buildProof(any(), any(), any()) } returns UUID.randomUUID().toString() },
                )
                client
                    .getBusinessDocument(id)
                    .asClue {
                        it.businessDocument shouldBe "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0idXRmLTgiPz4KPEFwcFJlYyB4bWxuczp4c2k9Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvWE1MU2NoZW1hLWluc3RhbmNlIiB4bWxuczp4c2Q9Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvWE1MU2NoZW1hIiB4bWxucz0iaHR0cDovL3d3dy5raXRoLm5vL3htbHN0ZHMvYXBwcmVjLzIwMTItMDItMTUiPgogIDxNc2dUeXBlIFY9IkFQUFJFQyIgLz4KICA8TUlHdmVyc2lvbj52MS4xIDIwMTItMDItMTU8L01JR3ZlcnNpb24+CiAgPEdlbkRhdGU+MjAyNS0wOC0yMVQxMToyMToxNi4wMDA0MTU1WjwvR2VuRGF0ZT4KICA8SWQ+OWViYWFmNDQtNzMxNy00MWI3LTg5MmEtMmE1NjhhY2Q1MTExPC9JZD4KICA8U2VuZGVyPgogICAgPFJvbGUgVj0iUFJJTSIgRE49IlByaW3DpnJtb3R0YWtlciIgLz4KICAgIDxIQ1A+CiAgICAgIDxJbnN0PgogICAgICAgIDxOYW1lPk5ITjwvTmFtZT4KICAgICAgICA8SWQ+MTEyMzc0PC9JZD4KICAgICAgICA8VHlwZUlkIFY9IkhFUiIgRE49IkhFUi1pZCIgLz4KICAgICAgICA8RGVwdD4KICAgICAgICAgIDxOYW1lPk5ITjwvTmFtZT4KICAgICAgICAgIDxJZD44MDk0ODY2PC9JZD4KICAgICAgICAgIDxUeXBlSWQgVj0iSEVSIiBETj0iSEVSLWlkIiAvPgogICAgICAgIDwvRGVwdD4KICAgICAgPC9JbnN0PgogICAgPC9IQ1A+CiAgPC9TZW5kZXI+CiAgPFJlY2VpdmVyPgogICAgPFJvbGUgVj0iQVZTIiAvPgogICAgPEhDUD4KICAgICAgPEluc3Q+CiAgICAgICAgPE5hbWU+S1MtRElHSVRBTEUgRkVMTEVTVEpFTkVTVEVSIEFTPC9OYW1lPgogICAgICAgIDxJZD44MTQyOTg3PC9JZD4KICAgICAgICA8VHlwZUlkIFY9IkhFUiIgRE49IkhFUi1pZCIgLz4KICAgICAgICA8RGVwdD4KICAgICAgICAgIDxOYW1lPlN2YXJVdCBtZWxkaW5nc2Zvcm1pZGxlcjwvTmFtZT4KICAgICAgICAgIDxJZD44MTQzMDYwPC9JZD4KICAgICAgICAgIDxUeXBlSWQgVj0iSEVSIiBETj0iSEVSLWlkIiAvPgogICAgICAgIDwvRGVwdD4KICAgICAgPC9JbnN0PgogICAgPC9IQ1A+CiAgPC9SZWNlaXZlcj4KICA8U3RhdHVzIFY9IjEiIEROPSJPSyIgLz4KICA8T3JpZ2luYWxNc2dJZD4KICAgIDxNc2dUeXBlIFY9IkRJQUxPR19IRUxTRUZBR0xJRyIgRE49IkhlbHNlZmFnbGlnIGRpYWxvZyIgLz4KICAgIDxJc3N1ZURhdGU+MjAyNS0wOC0yMVQxMzoyMDo0OCswMjowMDwvSXNzdWVEYXRlPgogICAgPElkPmE1ZTk2NjVlLTQ5ODItNDUyMS1hNTBmLTRiMTIxZmMyNTA5NDwvSWQ+CiAgPC9PcmlnaW5hbE1zZ0lkPgo8L0FwcFJlYz4K"
                        it.contentType shouldBe "application/xml"
                        it.contentTransferEncoding shouldBe "base64"
                    }
            }

            "Not OK response should throw exception" {
                val id = UUID.randomUUID()
                val expected = UUID.randomUUID().toString()
                mockGetBusinessDocument(id, 503, expected)

                shouldThrow<HttpServerException> {
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
            "Should make expected calls and parse response" {
                val id = UUID.randomUUID()
                mockGetStatus(id)

                val accessToken = UUID.randomUUID().toString()
                val helseIdClient = mockk<HelseIdClient> { every { getAccessToken(any()) } returns buildTokenResponse(accessToken) }
                val proofBuilder = mockk<ProofBuilder> { every { buildProof(any(), any(), any()) } returns UUID.randomUUID().toString() }
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
                            transportDeliveryState shouldBe DeliveryState.UNCONFIRMED
                            appRecStatus should beNull()
                        }
                    }

                verifySequence {
                    helseIdClient.getAccessToken(StandardAccessTokenRequest(TokenType.DPOP))
                    proofBuilder.buildProof(
                        Endpoint(HttpMethod.GET, "${wireMock.baseUrl}/Messages/$id/status"),
                        null,
                        accessToken
                    )
                }
            }

            "Should be able to parse acknowledged response" {
                val id = UUID.randomUUID()
                mockGetStatus(id, body = readResourceContentAsString("msh/get-status-acked.json"))

                val accessToken = UUID.randomUUID().toString()
                val client = MshInternalClient(
                    baseUrl = wireMock.baseUrl,
                    sourceSystem = UUID.randomUUID().toString(),
                    helseIdClient = mockk<HelseIdClient> { every { getAccessToken(any()) } returns buildTokenResponse(accessToken) },
                    proofBuilder = mockk<ProofBuilder> { every { buildProof(any(), any(), any()) } returns UUID.randomUUID().toString() },
                )
                client
                    .getStatus(id)
                    .asClue {
                        it.size shouldBe 1
                        with(it.single()) {
                            receiverHerId shouldBe 8094866
                            transportDeliveryState shouldBe DeliveryState.ACKNOWLEDGED
                            appRecStatus shouldBe AppRecStatus.OK
                        }
                    }
            }

            "Not OK response should throw exception" {
                val id = UUID.randomUUID()
                val expected = UUID.randomUUID().toString()
                mockGetStatus(id, 504, expected)

                shouldThrow<HttpServerException> {
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
            "Should make expected calls and parse response" {
                val id = UUID.randomUUID()
                val senderHerId = nextInt(1, 1000000)
                mockPostAppRec(id, senderHerId)

                val accessToken = UUID.randomUUID().toString()
                val helseIdClient = mockk<HelseIdClient> { every { getAccessToken(any()) } returns buildTokenResponse(accessToken) }
                val proofBuilder = mockk<ProofBuilder> { every { buildProof(any(), any(), any()) } returns UUID.randomUUID().toString() }
                val client = MshInternalClient(
                    baseUrl = wireMock.baseUrl,
                    sourceSystem = UUID.randomUUID().toString(),
                    helseIdClient = helseIdClient,
                    proofBuilder = proofBuilder,
                )
                client
                    .postAppRec(id, senderHerId, PostAppRecRequest())
                    .asClue {
                        it shouldBe UUID.fromString("680bb102-4541-4ca9-8210-07251b3206a3")
                    }

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

                shouldThrow<HttpServerException> {
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

    private fun mockGetAppRec(id: UUID, status: Int = 200, body: String = readResourceContentAsString("msh/get-apprecinfo-ok.json")) {
        wireMockClient.register(
            get("/Messages/$id/apprec")
                .willReturn(
                    status(status)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)
                )
        )
    }

    private fun mockGetMessages(receiverHerId: Int, status: Int = 200, body: String = readResourceContentAsString("msh/get-messages-response.json")) {
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

    private fun mockGetMessage(id: UUID, status: Int = 200, body: String = readResourceContentAsString("msh/get-message-response.json")) {
        wireMockClient.register(
            get("/Messages/$id")
                .willReturn(
                    status(status)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)
                )
        )
    }

    private fun mockGetBusinessDocument(id: UUID, status: Int = 200, body: String = readResourceContentAsString("msh/get-business-document-response.json")) {
        wireMockClient.register(
            get("/Messages/$id/business-document")
                .willReturn(
                    status(status)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)
                )
        )
    }

    private fun mockGetStatus(id: UUID, status: Int = 200, body: String = readResourceContentAsString("msh/get-status.json")) {
        wireMockClient.register(
            get("/Messages/$id/status")
                .willReturn(
                    status(status)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)
                )
        )
    }

    private fun mockSendMessage(status: Int = 201, body: String = readResourceContentAsString("msh/post-message-response.txt")) {
        wireMockClient.register(
            post("/Messages")
                .willReturn(
                    status(status)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)
                )
        )
    }

    private fun mockPostAppRec(appRecId: UUID, appRecSenderHerId: Int, status: Int = 201, body: String = readResourceContentAsString("msh/post-apprec-response.txt")) {
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
