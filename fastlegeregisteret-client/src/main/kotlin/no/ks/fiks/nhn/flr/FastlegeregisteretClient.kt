package no.ks.fiks.nhn.flr

import jakarta.xml.ws.soap.SOAPBinding
import no.nhn.schemas.reg.flr.IFlrReadOperations
import no.nhn.schemas.reg.flr.PatientToGPContractAssociation
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.ws.addressing.WSAddressingFeature

class FastlegeregisteretClient(
    val environment: Environment,
    val credentials: Credentials,
) {

    private val service = JaxWsProxyFactoryBean().apply {
        address = environment.url

        username = credentials.username
        password = credentials.password

        features.add(WSAddressingFeature())
        bindingId = SOAPBinding.SOAP12HTTP_BINDING
    }.create(IFlrReadOperations::class.java)

    fun lookupFastlege(patientId: String): Fastlege? = service.getPatientGPDetails(patientId)?.convert()

    private fun PatientToGPContractAssociation.convert() = Fastlege(
        herId = gpHerId.value,
    )

}