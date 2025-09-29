package no.ks.fiks.nhn.examples

import io.kotest.core.spec.style.StringSpec
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.ks.fiks.helseid.Environment
import no.ks.fiks.nhn.msh.ClientFactory
import no.ks.fiks.nhn.msh.ClientWithFastlegeLookup
import no.ks.fiks.nhn.msh.Configuration
import no.ks.fiks.nhn.msh.HelseIdConfiguration
import no.ks.fiks.nhn.msh.HelseIdTenantParameters
import no.ks.fiks.nhn.msh.HelseIdTokenParameters
import no.ks.fiks.nhn.msh.MultiTenantHelseIdTokenParameters
import no.ks.fiks.nhn.msh.SingleTenantHelseIdTokenParameters
import java.util.*

class CreateClientTest : StringSpec({
    "! Create client using factory" {
        ClientFactory.createClient(
            Configuration(
                helseId = HelseIdConfiguration(
                    environment = Environment(
                        issuer = "<HelseID issuer/base URL>",
                        audience = "<HelseID audience>",
                    ),
                    clientId = "<HelseID client id>",
                    jwk = "<HelseID JWK string>",
                ),
                mshBaseUrl = "<MSH base URL>",
                sourceSystem = "<Name describing the system using the client>",
            )
        )
    }

    "! Create client with parameters using factory" {
        ClientFactory.createClient(
            Configuration(
                helseId = HelseIdConfiguration(
                    environment = Environment(
                        issuer = "<HelseID issuer/base URL>",
                        audience = "<HelseID audience>",
                    ),
                    clientId = "<HelseID client id>",
                    jwk = "<HelseID JWK string>",
                    tokenParams = HelseIdTokenParameters(
                        tenant = MultiTenantHelseIdTokenParameters( // Can also be single tenant
                            parentOrganization = "111111111", // Organization number
                            childOrganization = "222222222", // Organization number
                        ),
                    ),
                ),
                mshBaseUrl = "<MSH base URL>",
                sourceSystem = "<Name describing the system using the client>",
            )
        )
    }
})