package no.ks.fiks.hdir


// Kodeverk: 9152 Type opplysning ved pasientsamhandling - fra Pleie- og omsorg
enum class TypeOpplysningPasientsamhandling(
    override val verdi: String,
    override val navn: String,
) : KodeverkVerdi {
    HELSEOPPLYSNINGER("1", "OK"),
    LEGEMIDDELOPPLYSNINGER("4", "Legemiddelopplysninger"),
    FORNYE_RESEPT("6", "Fornye resept(er)."),
    TIME_TIL_BEHANDLING("7", "Time til undersøkelse/behandling"),
    STATUS_FOR_UTSKRIVNING("8", "Status/plan for utskrivning"),
    ANNEN_HENVENDELSE("99", "Annen henvendelse");

    override val kodeverk: String = "2.16.578.1.12.4.1.1.9152"
}