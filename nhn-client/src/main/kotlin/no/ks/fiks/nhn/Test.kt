package no.ks.fiks.nhn

import jakarta.xml.bind.JAXBContext
import no.kith.xmlstds.msghead._2006_05_24.*
import java.util.*
import javax.xml.XMLConstants
import javax.xml.datatype.DatatypeFactory
import javax.xml.validation.SchemaFactory

fun main() {

    val context = JAXBContext.newInstance(MsgHead::class.java)
    val headSchema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
        .newSchema(ClassLoader.getSystemResource("xsd/MsgHead-v1_2.xsd"))
    val marshaller = context.createMarshaller().apply {
        schema = headSchema
    }

    // Eksempel Dialogmelding 1.0: https://git.sarepta.ehelse.no/publisert/standarder/raw/master/eksempel/Dialogmelding/Dialogmelding-v1-0/Dialogmelding_foresporsel_PLO_v1-0.xml
    marshaller.marshal(
        MsgHead().apply {
            msgInfo = MsgInfo().apply {
                type = CS().apply {

                }
                miGversion = ""
                genDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(GregorianCalendar().apply { time = Date() })
                msgId = UUID.randomUUID().toString()
                sender = Sender().apply {
                    organisation = Organisation().apply {

                    }
                }
                receiver = Receiver().apply {
                    organisation = Organisation().apply {

                    }
                }
                document = listOf(
                    Document().apply {
                        refDoc = RefDoc().apply {
                            msgType = CS()
                        }
                    }
                )
            }
        },
        System.out,
    )
}
