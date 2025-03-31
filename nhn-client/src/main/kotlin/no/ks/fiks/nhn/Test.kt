package no.ks.fiks.nhn

import jakarta.xml.bind.JAXBContext
import no.kith.xmlstds.CV
import no.kith.xmlstds.dialog._2006_10_11.Dialogmelding
import no.kith.xmlstds.dialog._2006_10_11.Foresporsel
import no.kith.xmlstds.msghead._2006_05_24.*
import java.util.*
import javax.xml.XMLConstants
import javax.xml.datatype.DatatypeFactory
import javax.xml.validation.SchemaFactory

fun main() {

    val context = JAXBContext.newInstance(MsgHead::class.java)
    val dialogMeldingContext = JAXBContext.newInstance(Dialogmelding::class.java)
    val headSchema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(ClassLoader.getSystemResource("xsd/MsgHead-v1_2.xsd"))

    val marshaller = context.createMarshaller().apply {
        schema = headSchema
    }

    val dialogMarshaller = dialogMeldingContext.createMarshaller().apply {
        schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(ClassLoader.getSystemResource("xsd/dialogmelding-v1.0.xsd"))
    }

    val dialogUnmarshaller = dialogMeldingContext.createUnmarshaller().apply {
        schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(ClassLoader.getSystemResource("xsd/dialogmelding-v1.0.xsd"))
    }


    val dialogmelding = Dialogmelding().apply {
        foresporsel = listOf(Foresporsel().apply {
            typeForesp = CV()
        })
    }

    // Eksempel Dialogmelding 1.0: https://git.sarepta.ehelse.no/publisert/standarder/raw/master/eksempel/Dialogmelding/Dialogmelding-v1-0/Dialogmelding_foresporsel_PLO_v1-0.xml
    marshaller.marshal(
        MsgHead().apply {
            msgInfo = MsgInfo().apply {
                type = CS().apply {

                }
                miGversion = "v1.2 2006-05-24"
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
                            msgType = CS().apply {

                            }
                            content = RefDoc.Content().apply {
                                this.any.add(dialogmelding)
                            }
                        }
                    },
                    /*Document().apply {
                        refDoc = RefDoc().apply {
                            msgType = CS().apply {
                                dn = "Vedlegg"
                            }
                            content = RefDoc.Content().apply {
                                   this.any.add(Base64Container().apply {
                                       value = java.util.Base64.getEncoder().encode("Test".toByteArray())
                                   })
                            }
                        }
                    }*/
                )
            }
        },
        System.out,
    )


}
