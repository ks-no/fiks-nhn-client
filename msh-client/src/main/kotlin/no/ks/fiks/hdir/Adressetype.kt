package no.ks.fiks.hdir

enum class Adressetype(
    override val verdi: String,
    override val navn: String,
) : KodeverkVerdi {

    UBRUKELIG_ADRESSE("BAD", "Ubrukelig adresse"),
    BOSTEDSADRESSE("H", "Bostedsadresse"),
    FOLKEREGISTERADRESSE("HP", "Folkeregisteradresse"),
    FERIEADRESSE("HV", "Ferieadresse"),
    FAKTURERINGSADRESSE("INV", "Faktureringsadresse"),
    POSTADRESSE("PST", "Postadresse"),
    BESOKSADRESSE("RES", "Besøksadresse"),
    MIDLERTIDIG_ADRESSE("TMP", "Midlertidig adresse"),
    ARBEIDSADRESSE("WP", "Arbeidsadresse");

    override val kodeverk: String = "2.16.578.1.12.4.1.1.3401"
}