package no.ks.fiks.hdir

import no.kith.xmlstds.msghead._2006_05_24.CS


// Kodeverk: 8114 Type dokumentreferanse
enum class TypeDokumentreferanse(
    override val verdi: String,
    override val navn: String,
): KodeverkVerdi {
    VEDLEGG("A", "Vedlegg"),
    REF("REF", "Referanse"),
    XML("XML", "XML-instans");

    override val kodeverk: String = "2.16.578.1.12.4.1.1.8114"

    fun toCS() = CS().apply {
        v = verdi
        dn = navn
    }

}
