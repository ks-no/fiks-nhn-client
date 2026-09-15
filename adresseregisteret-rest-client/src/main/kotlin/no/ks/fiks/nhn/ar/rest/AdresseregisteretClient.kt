package no.ks.fiks.nhn.ar.rest

class AdresseregisteretClient @JvmOverloads constructor(
    private val service: AdresseregisteretRestService,
    cacheConfig: CacheConfig? = null,
) {
    private val delegate = AdresseregisteretRestClient(service, cacheConfig)

    fun lookupHerId(herId: Int): CommunicationParty? = delegate.lookupHerId(herId)

    fun lookupPostalAddress(herId: Int): PostalAddress = delegate.lookupPostalAddress(herId)
}
