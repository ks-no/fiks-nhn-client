package no.ks.fiks.nhn.msh

import no.kith.xmlstds.CV
import no.kith.xmlstds.URL
import no.kith.xmlstds.dialog._2006_10_11.Dialogmelding
import no.kith.xmlstds.dialog._2006_10_11.Foresporsel
import no.kith.xmlstds.dialog._2006_10_11.HealthcareProfessional
import no.kith.xmlstds.dialog._2006_10_11.RollerRelatertNotat
import no.kith.xmlstds.felleskomponent1.TeleCom
import no.ks.fiks.hdir.HelsepersonellsFunksjoner
import no.ks.fiks.hdir.TypeOpplysningPasientsamhandling

object DialogmeldingBuilder {

    fun buildDialogmelding() = Dialogmelding()
        .apply {
            foresporsel = listOf(
                Foresporsel().apply {
                    typeForesp = TypeOpplysningPasientsamhandling.ANNEN_HENVENDELSE.let { // TODO: Fant bare noe tilfeldig, vet ikke hva vi skal bruke her
                        CV().apply {
                            v = it.verdi
                            dn = it.navn
                            s = it.kodeverk
                        }
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
            XmlContext.validate(it)
        }

}