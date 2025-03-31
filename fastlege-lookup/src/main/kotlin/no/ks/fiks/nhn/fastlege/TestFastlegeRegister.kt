package no.ks.fiks.nhn.fastlege

import jakarta.xml.ws.soap.SOAPBinding
import no.nhn.schemas.reg.flr.IFlrReadOperations
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.ws.addressing.WSAddressingFeature

fun main() {

    val service = JaxWsProxyFactoryBean().apply {
        serviceClass = IFlrReadOperations::class.java
        address = "https://register-web.test.nhn.no/v2/flr"

        username = ""
        password = ""

        features.add(WSAddressingFeature())
        bindingId = SOAPBinding.SOAP12HTTP_BINDING
    }.create(IFlrReadOperations::class.java)


    val details = service.getPatientGPDetails("27927298703")
    details.toString()

}
