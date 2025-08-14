package no.ks.fiks.nhn.msh

import no.ks.fiks.helseid.Environment as HelseIdEnvironment

data class Configuration(
    val helseId: HelseIdConfiguration,
    val mshBaseUrl: String,
    val sourceSystem: String,
)

data class ConfigurationWithFastlegeLookup(
    val helseId: HelseIdConfiguration,
    val adresseregister: AdresseregisterConfiguration,
    val fastlegeregister: FastlegeregisterConfiguration,
    val mshBaseUrl: String,
    val sourceSystem: String,
)

data class HelseIdConfiguration(
    val environment: HelseIdEnvironment,
    val clientId: String,
    val jwk: String,
    val tokenParams: HelseIdTokenParameters? = null,
)

data class AdresseregisterConfiguration(
    val url: String,
    val credentials: Credentials,
)

data class FastlegeregisterConfiguration(
    val url: String,
    val credentials: Credentials,
)

data class Credentials(
    val username: String,
    val password: String,
)
