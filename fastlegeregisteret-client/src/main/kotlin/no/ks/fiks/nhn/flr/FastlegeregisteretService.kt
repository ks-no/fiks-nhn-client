package no.ks.fiks.nhn.flr

import jakarta.xml.ws.soap.SOAPBinding
import no.nhn.schemas.reg.flr.IFlrReadOperations
import no.nhn.schemas.reg.flr.PatientToGPContractAssociation
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.ws.addressing.WSAddressingFeature

class FastlegeregisteretService(
    url: String,
    credentials: Credentials,
) {

    private val service: IFlrReadOperations = JaxWsProxyFactoryBean().apply {
        address = url

        username = credentials.username
        password = credentials.password

        features.add(WSAddressingFeature())
        bindingId = SOAPBinding.SOAP12HTTP_BINDING
    }.create(IFlrReadOperations::class.java)

    fun getPatientGPDetails(patientId: String): PatientToGPContractAssociation? = service.getPatientGPDetails(patientId)

}
