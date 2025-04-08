package no.ks.fiks.nhn

/**
 *  Kodeverk: 8279 Meldingens funksjon
 */
enum class MessageType(
    val verdi: String,
    val navn: String,
) {
    DIALOG_FORESPORSEL("DIALOG_FORESPORSEL", "Forespørsel"),
}