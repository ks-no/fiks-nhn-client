package no.ks.fiks.nhn.msh

data class RequestParameters(
    val helseId: HelseIdTokenParameters? = null,
)

data class HelseIdTokenParameters(
    val tenant: HelseIdTenantParameters? = null,
)

sealed class HelseIdTenantParameters
class SingleTenantHelseIdTokenParameters(
    val childOrganization: String,
) :  HelseIdTenantParameters()
class MultiTenantHelseIdTokenParameters(
    val parentOrganization: String,
    val childOrganization: String? = null,
) :  HelseIdTenantParameters()
