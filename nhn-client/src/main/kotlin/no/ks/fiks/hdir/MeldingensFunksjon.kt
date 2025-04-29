package no.ks.fiks.hdir

// Kodeverk: 8279 Meldingens funksjon
enum class MeldingensFunksjon(
    override val verdi: String,
    override val navn: String,
) : KodeverkVerdi {
    DIALOG_FORESPORSEL("DIALOG_FORESPORSEL", "Forespørsel"),;

    override val kodeverk: String = "2.16.578.1.12.4.1.1.8279"
}
