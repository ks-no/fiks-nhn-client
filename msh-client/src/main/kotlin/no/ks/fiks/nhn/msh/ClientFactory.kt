package no.ks.fiks.nhn.msh

import no.ks.fiks.helseid.AccessTokenRequestBuilder
import no.ks.fiks.helseid.HelseIdClient
import no.ks.fiks.helseid.TenancyType
import no.ks.fiks.helseid.dpop.ProofBuilder
import no.ks.fiks.nhn.ar.rest.AdresseregisteretClient
import no.ks.fiks.nhn.ar.rest.AdresseregisteretService
import no.ks.fiks.nhn.flr.Credentials
import no.ks.fiks.nhn.flr.FastlegeregisteretClient
import no.ks.fiks.nhn.flr.FastlegeregisteretService

object ClientFactory {

    @JvmOverloads
    fun createClient(configuration: Configuration, messageHandlers: List<MessageHandler> = emptyList()): Client =
        Client(
            internalClient = createMshInternalClient(configuration.helseId, configuration.mshBaseUrl, configuration.sourceSystem),
            messageHandlers = messageHandlers,
        )

    @JvmOverloads
    fun createClientWithFastlegeLookup(configuration: ConfigurationWithFastlegeLookup, messageHandlers: List<MessageHandler> = emptyList()): ClientWithFastlegeLookup =
        HelseIdDependencies(configuration.helseId).let { helseId ->
            ClientWithFastlegeLookup(
                internalClient = createMshInternalClient(configuration.helseId, configuration.mshBaseUrl, configuration.sourceSystem, helseId.client, helseId.proofBuilder),
                flrClient = createFlrClient(configuration.fastlegeregister),
                arClient = createArClient(configuration.adresseregister, configuration.helseId, helseId.client, helseId.proofBuilder),
                messageHandlers = messageHandlers,
            )
        }

    private fun createMshInternalClient(
        helseIdConfiguration: HelseIdConfiguration,
        mshBaseUrl: String,
        sourceSystem: String,
        helseIdClient: HelseIdClient = createHelseIdClient(helseIdConfiguration),
        proofBuilder: ProofBuilder = ProofBuilder(helseIdConfiguration.jwk),
    ) = MshInternalClient(
        baseUrl = mshBaseUrl,
        sourceSystem = sourceSystem,
        defaultTokenParams = helseIdConfiguration.tokenParams,
        helseIdClient = helseIdClient,
        proofBuilder = proofBuilder,
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

    fun createArClient(
        configuration: AdresseregisterConfiguration,
        helseIdConfiguration: HelseIdConfiguration,
        helseIdClient: HelseIdClient = createHelseIdClient(helseIdConfiguration),
        proofBuilder: ProofBuilder = ProofBuilder(helseIdConfiguration.jwk),
    ) = AdresseregisteretClient(
        AdresseregisteretService(
            url = configuration.url,
            helseIdClient = helseIdClient,
            proofBuilder = proofBuilder,
            accessTokenRequestBuilder = createAccessTokenRequestBuilder(helseIdConfiguration.tokenParams),
        )
    )

    private fun createHelseIdClient(helseIdConfiguration: HelseIdConfiguration) = HelseIdClient(
        no.ks.fiks.helseid.Configuration(
            clientId = helseIdConfiguration.clientId,
            jwk = helseIdConfiguration.jwk,
            environment = helseIdConfiguration.environment,
        ),
    )

    private fun createAccessTokenRequestBuilder(tokenParams: HelseIdTokenParameters?): AccessTokenRequestBuilder? =
        tokenParams?.tenant?.let { tenant ->
            AccessTokenRequestBuilder().apply {
                when (tenant) {
                    is SingleTenantHelseIdTokenParameters ->
                        tenancyType(TenancyType.SINGLE)
                            .childOrganizationNumber(tenant.childOrganization)

                    is MultiTenantHelseIdTokenParameters ->
                        tenancyType(TenancyType.MULTI)
                            .parentOrganizationNumber(tenant.parentOrganization)
                            .also { if (tenant.childOrganization != null) childOrganizationNumber(tenant.childOrganization) }
                }
            }
        }

    private data class HelseIdDependencies(
        val client: HelseIdClient,
        val proofBuilder: ProofBuilder,
    ) {
        constructor(configuration: HelseIdConfiguration) : this(
            client = createHelseIdClient(configuration),
            proofBuilder = ProofBuilder(configuration.jwk),
        )
    }


}