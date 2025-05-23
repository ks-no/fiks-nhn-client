package no.ks.fiks.nhn.ar

import jakarta.xml.bind.JAXBElement
import jakarta.xml.ws.soap.SOAPBinding
import no.nhn.register.communicationparty.*
import org.apache.cxf.feature.LoggingFeature
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.ws.addressing.WSAddressingFeature
import no.nhn.register.communicationparty.CommunicationParty as NhnCommunicationParty

class AdresseregisteretClient(
    environment: Environment,
    credentials: Credentials,
    private val service: ICommunicationPartyService = buildService(environment, credentials),
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
            throw AdresseregisteretException(e.faultInfo?.errorCode?.value, e.faultInfo?.message?.value, e.message)
        }

    private fun Organization.convert() = OrganizationCommunicationParty(
        herId = herId,
        parent = convertParent(),
        physicalAddresses = convertPhysicalAddresses(),
        organizationNumber = organizationNumber?.toString()?.padStart(9, '0'),
    )

    private fun OrganizationPerson.convert() = PersonCommunicationParty(
        herId = herId,
        parent = convertParent(),
        physicalAddresses = convertPhysicalAddresses(),
        firstName = person.value.firstName.value,
        middleName = person.value?.middleName?.value,
        lastName = person.value.lastName.value,
    )

    private fun Service.convert() = ServiceCommunicationParty(
        herId = herId,
        parent = convertParent(),
        physicalAddresses = convertPhysicalAddresses(),
    )

    private fun NhnCommunicationParty.convertParent() =
        takeIf { parentHerId != null && parentHerId != -1 }
            ?.run {
                CommunicationPartyParent(
                    herId = parentHerId,
                    name = parentName.value ?: "",
                )
            }

    private fun NhnCommunicationParty.convertPhysicalAddresses() = physicalAddresses.value?.physicalAddress
        ?.map { address ->
            PhysicalAddress(
                type = Adressetetype.fromCode(address.type.value?.codeValue?.value),
                streetAddress = address.streetAddress.valueNotBlank(),
                postbox = address.postbox.valueNotBlank(),
                postalCode = address.postalCode?.toString()?.padStart(4, '0'),
                city = address.city.valueNotBlank(),
                country = address.country.value?.codeText?.valueNotBlank(),
            )
        }
        ?: emptyList()

}

private fun JAXBElement<String>.valueNotBlank() = value?.takeIf { it.isNotBlank() }

private fun buildService(environment: Environment, credentials: Credentials): ICommunicationPartyService =
    JaxWsProxyFactoryBean().apply {
        address = environment.url

        username = credentials.username
        password = credentials.password

        features.add(WSAddressingFeature())
        bindingId = SOAPBinding.SOAP12HTTP_BINDING
    }.create(ICommunicationPartyService::class.java)
