package no.ks.fiks.nhn.msh

import no.ks.fiks.helseid.Environment as HelseIdEnvironment
import no.ks.fiks.nhn.ar.Environment as ArEnvironment
import no.ks.fiks.nhn.flr.Environment as FlrEnvironment
import no.ks.fiks.nhn.msh.Environment as MshEnvironment

data class Configuration(
    val environments: Environments,
    val sourceSystem: String,

    val helseId: HelseIdConfiguration,
    val fastlegeregisteret: Credentials,
    val adresseregisteret: Credentials,
)

data class Environments(
    val helseIdEnvironment: HelseIdEnvironment,
    val adresseregisteretEnvironment: ArEnvironment,
    val fastlegeregisteretEnvironment: FlrEnvironment,
    val mshEnvironment: MshEnvironment,
) {
    companion object {
        val TEST = Environments(
            HelseIdEnvironment.TEST,
            ArEnvironment.TEST,
            FlrEnvironment.TEST,
            MshEnvironment.TEST,
        )
        val PROD = Environments(
            HelseIdEnvironment.PROD,
            ArEnvironment.PROD,
            FlrEnvironment.PROD,
            MshEnvironment.PROD,
        )

    }
}

class EnvironmentsBuilder {

    private var helseIdEnvironment: HelseIdEnvironment? = null
    private var adresseregisteretEnvironment: ArEnvironment? = null
    private var fastlegeregisteretEnvironment: FlrEnvironment? = null
    private var mshEnvironment: MshEnvironment? = null

    fun helseIdEnvironment(url: String, audience: String) = apply { helseIdEnvironment = HelseIdEnvironment(url, audience) }
    fun adresseregisteretEnvironment(url: String) = apply { adresseregisteretEnvironment = ArEnvironment(url) }
    fun fastlegeregisteretEnvironment(url: String) = apply { fastlegeregisteretEnvironment = FlrEnvironment(url) }
    fun mshEnvironment(url: String) = apply { mshEnvironment = MshEnvironment(url) }

    fun build(): Environments = Environments(
        helseIdEnvironment = helseIdEnvironment ?: throw IllegalStateException("helseIdEnvironment is required"),
        adresseregisteretEnvironment = adresseregisteretEnvironment ?: throw IllegalStateException("adresseregisteretEnvironment is required"),
        fastlegeregisteretEnvironment = fastlegeregisteretEnvironment ?: throw IllegalStateException("fastlegeregisteretEnvironment is required"),
        mshEnvironment = mshEnvironment ?: throw IllegalStateException("mshEnvironment is required"),
    )

}

data class HelseIdConfiguration(
    val clientId: String,
    val jwk: String,
)

data class Credentials(
    val username: String,
    val password: String,
)
