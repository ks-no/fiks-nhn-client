package no.ks.fiks.nhn.msh

data class MshConfiguration(
    val herId: Int,
    val clientLocked: Boolean? = null,
    val rejectMessageFilters: RejectMessageFilters? = null,
)

data class RejectMessageFilters(
    val messageFunctions: List<String>,
)
