package no.ks.fiks.nhn.ar.rest

import no.ks.fiks.helseid.AccessTokenRequestBuilder
import no.ks.fiks.helseid.HelseIdClient
import no.ks.fiks.helseid.dpop.ProofBuilder

class AdresseregisteretService(
    url: String,
    helseIdClient: HelseIdClient,
    proofBuilder: ProofBuilder,
    accessTokenRequestBuilder: AccessTokenRequestBuilder? = null,
) : AdresseregisteretRestService(url, helseIdClient, proofBuilder, accessTokenRequestBuilder)
