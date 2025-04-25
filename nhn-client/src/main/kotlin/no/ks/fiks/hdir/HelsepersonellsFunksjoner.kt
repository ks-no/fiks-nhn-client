package no.ks.fiks.hdir

import no.kith.xmlstds.CV


// Kodeverk: 9034 Helsepersonells funksjoner mv.
enum class HelsepersonellsFunksjoner(
    override val verdi: String,
    override val navn: String,
): KodeverkVerdi {
    HELSEFAGLIG_KONTAKT("21", "Helsefaglig kontakt");

    override val kodeverk: String = "2.16.578.1.12.4.1.1.9034"

    fun toCV() = CV().apply {
        v = verdi
        dn = navn
        s = kodeverk
    }
}
