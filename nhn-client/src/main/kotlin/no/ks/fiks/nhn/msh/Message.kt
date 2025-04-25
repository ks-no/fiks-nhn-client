package no.ks.fiks.nhn.msh

import no.ks.fiks.hdir.IdType
import no.ks.fiks.hdir.MeldingensFunksjon
import java.io.InputStream

data class Message(
    val type: MeldingensFunksjon,
    val sender: Organisation,
    val receiver: Receiver,
    val vedlegg: InputStream?,
)

data class Organisation(
    val name: String,
    val id: Id,
    val childOrganisation: Organisation? = null,
)

data class Id(
    val id: String,
    val type: IdType,
)

sealed class Receiver
data class HerIdReceiver(
    val parent: HerIdReceiverParent,
    val child: HerIdReceiverChild,
    val patient: Patient,
) : Receiver()

data class HerIdReceiverParent(
    val name: String,
    val id: Id,
)

sealed class HerIdReceiverChild
data class OrganisasjonHerIdReceiverChild(
    val name: String,
    val id: Id,
) : HerIdReceiverChild()
data class PersonHerIdReceiverChild(
    val id: Id,
    val firstName: String,
    val middleName: String?,
    val lastName: String,
) : HerIdReceiverChild()

data class Patient(
    val fnr: String,
    val firstName: String,
    val middleName: String?,
    val lastName: String,
)

