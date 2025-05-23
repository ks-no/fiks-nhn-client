package no.ks.fiks.nhn.ar

import jakarta.xml.ws.soap.SOAPBinding
import no.nhn.register.communicationparty.*
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
            throw AdresseregisteretException(e.faultInfo?.errorCode?.value, e.faultInfo?.message?.value ?: "No error message was returned")
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
        ?.map {
            PhysicalAddress(
                type = Adressetetype.fromCode(it.type.value?.codeValue?.value),
                streetAddress = it.streetAddress.value,
                postbox = it.postbox.value,
                postalCode = it.postalCode?.toString()?.padStart(4, '0'),
                city = it.city.value,
                country = it.country.value?.codeText?.value,
            )
        }
        ?: emptyList()

}

private fun buildService(environment: Environment, credentials: Credentials): ICommunicationPartyService =
    JaxWsProxyFactoryBean().apply {
        address = environment.url

        username = credentials.username
        password = credentials.password

        features.add(WSAddressingFeature())
        bindingId = SOAPBinding.SOAP12HTTP_BINDING
    }.create(ICommunicationPartyService::class.java)
