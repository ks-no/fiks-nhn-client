package no.ks.fiks.nhn

data class Message(
    val type: MessageType,
    val sender: Organisation,
    val receiver: Receiver,
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

interface IdType {
    val verdi: String
    val navn: String
    val kodeverk: String
}

// Kodeverk: 9051 ID-typer for organisatoriske enheter
enum class OrganisasjonIdType(
    override val verdi: String,
    override val navn: String,
) : IdType {
    HER_ID("HER", "HER-id");

    override val kodeverk: String = "2.16.578.1.12.4.1.1.9051"
}

// Kodeverk: 8116 ID-type for personer
enum class PersonIdType(
    override val verdi: String,
    override val navn: String,
) : IdType {
    HER_ID("HER", "HER-id"),
    FNR("FNR", "Fødselsnummer");

    override val kodeverk: String = "2.16.578.1.12.4.1.1.8116"
}

sealed class Receiver
data class FastlegeForPersonReceiver(
    val fnr: String,
) : Receiver()

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

