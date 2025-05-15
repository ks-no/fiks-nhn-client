package no.ks.fiks.hdir

interface IdType : KodeverkVerdi

// Kodeverk: 8116 ID-type for personer
enum class PersonIdType(
    override val verdi: String,
    override val navn: String,
) : IdType {
    HER_ID("HER", "HER-id"),
    FNR("FNR", "Fødselsnummer");

    override val kodeverk: String = "2.16.578.1.12.4.1.1.8116"
}

// Kodeverk: 9051 ID-typer for organisatoriske enheter
enum class OrganisasjonIdType(
    override val verdi: String,
    override val navn: String,
) : IdType {
    HER_ID("HER", "HER-id");

    override val kodeverk: String = "2.16.578.1.12.4.1.1.9051"
}
