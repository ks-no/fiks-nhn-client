package no.ks.fiks.nhn.ar.rest

import feign.Request
import feign.RequestTemplate
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.ks.fiks.helseid.AccessTokenRequestBuilder
import no.ks.fiks.helseid.HelseIdClient
import no.ks.fiks.helseid.TokenResponse
import no.ks.fiks.helseid.TokenType
import no.ks.fiks.helseid.dpop.Endpoint
import no.ks.fiks.helseid.dpop.HttpMethod
import no.ks.fiks.helseid.dpop.ProofBuilder
import java.util.UUID

class DpopAuthInterceptorTest : StringSpec({
    "apply adds DPoP headers and builds endpoint URL" {
        val helseIdClient = mockk<HelseIdClient>()
        val proofBuilder = mockk<ProofBuilder>()
        val accessTokenRequest = AccessTokenRequestBuilder()
            .tokenType(TokenType.DPOP)
            .build()
        val endpoint = slot<Endpoint>()
        val accessToken = UUID.randomUUID().toString()
        val proofToken = UUID.randomUUID().toString()

        every { helseIdClient.getAccessToken(accessTokenRequest) } returns TokenResponse(
            accessToken,
            3600,
            "DPoP",
            "scope",
        )
        every { proofBuilder.buildProof(capture(endpoint), null, accessToken) } returns proofToken

        val interceptor = DpopAuthInterceptor(
            baseUrl = "https://example.com/",
            helseIdClient = helseIdClient,
            proofBuilder = proofBuilder,
            accessTokenRequest = accessTokenRequest,
        )
        val template = RequestTemplate()
            .method(Request.HttpMethod.GET)
            .uri("api/v1/communicationparty/123")

        interceptor.apply(template)

        endpoint.captured shouldBe Endpoint(
            method = HttpMethod.GET,
            url = "https://example.com/api/v1/communicationparty/123",
        )
        template.headers()["Authorization"]?.single() shouldBe "DPoP $accessToken"
        template.headers()["DPoP"]?.single() shouldBe proofToken
        verify(exactly = 1) { helseIdClient.getAccessToken(accessTokenRequest) }
        verify(exactly = 1) { proofBuilder.buildProof(any(), null, accessToken) }
    }

    "apply rethrows exceptions from token lookup" {
        val helseIdClient = mockk<HelseIdClient>()
        val proofBuilder = mockk<ProofBuilder>()
        val accessTokenRequest = AccessTokenRequestBuilder()
            .tokenType(TokenType.DPOP)
            .build()
        val cause = IllegalStateException("token failure")

        every { helseIdClient.getAccessToken(accessTokenRequest) } throws cause

        val interceptor = DpopAuthInterceptor(
            baseUrl = "https://example.com",
            helseIdClient = helseIdClient,
            proofBuilder = proofBuilder,
            accessTokenRequest = accessTokenRequest,
        )
        val template = RequestTemplate()
            .method(Request.HttpMethod.POST)
            .uri("/api/v1/communicationparty")

        val exception = shouldThrow<IllegalStateException> {
            interceptor.apply(template)
        }

        exception shouldBe cause
    }
})



