package no.ks.fiks.nhn.edi.v1_1

import no.kith.xmlstds.CS
import no.kith.xmlstds.CV
import no.kith.xmlstds.URL
import no.kith.xmlstds.dialog._2013_01_23.Dialogmelding
import no.kith.xmlstds.dialog._2013_01_23.HealthcareProfessional
import no.kith.xmlstds.dialog._2013_01_23.Notat
import no.kith.xmlstds.dialog._2013_01_23.RollerRelatertNotat
import no.kith.xmlstds.felleskomponent1.TeleCom
import no.ks.fiks.hdir.HelsepersonellsFunksjoner
import no.ks.fiks.hdir.TemaForHelsefagligDialog
import no.ks.fiks.nhn.edi.toCV
import no.ks.fiks.nhn.msh.OutgoingMessage

object DialogmeldingBuilder {

    fun buildDialogmelding(message: OutgoingMessage) = Dialogmelding()
        .apply {
            notat = listOf(
                Notat().apply {
                    temaKodet = TemaForHelsefagligDialog.HENVENDELSE_OM_PASIENT.let {
                        CV().apply {
                            v = it.verdi
                            dn = it.navn
                            s = it.kodeverk
                        }
                    }
                    tema = message.subject
                    tekstNotatInnhold = message.body
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
                        // Ved opprettelse av Helsefaglig dialog uten referanse til tidligere melding, skal rollen
                        // "Kontakt hos mottaker" oppgis med profesjonsgruppe og ev. navn på helsepersonell.
                        // Opplysningene oppgis i klassen Helsepersonell.
                        // Kravet gjelder kun ved kodeverdi 6 "Henvendelse om pasient" i elementet tema kodet.
                        RollerRelatertNotat().apply {
                            roleToPatient = HelsepersonellsFunksjoner.KONTAKT_HOS_MOTTAKER.toCV()
                            healthcareProfessional = HealthcareProfessional().apply {
                                typeHealthcareProfessional = message.recipientContact.type.let {
                                    CS().apply {
                                        dn = it.navn
                                        v = it.verdi
                                    }
                                }
                            }
                        }
                    )
                }
            )
        }

}