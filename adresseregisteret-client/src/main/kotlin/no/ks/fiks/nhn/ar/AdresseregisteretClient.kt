package no.ks.fiks.nhn.ar

import jakarta.xml.ws.soap.SOAPBinding
import no.nhn.register.communicationparty.ICommunicationPartyService
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.ws.addressing.WSAddressingFeature
import no.nhn.register.communicationparty.CommunicationParty as NhnCommunicationParty

class AdresseregisteretClient(
    environment: Environment,
    credentials: Credentials,
) {

    private val service = JaxWsProxyFactoryBean().apply {
        address = environment.url

        username = credentials.username
        password = credentials.password

        features.add(WSAddressingFeature())
        bindingId = SOAPBinding.SOAP12HTTP_BINDING
    }.create(ICommunicationPartyService::class.java)

    fun lookupHerId(herId: Int): CommunicationParty? =
        service.getCommunicationPartyDetails(herId)
            ?.convert()

    private fun NhnCommunicationParty.convert() = CommunicationParty(
        herId = herId,
        name = name.value,
        parent = CommunicationPartyParent(
            herId = parentHerId,
            name = parentName.value,
        ),
        physicalAddresses = physicalAddresses.value?.physicalAddress
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
    )

}