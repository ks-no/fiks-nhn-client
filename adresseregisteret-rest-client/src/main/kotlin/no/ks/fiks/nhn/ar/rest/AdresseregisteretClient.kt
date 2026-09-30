package no.ks.fiks.nhn.ar.rest

import com.github.benmanes.caffeine.cache.Caffeine
import no.ks.fiks.nhn.ar.rest.model.AdministrativeCode
import no.ks.fiks.nhn.ar.rest.model.AmqpAddress
import no.ks.fiks.nhn.ar.rest.model.CertificateMetadata
import no.ks.fiks.nhn.ar.rest.model.CommunicationParty
import no.ks.fiks.nhn.ar.rest.model.InterMunicipalityCoverageArea
import no.ks.fiks.nhn.ar.rest.model.OrganizationDetails
import no.ks.fiks.nhn.ar.rest.model.ParentOrganization
import no.ks.fiks.nhn.ar.rest.model.PersonDetails
import no.ks.fiks.nhn.ar.rest.model.PostalAddress
import no.ks.fiks.nhn.ar.rest.model.ServiceDetails

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

private fun CommunicationParty.deepCopy(): CommunicationParty =
    let { source ->
        CommunicationParty().apply {
            herId(source.herId)
            name(source.name)
            displayName(source.displayName)
            type(source.type)
            organizationDetails(source.organizationDetails?.deepCopy())
            personDetails(source.personDetails?.deepCopy())
            serviceDetails(source.serviceDetails?.deepCopy())
            currentSigningCertificate(source.currentSigningCertificate?.deepCopy())
            currentEncryptionCertificate(source.currentEncryptionCertificate?.deepCopy())
            email(source.email)
            homepageUrl(source.homepageUrl)
            phoneNumber(source.phoneNumber)
            faxNumber(source.faxNumber)
            ediAddress(source.ediAddress)
            fhirAddress(source.fhirAddress)
            postalAddress(source.postalAddress?.deepCopy())
            amqpTransportStatus(source.amqpTransportStatus)
            amqpAddress(source.amqpAddress?.deepCopy())
            validFrom(source.validFrom)
            validTo(source.validTo)
        }
    }

private fun OrganizationDetails.deepCopy(): OrganizationDetails =
    let { source ->
        OrganizationDetails().apply {
            organizationNumber(source.organizationNumber)
            businessType(source.businessType.deepCopy())
            persons(source.persons?.toList())
            services(source.services?.toList())
        }
    }

private fun PersonDetails.deepCopy(): PersonDetails =
    let { source ->
        PersonDetails().apply {
            hprNumber(source.hprNumber)
            parentOrganization(source.parentOrganization.deepCopy())
        }
    }

private fun ServiceDetails.deepCopy(): ServiceDetails =
    let { source ->
        ServiceDetails().apply {
            serviceType(source.serviceType.deepCopy())
            interMunicipalityCoverageArea(source.interMunicipalityCoverageArea?.deepCopy())
            serviceSpecification(source.serviceSpecification)
            parentOrganization(source.parentOrganization.deepCopy())
        }
    }

private fun ParentOrganization.deepCopy(): ParentOrganization =
    let { source ->
        ParentOrganization().apply {
            name(source.name)
            herId(source.herId)
            organizationNumber(source.organizationNumber)
        }
    }

private fun PostalAddress.deepCopy(): PostalAddress =
    let { source ->
        PostalAddress().apply {
            address(source.address)
            postalBox(source.postalBox)
            postalCode(source.postalCode)
            city(source.city)
        }
    }

private fun AdministrativeCode.deepCopy(): AdministrativeCode =
    let { source ->
        AdministrativeCode().apply {
            codeListId(source.codeListId)
            value(source.value)
            name(source.name)
            url(source.url)
        }
    }

private fun CertificateMetadata.deepCopy(): CertificateMetadata =
    let { source ->
        CertificateMetadata().apply {
            thumbprint(source.thumbprint)
            validFrom(source.validFrom)
            validTo(source.validTo)
        }
    }

private fun AmqpAddress.deepCopy(): AmqpAddress =
    let { source ->
        AmqpAddress().apply {
            amqpSyncQueue(source.amqpSyncQueue)
            amqpSyncReplyQueue(source.amqpSyncReplyQueue)
            amqpAsyncQueue(source.amqpAsyncQueue)
            amqpErrorQueue(source.amqpErrorQueue)
        }
    }

private fun InterMunicipalityCoverageArea.deepCopy(): InterMunicipalityCoverageArea =
    let { source ->
        InterMunicipalityCoverageArea().apply {
            municipalityHerIds(source.municipalityHerIds?.toList())
        }
    }

