package no.ks.fiks.nhn.fastlege

import jakarta.xml.ws.soap.SOAPBinding
import no.nhn.schemas.reg.orchestrationv2.IOrchestrationService2
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.ws.addressing.WSAddressingFeature

fun main() {
    val service = JaxWsProxyFactoryBean().apply {
        serviceClass = IOrchestrationService2::class.java
        address = "https://register-web.test.nhn.no/v2/Orchestration"

        username = ""
        password = ""

        features.add(WSAddressingFeature())
        bindingId = SOAPBinding.SOAP12HTTP_BINDING
    }.create(IOrchestrationService2::class.java)

    val ping = service.ping("hello")
    println(ping)

    val details = service.getGPCommunicationDetails("")
    println(details)

}
