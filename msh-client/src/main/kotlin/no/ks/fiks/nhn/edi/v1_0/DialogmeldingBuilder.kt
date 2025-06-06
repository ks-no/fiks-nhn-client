package no.ks.fiks.nhn.edi.v1_0

import no.kith.xmlstds.CV
import no.kith.xmlstds.URL
import no.kith.xmlstds.dialog._2006_10_11.Dialogmelding
import no.kith.xmlstds.dialog._2006_10_11.Foresporsel
import no.kith.xmlstds.dialog._2006_10_11.HealthcareProfessional
import no.kith.xmlstds.dialog._2006_10_11.RollerRelatertNotat
import no.kith.xmlstds.felleskomponent1.TeleCom
import no.ks.fiks.hdir.TypeOpplysningPasientsamhandling
import no.ks.fiks.nhn.edi.toCV
import no.ks.fiks.nhn.msh.OutgoingMessage

object DialogmeldingBuilder {

    fun buildDialogmelding(message: OutgoingMessage) = Dialogmelding()
        .apply {
            foresporsel = listOf(
                Foresporsel().apply {
                    typeForesp = TypeOpplysningPasientsamhandling.ANNEN_HENVENDELSE.let {
                        CV().apply {
                            v = it.verdi
                            dn = it.navn
                            s = it.kodeverk
                        }
                    }
                    sporsmal = message.subject + ": " + message.body
                    rollerRelatertNotat = listOf(
                        RollerRelatertNotat().apply {
                            roleToPatient = message.responsibleHealthcareProfessional.roleToPatient.toCV()
                            healthcareProfessional = HealthcareProfessional().apply {
                                givenName = message.responsibleHealthcareProfessional.firstName
                                middleName = message.responsibleHealthcareProfessional.middleName
                                familyName = message.responsibleHealthcareProfessional.lastName
                                teleCom.add(
                                    TeleCom().apply {
                                        teleAddress = URL().apply {
                                            v = "tel:${message.responsibleHealthcareProfessional.phoneNumber}"
                                        }
                                    }
                                )
                            }
                        },
                    )
                }
            )
        }

}