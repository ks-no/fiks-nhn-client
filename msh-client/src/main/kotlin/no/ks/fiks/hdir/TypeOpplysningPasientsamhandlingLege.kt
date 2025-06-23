package no.ks.fiks.hdir


// Kodeverk: 9153 Type opplysninger ved pasientsamhandling - fra lege
enum class TypeOpplysningPasientsamhandlingLege(
    override val verdi: String,
    override val navn: String,
) : KodeverkVerdi {
    TJENESTETILBUD("3", "Tjenestetilbud"),
    TILSTANDSVURDERING("5", "Tilstandsvurdering"),
    ANNEN_HENVENDELSE("99", "Annen henvendelse");

    override val kodeverk: String = "2.16.578.1.12.4.1.1.9153"
}