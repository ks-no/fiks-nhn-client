package no.ks.fiks.hdir

object KodeverkRegister {

    private val register = listOf(
        FeilmeldingForApplikasjonskvittering.entries,
        HelsepersonellsFunksjoner.entries,
        MeldingensFunksjon.entries,
        TypeDokumentreferanse.entries,
        OrganisasjonIdType.entries,
        PersonIdType.entries,
    ).flatten()
        .associate {
            KodeverkVerdiKey(
                it.verdi,
                it.navn,
                it.kodeverk
            ) to it as KodeverkVerdi
        }

    fun getKodeverk(kodeverk: String, verdi: String, navn: String): KodeverkVerdi {
        val key = KodeverkVerdiKey(verdi, navn, kodeverk)
        return register[key]
            ?: throw IllegalArgumentException("No kodeverk exists for key $key")
    }

}

private data class KodeverkVerdiKey(
    override val verdi: String,
    override val navn: String,
    override val kodeverk: String
) : KodeverkVerdi
