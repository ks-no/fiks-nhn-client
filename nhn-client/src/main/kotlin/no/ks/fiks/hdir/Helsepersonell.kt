package no.ks.fiks.hdir


// Kodeverk: 9060 Kategori helsepersonell
enum class Helsepersonell(
    override val verdi: String,
    override val navn: String,
): KodeverkVerdi {
    AMBULANSEARBEIDER("AA", "Ambulansearbeider"),
    APOTEKTEKNIKER("AT", "Apotektekniker"),
    AUDIOGRAF("AU", "Audiograf"),
    BIOINGENIOR("BI", "Bioingeniør"),
    ERGOTERAPEUT("ET", "Ergoterapeut"),
    PROVISORFARMASOYT("FA1", "Provisorfarmasøyt"),
    RESEPTARFARMASOYT("FA2", "Reseptarfarmasøyt"),
    FISKEHELSEBIOLOG("FB", "Fiskehelsebiolog"),
    FOTTERAPEUT("FO", "Fotterapeut"),
    FYSIOTERAPEUT("FT", "Fysioterapeut"),
    HELSESEKRETAER("HE", "Helsesekretær"),
    HELSEFAGARBEIDER("HF", "Helsefagarbeider"),
    HJELPEPLEIER("HP", "Hjelpepleier"),
    JORDMOR("JO", "Jordmor"),
    KLINISK_ERNAERINGSFYSIOLOG("KE", "Klinisk ernæringsfysiolog"),
    KIROPRAKTOR("KI", "Kiropraktor"),
    LEGE("LE", "Lege"),
    MANUELLTERAPEUT("MT", "Manuellterapeut"),
    NAPRAPAT("NP", "Naprapat"),
    OMSORGSARBEIDER("OA", "Omsorgsarbeider"),
    ORTOPEDIINGENIOR("OI", "Ortopediingeniør"),
    OPTIKER("OP", "Optiker"),
    ORTOPTIST("OR", "Ortoptist"),
    OSTEOPAT("OS", "Osteopat"),
    PERFUSJONIST("PE", "Perfusjonist"),
    PARAMEDISINER("PM", "Paramedisiner"),
    PSYKOLOG("PS", "Psykolog"),
    RADIOGRAF("RA", "Radiograf"),
    SYKEPLEIER("SP", "Sykepleier"),
    TANNHELSESEKRETAER("TH", "Tannhelsesekretær"),
    TANNLEGE("TL", "Tannlege"),
    TANNPLEIER("TP", "Tannpleier"),
    TANNTEKNIKER("TT", "Tanntekniker"),
    VETERINAER("VE", "Veterinær"),
    VERNEPLEIER("VP", "Vernepleier"),
    UKJENT("XX", "Ukjent/uspesifisert");


    override val kodeverk: String = "2.16.578.1.12.4.1.1.9060"

}
