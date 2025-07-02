package no.ks.fiks.nhn.ar

import jakarta.xml.ws.soap.SOAPBinding
import no.nhn.register.communicationparty.CommunicationParty
import no.nhn.register.communicationparty.ICommunicationPartyService
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.ws.addressing.WSAddressingFeature

class AdresseregisteretService(
    url: String,
    credentials: Credentials,
) {

    fun getCommunicationPartyDetails(herId: Int): CommunicationParty? = soapService.getCommunicationPartyDetails(herId)

    private val soapService: ICommunicationPartyService =
        JaxWsProxyFactoryBean().apply {
            address = url

            username = credentials.username
            password = credentials.password

            features.add(WSAddressingFeature())
            bindingId = SOAPBinding.SOAP12HTTP_BINDING
        }.create(ICommunicationPartyService::class.java)


}
