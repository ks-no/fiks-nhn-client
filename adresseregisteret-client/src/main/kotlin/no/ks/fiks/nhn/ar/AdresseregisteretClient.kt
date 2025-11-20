package no.ks.fiks.nhn.ar

import jakarta.xml.bind.JAXBElement
import mu.KotlinLogging
import no.nhn.register.communicationparty.ICommunicationPartyServiceGetCommunicationPartyDetailsGenericFaultFaultFaultMessage
import no.nhn.register.communicationparty.Organization
import no.nhn.register.communicationparty.OrganizationPerson
import no.nhn.register.communicationparty.Service
import no.nhn.register.communicationparty.CommunicationParty as NhnCommunicationParty

private val log = KotlinLogging.logger { }

class AdresseregisteretClient(
    private val service: AdresseregisteretService,
) {

    fun lookupHerId(herId: Int): CommunicationParty? =
        try {
            service.getCommunicationPartyDetails(herId)
                ?.let {
                    when (it) {
                        is Organization -> it.convert()
                        is OrganizationPerson -> it.convert()
                        is Service -> it.convert()
                        else -> throw RuntimeException("Unsupported communication party type: ${it::class}")
                    }
                }
        } catch (e: ICommunicationPartyServiceGetCommunicationPartyDetailsGenericFaultFaultFaultMessage) {
            throw AdresseregisteretApiException(
                errorCode = e.faultInfo?.errorCode?.value,
                faultMessage = e.faultInfo?.message?.value,
                message = e.message,
                cause = e,
            )
        } catch (e: Exception) {
            throw AdresseregisteretException(
                message = "Unknown error from Adresseregisteret",
                cause = e,
            )
        }

    fun lookupPostalAddress(herId: Int): PostalAddress =
        lookupHerId(herId)?.let { communicationParty ->
            if (communicationParty.physicalAddresses.isEmpty()) {
                throw AddressNotFoundException("Could not find any physicalAdresses related to herId")
            }
            log.debug("Found ${communicationParty.physicalAddresses.size} addresses for $herId")

            val addressPriority = listOf(PostalAddressType.POSTADRESSE, PostalAddressType.BESOKSADRESSE)

            addressPriority.firstNotNullOfOrNull { type -> communicationParty.physicalAddresses.firstOrNull { it.type == type } }
                ?.toPostalAddress(communicationParty.name)
                ?: throw AddressNotFoundException("Could not find any relevant physicalAdresses related to herId")
        } ?: throw AddressNotFoundException("Did not find any communication party related to herId")


    private fun Organization.convert() = OrganizationCommunicationParty(
        herId = herId,
        name = name.value,
        parent = convertParent(),
        physicalAddresses = convertPhysicalAddresses(),
        electronicAddresses = convertElectronicAddresses(),
        organizationNumber = organizationNumber?.toString()?.padStart(9, '0'),
    )

    private fun OrganizationPerson.convert() = PersonCommunicationParty(
        herId = herId,
        name = name.value,
        parent = convertParent(),
        physicalAddresses = convertPhysicalAddresses(),
        electronicAddresses = convertElectronicAddresses(),
        firstName = person.value.firstName.value,
        middleName = person.value?.middleName?.value,
        lastName = person.value.lastName.value,
    )

    private fun Service.convert() = ServiceCommunicationParty(
        herId = herId,
        name = name.value,
        parent = convertParent(),
        physicalAddresses = convertPhysicalAddresses(),
        electronicAddresses = convertElectronicAddresses(),
    )

    private fun NhnCommunicationParty.convertParent() =
        takeIf { parentHerId != null && parentHerId != -1 }
            ?.run {
                CommunicationPartyParent(
                    herId = parentHerId,
                    name = parentName.value ?: "",
                    organizationNumber = "$parentOrganizationNumber",
                )
            }

    private fun NhnCommunicationParty.convertPhysicalAddresses() = physicalAddresses.value?.physicalAddress
        ?.map { address ->
            PhysicalAddress(
                type = PostalAddressType.fromCode(address.type.value?.codeValue?.value),
                streetAddress = address.streetAddress.valueNotBlank(),
                postbox = address.postbox.valueNotBlank(),
                postalCode = address.postalCode?.toString()?.padStart(4, '0'),
                city = address.city.valueNotBlank(),
                country = address.country.value?.codeText?.valueNotBlank(),
            )
        }
        ?: emptyList()

    private fun NhnCommunicationParty.convertElectronicAddresses() = electronicAddresses?.value?.electronicAddress
        ?.map { address ->
            ElectronicAddress(
                type = AddressComponent.fromCode(address.typeCodeValue?.value),
                address = address.address.value,
                lastChanged = address.lastChanged?.toOffsetDateTime(),
            )
        }
        ?: emptyList()

}

private fun JAXBElement<String>.valueNotBlank() = value?.takeIf { it.isNotBlank() }
