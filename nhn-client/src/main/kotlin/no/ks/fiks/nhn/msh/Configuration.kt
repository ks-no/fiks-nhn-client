package no.ks.fiks.nhn.msh

import java.time.Duration
import no.ks.fiks.helseid.Environment as HelseIdEnvironment
import no.ks.fiks.nhn.ar.Credentials as ArCredentials
import no.ks.fiks.nhn.ar.Environment as ArEnvironment
import no.ks.fiks.nhn.flr.Credentials as FlrCredentials
import no.ks.fiks.nhn.flr.Environment as FlrEnvironment

data class Configuration(
    val environments: Environments,
    val fastlegeregisteretCredentials: Credentials,
    val adresseregisteretCredentials: Credentials,

    val clientId: String,
    val jwk: String,
    val jwtRequestExpirationTime: Duration = Duration.ofSeconds(60),

    val sourceSystem: String,
) {

    fun getAdresseregisteretCredentials() = ArCredentials(
        username = adresseregisteretCredentials.username,
        password = adresseregisteretCredentials.password,
    )

    fun getFastlegeregisteretCredentials() = FlrCredentials(
        username = fastlegeregisteretCredentials.username,
        password = fastlegeregisteretCredentials.password,
    )

}

data class Credentials(
    val username: String,
    val password: String,
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
