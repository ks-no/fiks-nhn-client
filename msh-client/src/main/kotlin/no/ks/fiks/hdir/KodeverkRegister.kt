package no.ks.fiks.hdir

object KodeverkRegister {

    private val register = listOf(
        FeilmeldingForApplikasjonskvittering.entries,
        HelsepersonellsFunksjoner.entries,
        MeldingensFunksjon.entries,
        TypeDokumentreferanse.entries,
        OrganizationIdType.entries,
        PersonIdType.entries,
    ).flatten()
        .associate {
            KodeverkVerdiKey(
                it.verdi,
                it.kodeverk,
            ) to it as KodeverkVerdi
        }

    fun getKodeverk(kodeverk: String, verdi: String): KodeverkVerdi {
        val key = KodeverkVerdiKey(verdi, kodeverk)
        return register[key]
            ?: throw IllegalArgumentException("No kodeverk exists for key $key")
    }

}

private data class KodeverkVerdiKey(
    val verdi: String,
    val kodeverk: String
)
