package no.ks.fiks.nhn.ar.rest

import com.github.benmanes.caffeine.cache.Caffeine
import no.nhn.register.communicationparty.rest.model.CommunicationParty as GeneratedCommunicationParty
import no.nhn.register.communicationparty.rest.model.CommunicationPartyType as GeneratedCommunicationPartyType
import no.nhn.register.communicationparty.rest.model.ParentOrganization
import no.nhn.register.communicationparty.rest.model.PostalAddress as GeneratedPostalAddress

class AdresseregisteretRestClient @JvmOverloads constructor(
    private val service: AdresseregisteretRestService,
    cacheConfig: CacheConfig? = null,
) {
    private val cache = cacheConfig
        ?.let { CaffeineCache(config = cacheConfig, loader = ::lookupHerIdFromApi) }
        ?: Cache { herId: Int -> lookupHerIdFromApi(herId) }

    fun lookupHerId(herId: Int): CommunicationParty? = cache.get(herId)

    fun lookupPostalAddress(herId: Int): PostalAddress =
        lookupHerId(herId)?.let { communicationParty ->
            if (communicationParty.physicalAddresses.isEmpty()) {
                throw AddressNotFoundException("Could not find any physicalAdresses related to herId")
            }
            communicationParty.physicalAddresses.firstOrNull()
                ?.toPostalAddress(communicationParty.name)
                ?: throw AddressNotFoundException("Could not find any relevant physicalAdresses related to herId")
        } ?: throw AddressNotFoundException("Did not find any communication party related to herId")

    private fun lookupHerIdFromApi(herId: Int): CommunicationParty? =
        try {
            service.getCommunicationPartyDetails(herId)
                ?.let { it.convert() }
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
            electronicAddresses = convertElectronicAddresses(),
            organizationNumber = organizationDetails?.organizationNumber,
        )
        GeneratedCommunicationPartyType.PERSON -> PersonCommunicationParty(
            herId = herId,
            name = name.orEmpty(),
            parent = personDetails?.parentOrganization?.toParent(),
            physicalAddresses = convertPhysicalAddresses(),
            electronicAddresses = convertElectronicAddresses(),
            firstName = splitPersonName(name).first,
            middleName = splitPersonName(name).second,
            lastName = splitPersonName(name).third,
        )
        GeneratedCommunicationPartyType.SERVICE -> ServiceCommunicationParty(
            herId = herId,
            name = name.orEmpty(),
            parent = serviceDetails?.parentOrganization?.toParent(),
            physicalAddresses = convertPhysicalAddresses(),
            electronicAddresses = convertElectronicAddresses(),
        )
    }

    private fun GeneratedCommunicationParty.convertPhysicalAddresses(): List<PhysicalAddress> =
        listOfNotNull(postalAddress?.toPhysicalAddress())

    private fun GeneratedCommunicationParty.convertElectronicAddresses(): List<ElectronicAddress> = buildList {
        email?.takeIf { it.isNotBlank() }?.let { add(ElectronicAddress(AddressComponent.EPOST, it, null)) }
        homepageUrl?.takeIf { it.isNotBlank() }?.let { add(ElectronicAddress(AddressComponent.HJEMMESIDE, it, null)) }
        phoneNumber?.takeIf { it.isNotBlank() }?.let { add(ElectronicAddress(AddressComponent.TELEFONNUMMER, it, null)) }
        faxNumber?.takeIf { it.isNotBlank() }?.let { add(ElectronicAddress(AddressComponent.FAXNUMMER, it, null)) }
        ediAddress?.takeIf { it.isNotBlank() }?.let { add(ElectronicAddress(AddressComponent.EDI, it, null)) }
        fhirAddress?.takeIf { it.isNotBlank() }?.let { add(ElectronicAddress(AddressComponent.FHIR_ENDEPUNKT, it, null)) }
    }

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

    private fun splitPersonName(fullName: String?): Triple<String, String?, String> {
        val name = fullName.orEmpty().trim()
        if (name.isEmpty()) return Triple("", null, "")
        val parts = name.split(Regex("\\s+"))
        return when {
            parts.size <= 1 -> Triple(parts.firstOrNull() ?: "", null, "")
            parts.size == 2 -> Triple(parts[0], null, parts[1])
            else -> Triple(parts.first(), parts.subList(1, parts.size - 1).joinToString(" "), parts.last())
        }
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

        override fun get(herId: Int) = cache.get(herId)
    }
}
