package no.ks.fiks.nhn.msh

import no.ks.fiks.helseid.HelseIdClient
import no.ks.fiks.helseid.dpop.ProofBuilder
import no.ks.fiks.nhn.ar.AdresseregisteretClient
import no.ks.fiks.nhn.ar.AdresseregisteretService
import no.ks.fiks.nhn.flr.Credentials
import no.ks.fiks.nhn.flr.FastlegeregisteretClient
import no.ks.fiks.nhn.flr.FastlegeregisteretService

object ClientFactory {

    fun createClient(configuration: Configuration): Client =
        Client(
            apiService = buildApiService(configuration),
        )

    fun createClientWithFastlegeLookup(configuration: Configuration): ClientWithFastlegeLookup =
        ClientWithFastlegeLookup(
            apiService = buildApiService(configuration),
            flrClient = buildFlrClient(configuration),
            arClient = buildArClient(configuration),
        )

    private fun buildApiService(configuration: Configuration) = ApiService(
        api = FeignApiBuilder.build(
            mshBaseUrl = configuration.environments.mshBaseUrl,
            helseIdClient = HelseIdClient(
                no.ks.fiks.helseid.Configuration(
                    clientId = configuration.helseId.clientId,
                    jwk = configuration.helseId.jwk,
                    environment = configuration.environments.helseIdEnvironment,
                ),
            ),
            proofBuilder = ProofBuilder(configuration.helseId.jwk),
            tokenParams = configuration.helseId.tokenParams,
        ),
        sourceSystem = configuration.sourceSystem,
    )

    private fun buildFlrClient(configuration: Configuration) = FastlegeregisteretClient(
        FastlegeregisteretService(
            url = configuration.environments.fastlegeregisterUrl,
            credentials = configuration.fastlegeregisteret.let {
                Credentials(
                    username = it.username,
                    password = it.password,
                )
            },
        )
    )

    private fun buildArClient(configuration: Configuration) = AdresseregisteretClient(
        AdresseregisteretService(
            url = configuration.environments.adresseregisterUrl,
            credentials = configuration.adresseregisteret.let {
                no.ks.fiks.nhn.ar.Credentials(
                    username = it.username,
                    password = it.password,
                )
            },
        )
    )


}