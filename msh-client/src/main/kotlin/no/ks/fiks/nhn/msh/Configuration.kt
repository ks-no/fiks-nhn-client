package no.ks.fiks.nhn.msh

import no.ks.fiks.helseid.Environment as HelseIdEnvironment

data class Configuration(
    val environments: Environments,
    val sourceSystem: String,

    val helseId: HelseIdConfiguration,
    val fastlegeregisteret: Credentials,
    val adresseregisteret: Credentials,
)

data class Environments(
    val helseIdEnvironment: HelseIdEnvironment,
    val adresseregisterUrl: String,
    val fastlegeregisterUrl: String,
    val mshBaseUrl: String,
)

class EnvironmentsBuilder {

    private var helseIdEnvironment: HelseIdEnvironment? = null
    private var adresseregisterUrl: String? = null
    private var fastlegeregisterUrl: String? = null
    private var mshBaseUrl: String? = null

    fun helseIdEnvironment(url: String, audience: String) = apply { helseIdEnvironment = HelseIdEnvironment(url, audience) }
    fun adresseregisteretUrl(url: String) = apply { adresseregisterUrl = url }
    fun fastlegeregisteretUrl(url: String) = apply { fastlegeregisterUrl = url }
    fun mshBaseUrl(url: String) = apply { mshBaseUrl = url }

    fun build(): Environments = Environments(
        helseIdEnvironment = helseIdEnvironment ?: throw IllegalStateException("helseIdEnvironment is required"),
        adresseregisterUrl = adresseregisterUrl ?: throw IllegalStateException("adresseregisteretEnvironment is required"),
        fastlegeregisterUrl = fastlegeregisterUrl ?: throw IllegalStateException("fastlegeregisteretEnvironment is required"),
        mshBaseUrl = mshBaseUrl ?: throw IllegalStateException("mshBaseUrl is required"),
    )
}

data class HelseIdConfiguration(
    val clientId: String,
    val jwk: String,
    val tokenConfiguration: HelseIdTokenConfiguration? = null,
)
data class HelseIdTokenConfiguration(
    val tenantConfiguration: HelseIdTenantConfiguration? = null,
)
sealed class HelseIdTenantConfiguration
class SingleTenantHelseIdTokenConfiguration(
    val childOrganization: String,
) :  HelseIdTenantConfiguration()
class MultiTenantHelseIdTokenConfiguration(
    val parentOrganization: String,
    val childOrganization: String? = null,
) :  HelseIdTenantConfiguration()

data class Credentials(
    val username: String,
    val password: String,
)
