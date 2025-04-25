package no.ks.fiks.nhn.msh

import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.util.JAXBSource
import no.kith.xmlstds.CV
import no.kith.xmlstds.URL
import no.kith.xmlstds.base64container.Base64Container
import no.kith.xmlstds.dialog._2006_10_11.*
import no.kith.xmlstds.felleskomponent1.TeleCom
import no.kith.xmlstds.msghead._2006_05_24.MsgHead
import no.ks.fiks.hdir.HelsepersonellsFunksjoner
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
                    sporsmal = "Se vedlegg?" // TODO: Hva skal stå her?
                    rollerRelatertNotat = listOf(
                        RollerRelatertNotat().apply {
                            roleToPatient = HelsepersonellsFunksjoner.HELSEFAGLIG_KONTAKT.toCV() // TODO: Hvilken type?
                            healthcareProfessional = HealthcareProfessional().apply { // TODO: Hva legger vi her? Navn og telefonnummer er påkrevd for Forespørsel
                                givenName = "Henrikk"
                                familyName = "Helsepersonell"
                                teleCom.add(
                                    TeleCom().apply {
                                        teleAddress = URL().apply {
                                            v = "tel:12345678"
                                        }
                                    }
                                )
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