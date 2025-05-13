package no.ks.fiks.hdir

// Kodeverk: 7322 Tema for helsefaglig dialog
enum class TemaForHelsefagligDialog(
    override val verdi: String,
    override val navn: String,
) : KodeverkVerdi {
    HENVENDELSE_OM_PASIENT("6", "Henvendelse om pasient"),
    TILBAKEMELDING_MOTTAK("7", "Tilbakemelding på mottak av utskrivningsklar pasient"),
    FORESPORSEL_HELSEOPPLYSNINGER("8", "Forespørsel om helseopplysninger"),
    FORESPORSEL_LEGEMIDDELOPPLYSNINGER("9", "Forespørsel om legemiddelopplysninger");

    override val kodeverk: String = "2.16.578.1.12.4.1.1.7322"
}