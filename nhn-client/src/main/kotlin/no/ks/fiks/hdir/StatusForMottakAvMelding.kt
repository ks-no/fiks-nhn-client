package no.ks.fiks.hdir

// Kodeverk: 8258 Status for mottak av melding
enum class StatusForMottakAvMelding(
    override val verdi: String,
    override val navn: String,
) : KodeverkVerdi {
    OK("1", "OK"),
    AVVIST("2", "Avvist"),
    OK_FEIL_I_DELMELDING("3", "OK, feil i delmelding");

    override val kodeverk: String = "2.16.578.1.12.4.1.1.8258"
}