package no.ks.fiks.nhn.msh

import no.ks.fiks.helseid.Environment as HelseIdEnvironment
import no.ks.fiks.nhn.ar.Environment as ArEnvironment
import no.ks.fiks.nhn.flr.Environment as FlrEnvironment

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
    val mshEnvironment: Environment,
) {
    companion object {
        val TEST = Environments(
            HelseIdEnvironment.TEST,
            ArEnvironment.TEST,
            FlrEnvironment.TEST,
            Environment.TEST,
        )
        val PROD = Environments(
            HelseIdEnvironment.PROD,
            ArEnvironment.PROD,
            FlrEnvironment.PROD,
            Environment.PROD,
        )

    }

}

data class HelseIdConfiguration(
    val clientId: String,
    val jwk: String,
)

data class Credentials(
    val username: String,
    val password: String,
)
