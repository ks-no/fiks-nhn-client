package no.ks.fiks.hdir


// Kodeverk: 9034 Helsepersonells funksjoner mv.
enum class HelsepersonellsFunksjoner(
    override val verdi: String,
    override val navn: String,
) : KodeverkVerdi {
    BEHANDLINGSANSVARLIG_LEGE("3", "Behandlingsansvarlig lege"),
    JOURNALANSVARLIG("4", "Journalansvarlig"),
    INFORMASJONSANSVARLIG("5", "Informasjonsansvarlig"),
    FASTLEGE("6", "Fastlege"),
    FAGLIG_ANSVARLIG_VEDTAK_PSYKISK_HELSEVERN("7", "Faglig ansvarlig for vedtak i psykisk helsevern"),
    KOORDINATOR_INDIVIDUELL_PLAN("8", "Koordinator Individuell plan"),
    PRIMAERKONTAKT("9", "Primærkontakt"),
    UTSKRIVENDE_LEGE("10", "Utskrivende lege"),
    UTSKRIVENDE_SYKEPLEIER("11", "Utskrivende sykepleier"),
    INSTITUERENDE_LEGE("12", "Instituerende lege"),
    INNLEGGENDE_LEGE("13", "Innleggende lege"),
    ANSVARLIG_JORDMOR("14", "Ansvarlig jordmor"),
    VIKAR_FASTLEGE("15", "Vikar for fastlege"),
    TURNUSLEGE("16", "Turnuslege"),
    FORLOPSKOORDINATOR_KREFT("17", "Forløpskoordinator kreft"),
    KOORDINATOR("18", "Koordinator (spesialisthelsetjenesteloven)"),
    KONTAKTLEGE("19", "Kontaktlege"),
    KONTAKTPSYKOLOG("20", "Kontaktpsykolog"),
    HELSEFAGLIG_KONTAKT("21", "Helsefaglig kontakt"),
    KONTAKT_PRIMAERHELSETEAM("22", "Kontakt i primærhelseteam"),
    ONSKET_BEHANDLER("23", "Ønsket behandler"),
    KONTAKT_HOS_MOTTAKER("24", "Kontakt hos mottaker");

    override val kodeverk: String = "2.16.578.1.12.4.1.1.9034"

}
