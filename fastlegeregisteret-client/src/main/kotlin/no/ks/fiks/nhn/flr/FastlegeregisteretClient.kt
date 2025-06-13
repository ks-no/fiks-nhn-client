package no.ks.fiks.nhn.flr

import jakarta.xml.ws.soap.SOAPBinding
import no.nhn.schemas.reg.flr.IFlrReadOperations
import no.nhn.schemas.reg.flr.IFlrReadOperationsGetPatientGPDetailsGenericFaultFaultFaultMessage
import no.nhn.schemas.reg.flr.PatientToGPContractAssociation
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.ws.addressing.WSAddressingFeature

class FastlegeregisteretClient(
    url: String,
    credentials: Credentials,
    private val service: IFlrReadOperations = buildService(url, credentials)
) {

    fun getPatientGP(patientId: String): PatientGP? =
        try {
            service.getPatientGPDetails(patientId)?.convert()
        } catch (e: IFlrReadOperationsGetPatientGPDetailsGenericFaultFaultFaultMessage) {
            throw FastlegeregisteretException(e.faultInfo?.errorCode?.value, e.faultInfo?.message?.value, e.message)
        }

    private fun PatientToGPContractAssociation.convert() = PatientGP(
        patientId = patientNIN.value,
        gpHerId = gpHerId.value,
    )

}

class FastlegeregisteretClientBuilder {

    private var url: String? = null
    private var credentials: Credentials? = null
    private var service: IFlrReadOperations? = null

    fun url(url: String) = apply { this.url = url }
    fun credentials(credentials: Credentials) = apply { this.credentials = credentials }
    fun service(service: IFlrReadOperations) = apply { this.service = service }

    fun build(): FastlegeregisteretClient {
        val url = url ?: throw IllegalStateException("url is required")
        val credentials = credentials ?: throw IllegalStateException("credentials is required")
        return FastlegeregisteretClient(
            url = url,
            credentials = credentials,
            service = service ?: buildService(url, credentials),
        )
    }

}

private fun buildService(url: String, credentials: Credentials) = JaxWsProxyFactoryBean().apply {
    address = url

    username = credentials.username
    password = credentials.password

    features.add(WSAddressingFeature())
    bindingId = SOAPBinding.SOAP12HTTP_BINDING
}.create(IFlrReadOperations::class.java)
