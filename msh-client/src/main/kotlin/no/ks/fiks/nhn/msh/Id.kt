package no.ks.fiks.nhn.msh

import no.ks.fiks.hdir.IdType
import no.ks.fiks.hdir.OrganizationIdType
import no.ks.fiks.hdir.PersonIdType

sealed class Id(
    val id: String,
    val type: IdType,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Id

        if (id != other.id) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }
}

class PersonId(
    id: String,
    type: PersonIdType,
) : Id(id, type)

class OrganizationId(
    id: String,
    type: OrganizationIdType,
) : Id(id, type)