package no.ks.fiks.nhn.edi

import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.Marshaller
import jakarta.xml.bind.Unmarshaller
import jakarta.xml.bind.util.JAXBSource
import no.kith.xmlstds.base64container.Base64Container
import no.kith.xmlstds.msghead._2006_05_24.MsgHead
import no.ks.fiks.nhn.edi.v1_0.AppRecDeserializer
import org.w3._2000._09.xmldsig_.ObjectFactory
import java.io.StringReader
import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory
import no.kith.xmlstds.dialog._2006_10_11.Dialogmelding as Dialogmelding1_0
import no.kith.xmlstds.dialog._2013_01_23.Dialogmelding as Dialogmelding1_1

object XmlContext {

    private val context = JAXBContext.newInstance(
        MsgHead::class.java,
        Base64Container::class.java,
        Dialogmelding1_0::class.java,
        Dialogmelding1_1::class.java,
        ObjectFactory::class.java,
    )
    private val headSchema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
        .newSchema(
            arrayOf(
                StreamSource(ClassLoader.getSystemResourceAsStream("xsd/xmldsig-core-schema.xsd")),
                StreamSource(ClassLoader.getSystemResourceAsStream("xsd/kith.xsd")),
                StreamSource(ClassLoader.getSystemResourceAsStream("xsd/kith-base64.xsd")),
                StreamSource(ClassLoader.getSystemResourceAsStream("xsd/felleskomponent1.xsd")),
                StreamSource(ClassLoader.getSystemResourceAsStream("xsd/MsgHead-v1_2.xsd")),
                StreamSource(ClassLoader.getSystemResourceAsStream("xsd/dialogmelding-v1.0.xsd")),
                StreamSource(ClassLoader.getSystemResourceAsStream("xsd/dialogmelding-v1.1.xsd")),
            )
        )

    fun createMarshaller(): Marshaller = context.createMarshaller()

    fun createUnmarshaller(): Unmarshaller = context.createUnmarshaller()

    fun validateObject(content: Any) {
        headSchema.newValidator().validate(JAXBSource(context, content))
    }

    fun validateXml(xml: String) {
        headSchema.newValidator().validate(StreamSource(StringReader(xml)))
    }

}