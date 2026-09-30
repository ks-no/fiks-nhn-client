package no.ks.fiks.nhn.ar.rest

import com.github.benmanes.caffeine.cache.Caffeine
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import no.ks.fiks.nhn.ar.rest.model.CommunicationParty
import no.ks.fiks.nhn.ar.rest.model.PostalAddress

class AdresseregisteretClient @JvmOverloads constructor(
    private val service: AdresseregisteretService,
    cacheConfig: CacheConfig? = null,
) {
    private val cache = cacheConfig
        ?.let { CaffeineCache(config = cacheConfig, loader = ::lookupHerIdFromApi) }
        ?: Cache { herId: Int -> lookupHerIdFromApi(herId)?.deepCopy() }

    fun lookupHerId(herId: Int): CommunicationParty? = cache.get(herId)

    fun lookupPostalAddress(herId: Int): PostalAddress =
        lookupHerId(herId)?.let { communicationParty ->
            communicationParty.postalAddress
                ?: throw AddressNotFoundException("Could not find any postal addresses related to herId")
        } ?: throw AddressNotFoundException("Did not find any communication party related to herId")

    private fun lookupHerIdFromApi(herId: Int): CommunicationParty? =
        try {
            service.getCommunicationPartyDetails(herId)
        } catch (_: feign.FeignException.NotFound) {
            throw AddressNotFoundException("Could not find any communication party related to herId")
        } catch (e: feign.FeignException) {
            throw AdresseregisteretApiException(
                statusCode = e.status(),
                message = "Error from Adresseregisteret REST API: ${e.message}",
                cause = e,
            )
        } catch (e: Exception) {
            throw AdresseregisteretException(
                message = "Unknown error from Adresseregisteret REST API",
                cause = e,
            )
        }


    private fun interface Cache {
        fun get(herId: Int): CommunicationParty?
    }

    private class CaffeineCache(
        config: CacheConfig,
        loader: (Int) -> CommunicationParty?,
    ) : Cache {
        private val cache = Caffeine.newBuilder()
            .maximumSize(config.maxSize)
            .expireAfterWrite(config.cacheTtl)
            .build<Int, CommunicationParty?> { herId -> loader.invoke(herId) }

        override fun get(herId: Int) = cache.get(herId)?.deepCopy()
    }
}

private val copyMapper = ObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .disable(com.fasterxml.jackson.databind.DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)

private fun CommunicationParty.deepCopy(): CommunicationParty =
    copyMapper.readValue(copyMapper.writeValueAsBytes(this), CommunicationParty::class.java)

