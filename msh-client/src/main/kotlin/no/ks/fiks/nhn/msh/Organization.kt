package no.ks.fiks.nhn.msh

data class Organization(
    val name: String,
    val ids: List<Id>,
    val childOrganization: ChildOrganization?,
)

data class ChildOrganization(
    val name: String,
    val ids: List<Id>,
)
