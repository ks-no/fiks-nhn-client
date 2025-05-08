package no.ks.fiks.nhn.msh

import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.Marshaller
import jakarta.xml.bind.Unmarshaller
import jakarta.xml.bind.util.JAXBSource
import no.kith.xmlstds.apprec._2004_11_21.AppRec
import no.kith.xmlstds.base64container.Base64Container
import no.kith.xmlstds.dialog._2006_10_11.Dialogmelding
import no.kith.xmlstds.msghead._2006_05_24.MsgHead
import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory

object XmlContext {

    private val context = JAXBContext.newInstance(MsgHead::class.java, Dialogmelding::class.java, Base64Container::class.java, AppRec::class.java)
    private val headSchema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
        .newSchema(
            arrayOf(
                StreamSource(ClassLoader.getSystemResourceAsStream("xsd/kith.xsd")),
                StreamSource(ClassLoader.getSystemResourceAsStream("xsd/felleskomponent1.xsd")),
                StreamSource(ClassLoader.getSystemResourceAsStream("xsd/MsgHead-v1_2.xsd")),
                StreamSource(ClassLoader.getSystemResourceAsStream("xsd/dialogmelding-v1.0.xsd")),
            )
        )

    fun createMarshaller(): Marshaller = context.createMarshaller()

    fun createUnmarshaller(): Unmarshaller = context.createUnmarshaller()

    fun validate(content: Any) {
        headSchema.newValidator().validate(JAXBSource(context, content))
    }

}