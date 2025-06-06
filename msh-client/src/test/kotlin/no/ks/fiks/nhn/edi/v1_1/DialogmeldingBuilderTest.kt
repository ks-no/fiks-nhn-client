package no.ks.fiks.nhn.edi.v1_1

import io.kotest.assertions.asClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.beNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import no.ks.fiks.hdir.Helsepersonell
import no.ks.fiks.hdir.HelsepersonellsFunksjoner
import no.ks.fiks.hdir.TemaForHelsefagligDialog
import no.ks.fiks.nhn.msh.OutgoingMessage
import no.ks.fiks.nhn.msh.HealthcareProfessional
import no.ks.fiks.nhn.msh.RecipientContact
import java.util.*

class DialogmeldingBuilderTest : StringSpec({

    "Test mapping from BusinessDocumentMessage to Dialogmelding" {
        val message = OutgoingMessage(
            subject = UUID.randomUUID().toString(),
            body = UUID.randomUUID().toString() + "\n" + UUID.randomUUID().toString(),
            responsibleHealthcareProfessional = HealthcareProfessional(
                firstName = UUID.randomUUID().toString(),
                middleName = UUID.randomUUID().toString(),
                lastName = UUID.randomUUID().toString(),
                phoneNumber = UUID.randomUUID().toString(),
                roleToPatient = HelsepersonellsFunksjoner.entries.random(),
            ),
            recipientContact = RecipientContact(
                type = Helsepersonell.entries.random(),
            )
        )

        DialogmeldingBuilder.buildDialogmelding(message).asClue {
            it.sakstypeKodet should beNull()
            it.sakstype should beNull()
            it.foresporsel.shouldBeEmpty()

            it.notat shouldHaveSize 1
            with(it.notat.single()) {
                temaKodet shouldNot beNull()
                temaKodet.v shouldBe TemaForHelsefagligDialog.HENVENDELSE_OM_PASIENT.verdi
                temaKodet.dn shouldBe TemaForHelsefagligDialog.HENVENDELSE_OM_PASIENT.navn
                temaKodet.s shouldBe TemaForHelsefagligDialog.HENVENDELSE_OM_PASIENT.kodeverk
                temaKodet.ot should beNull()

                tema shouldBe message.subject
                tekstNotatInnhold shouldBe message.body

                rollerRelatertNotat shouldHaveSize 2
                with(rollerRelatertNotat[0]) {
                    rolleNotat should beNull()
                    person should beNull()
                    tilknyttetEnhet should beNull()

                    roleToPatient shouldNot beNull()
                    roleToPatient.v shouldBe message.responsibleHealthcareProfessional.roleToPatient.verdi
                    roleToPatient.dn shouldBe message.responsibleHealthcareProfessional.roleToPatient.navn
                    roleToPatient.s shouldBe message.responsibleHealthcareProfessional.roleToPatient.kodeverk
                    roleToPatient.ot should beNull()

                    healthcareProfessional shouldNot beNull()
                    healthcareProfessional.givenName shouldBe message.responsibleHealthcareProfessional.firstName
                    healthcareProfessional.middleName shouldBe message.responsibleHealthcareProfessional.middleName
                    healthcareProfessional.familyName shouldBe message.responsibleHealthcareProfessional.lastName

                    healthcareProfessional.teleCom shouldHaveSize 1
                    with(healthcareProfessional.teleCom.single()) {
                        typeTelecom should beNull()
                        teleAddress shouldNot beNull()
                        teleAddress.v shouldBe "tel:${message.responsibleHealthcareProfessional.phoneNumber}"
                    }

                    healthcareProfessional.typeHealthcareProfessional should beNull()
                    healthcareProfessional.roleToPatient should beNull()
                    healthcareProfessional.dateOfBirth should beNull()
                    healthcareProfessional.sex should beNull()
                    healthcareProfessional.nationality should beNull()
                    healthcareProfessional.ident.shouldBeEmpty()
                    healthcareProfessional.address should beNull()
                }
                with(rollerRelatertNotat[1]) {
                    rolleNotat should beNull()
                    person should beNull()
                    tilknyttetEnhet should beNull()

                    roleToPatient shouldNot beNull()
                    roleToPatient.v shouldBe HelsepersonellsFunksjoner.KONTAKT_HOS_MOTTAKER.verdi
                    roleToPatient.dn shouldBe HelsepersonellsFunksjoner.KONTAKT_HOS_MOTTAKER.navn
                    roleToPatient.s shouldBe HelsepersonellsFunksjoner.KONTAKT_HOS_MOTTAKER.kodeverk
                    roleToPatient.ot should beNull()

                    healthcareProfessional shouldNot beNull()
                    healthcareProfessional.typeHealthcareProfessional.v shouldBe message.recipientContact.type.verdi
                    healthcareProfessional.typeHealthcareProfessional.dn shouldBe message.recipientContact.type.navn
                    healthcareProfessional.givenName should beNull()
                    healthcareProfessional.middleName should beNull()
                    healthcareProfessional.familyName should beNull()
                    healthcareProfessional.teleCom.shouldBeEmpty()
                    healthcareProfessional.roleToPatient should beNull()
                    healthcareProfessional.dateOfBirth should beNull()
                    healthcareProfessional.sex should beNull()
                    healthcareProfessional.nationality should beNull()
                    healthcareProfessional.ident.shouldBeEmpty()
                    healthcareProfessional.address should beNull()
                }

                merknad should beNull()
                dokIdNotat should beNull()
                datoNotat should beNull()
                foresporsel should beNull()
            }
        }
    }

})