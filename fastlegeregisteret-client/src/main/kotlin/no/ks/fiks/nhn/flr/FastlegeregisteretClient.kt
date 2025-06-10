package no.ks.fiks.nhn.flr

import jakarta.xml.ws.soap.SOAPBinding
import no.nhn.schemas.reg.flr.IFlrReadOperations
import no.nhn.schemas.reg.flr.IFlrReadOperationsGetPatientGPDetailsGenericFaultFaultFaultMessage
import no.nhn.schemas.reg.flr.PatientToGPContractAssociation
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.ws.addressing.WSAddressingFeature

class FastlegeregisteretClient(
    environment: Environment,
    credentials: Credentials,
    private val service: IFlrReadOperations = buildService(environment, credentials)
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

    private var environment: Environment? = null
    private var credentials: Credentials? = null
    private var service: IFlrReadOperations? = null

    fun url(url: String) = apply { this.environment = Environment(url) }
    fun environment(environment: Environment) = apply { this.environment = environment }
    fun credentials(credentials: Credentials) = apply { this.credentials = credentials }
    fun service(service: IFlrReadOperations) = apply { this.service = service }

    fun build(): FastlegeregisteretClient {
        val environment = environment ?: throw IllegalStateException("environment is required")
        val credentials = credentials ?: throw IllegalStateException("credentials is required")
        return FastlegeregisteretClient(
            environment = environment,
            credentials = credentials,
            service = service ?: buildService(environment, credentials),
        )
    }

}

private fun buildService(environment: Environment, credentials: Credentials) = JaxWsProxyFactoryBean().apply {
    address = environment.url

    username = credentials.username
    password = credentials.password

    features.add(WSAddressingFeature())
    bindingId = SOAPBinding.SOAP12HTTP_BINDING
}.create(IFlrReadOperations::class.java)
