package no.ks.fiks.hdir

// Kodeverk: 8279 Meldingens funksjon
enum class MeldingensFunksjon(
    override val verdi: String,
    override val navn: String,
) : KodeverkVerdi {
    DIALOG_FORESPORSEL("DIALOG_FORESPORSEL", "Forespørsel"),
    DIALOG_HELSEFAGLIG("DIALOG_HELSEFAGLIG", "Helsefaglig dialog"),
    DIALOG_SVAR("DIALOG_SVAR", "Svar på forespørsel");

    override val kodeverk: String = "2.16.578.1.12.4.1.1.8279"
}
