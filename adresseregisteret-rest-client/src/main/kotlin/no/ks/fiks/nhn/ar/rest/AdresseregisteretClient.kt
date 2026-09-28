package no.ks.fiks.nhn.ar.rest

import com.github.benmanes.caffeine.cache.Caffeine
import no.nhn.register.communicationparty.rest.model.CommunicationParty as GeneratedCommunicationParty
import no.nhn.register.communicationparty.rest.model.CommunicationPartyType as GeneratedCommunicationPartyType
import no.nhn.register.communicationparty.rest.model.ParentOrganization
import no.nhn.register.communicationparty.rest.model.PostalAddress as GeneratedPostalAddress

class AdresseregisteretClient @JvmOverloads constructor(
    private val service: AdresseregisteretService,
    cacheConfig: CacheConfig? = null,
) {
    private val cache = cacheConfig
        ?.let { CaffeineCache(config = cacheConfig, loader = ::lookupHerIdFromApi) }
        ?: Cache { herId: Int -> lookupHerIdFromApi(herId) }

    fun lookupHerId(herId: Int): CommunicationParty? = cache.get(herId)

    fun lookupPostalAddress(herId: Int): PostalAddress =
        lookupHerId(herId)?.let { communicationParty ->
            if (communicationParty.physicalAddresses.isEmpty()) {
                throw AddressNotFoundException("Could not find any physical addresses related to herId")
            }
            communicationParty.physicalAddresses.firstOrNull()
                ?.toPostalAddress(communicationParty.name)
                ?: throw AddressNotFoundException("Could not find any relevant physical addresses related to herId")
        } ?: throw AddressNotFoundException("Did not find any communication party related to herId")

     private fun lookupHerIdFromApi(herId: Int): CommunicationParty? =
         try {
             service.getCommunicationPartyDetails(herId)?.convert()
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

    private fun GeneratedCommunicationParty.convert() = when (type) {
        GeneratedCommunicationPartyType.ORGANIZATION -> OrganizationCommunicationParty(
            herId = herId,
            name = name.orEmpty(),
            parent = null,
            physicalAddresses = convertPhysicalAddresses(),
            email = email,
            homepageUrl = homepageUrl,
            phoneNumber = phoneNumber,
            faxNumber = faxNumber,
            ediAddress = ediAddress,
            fhirAddress = fhirAddress,
            organizationNumber = organizationDetails?.organizationNumber,
        )
        GeneratedCommunicationPartyType.PERSON -> PersonCommunicationParty(
            herId = herId,
            name = name.orEmpty(),
            parent = personDetails?.parentOrganization?.toParent(),
            physicalAddresses = convertPhysicalAddresses(),
            email = email,
            homepageUrl = homepageUrl,
            phoneNumber = phoneNumber,
            faxNumber = faxNumber,
            ediAddress = ediAddress,
            fhirAddress = fhirAddress,
        )
        GeneratedCommunicationPartyType.SERVICE -> ServiceCommunicationParty(
            herId = herId,
            name = name.orEmpty(),
            parent = serviceDetails?.parentOrganization?.toParent(),
            physicalAddresses = convertPhysicalAddresses(),
            email = email,
            homepageUrl = homepageUrl,
            phoneNumber = phoneNumber,
            faxNumber = faxNumber,
            ediAddress = ediAddress,
            fhirAddress = fhirAddress,
        )
    }

    private fun GeneratedCommunicationParty.convertPhysicalAddresses(): List<PhysicalAddress> =
        listOfNotNull(postalAddress?.toPhysicalAddress())


    private fun ParentOrganization.toParent() = CommunicationPartyParent(
        herId = herId,
        name = name.orEmpty(),
        organizationNumber = organizationNumber.orEmpty(),
    )

    private fun GeneratedPostalAddress.toPhysicalAddress() = PhysicalAddress(
        type = PostalAddressType.POSTADRESSE,
        streetAddress = address,
        postbox = postalBox,
        postalCode = postalCode?.padStart(4, '0'),
        city = city,
        country = null,
    )


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

        override fun get(herId: Int) = cache.get(herId)
    }
}

