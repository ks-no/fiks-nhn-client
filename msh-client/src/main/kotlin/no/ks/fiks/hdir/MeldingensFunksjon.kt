package no.ks.fiks.hdir

// Kodeverk: 8279 Meldingens funksjon
enum class MeldingensFunksjon(
    override val verdi: String,
    override val navn: String,
) : KodeverkVerdi {

    // Dialogmelding 1.0
    DIALOG_FORESPORSEL("DIALOG_FORESPORSEL", "Forespørsel"),
    DIALOG_SVAR("DIALOG_SVAR", "Svar på forespørsel"),
    DIALOG_NOTAT("DIALOG_NOTAT", "Notat"), // Removed in the latest revision of the standard, but still supported for backward compatibility

    // Dialogmelding 1.1
    DIALOG_HELSEFAGLIG("DIALOG_HELSEFAGLIG", "Helsefaglig dialog");

    override val kodeverk: String = "2.16.578.1.12.4.1.1.8279"
}
