package no.ks.fiks.nhn.ar.rest

import no.ks.fiks.helseid.AccessTokenRequestBuilder
import no.ks.fiks.helseid.Configuration as HelseIdConfiguration

class AdresseregisteretService(
    url: String,
    helseIdConfiguration: HelseIdConfiguration,
    accessTokenRequestBuilder: AccessTokenRequestBuilder? = null,
) : AdresseregisteretRestService(url, helseIdConfiguration, accessTokenRequestBuilder)
