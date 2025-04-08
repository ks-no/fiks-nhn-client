package no.ks.fiks.nhn

import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.util.JAXBSource
import no.kith.xmlstds.CV
import no.kith.xmlstds.base64container.Base64Container
import no.kith.xmlstds.dialog._2006_10_11.*
import no.kith.xmlstds.msghead._2006_05_24.MsgHead
import javax.xml.XMLConstants
import javax.xml.validation.SchemaFactory

object DialogmeldingBuilder {

    private val context = JAXBContext.newInstance(MsgHead::class.java, Dialogmelding::class.java, Base64Container::class.java)
    private val dialogmeldingSchema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
        .newSchema(ClassLoader.getSystemResource("xsd/dialogmelding-v1.0.xsd"))

    fun buildDialogmelding() = Dialogmelding()
        .apply {
            foresporsel = listOf(
                Foresporsel().apply {
                    typeForesp = CV().apply {
                        v = "HE"  // TODO: Fant bare noe tilfeldig, vet ikke hva vi skal bruke her
                        dn = "Henvendelse"
                        s = "2.16.578.1.12.4.1.1.7601"
                    }
                    sporsmal = "Se vedlegg?"
                    rollerRelatertNotat = listOf(
                        RollerRelatertNotat().apply {
                            roleToPatient = CV().apply {
                                v = "9"
                                dn = "Primærkontakt"
                                s = "2.16.578.1.12.4.1.1.9034"
                            }
                            healthcareProfessional = HealthcareProfessional().apply {

                            }
//                            person = Person().apply {
//
//                            }
                        }
                    )
                }
            )
        }
        .also {
            dialogmeldingSchema.newValidator().validate(JAXBSource(context, it))
        }

}