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
            internalClient = createMshInternalClient(configuration.helseId, configuration.mshBaseUrl, configuration.sourceSystem),
        )

    fun createClientWithFastlegeLookup(configuration: ConfigurationWithFastlegeLookup): ClientWithFastlegeLookup =
        ClientWithFastlegeLookup(
            internalClient = createMshInternalClient(configuration.helseId, configuration.mshBaseUrl, configuration.sourceSystem),
            flrClient = createFlrClient(configuration.fastlegeregister),
            arClient = createArClient(configuration.adresseregister),
        )

    private fun createMshInternalClient(
        helseIdConfiguration: HelseIdConfiguration,
        mshBaseUrl: String,
        sourceSystem: String,
    ) = MshInternalClient(
        baseUrl = mshBaseUrl,
        sourceSystem = sourceSystem,
        defaultTokenParams = helseIdConfiguration.tokenParams,
        helseIdClient = HelseIdClient(
            no.ks.fiks.helseid.Configuration(
                clientId = helseIdConfiguration.clientId,
                jwk = helseIdConfiguration.jwk,
                environment = helseIdConfiguration.environment,
            ),
        ),
        proofBuilder = ProofBuilder(helseIdConfiguration.jwk),
    )

    fun createFlrClient(configuration: FastlegeregisterConfiguration) = FastlegeregisteretClient(
        FastlegeregisteretService(
            url = configuration.url,
            credentials = configuration.credentials.let {
                Credentials(
                    username = it.username,
                    password = it.password,
                )
            },
        )
    )

    fun createArClient(configuration: AdresseregisterConfiguration) = AdresseregisteretClient(
        AdresseregisteretService(
            url = configuration.url,
            credentials = configuration.credentials.let {
                no.ks.fiks.nhn.ar.Credentials(
                    username = it.username,
                    password = it.password,
                )
            },
        )
    )


}