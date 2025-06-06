package no.ks.fiks.nhn.edi.v1_0

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
import no.ks.fiks.hdir.TypeOpplysningPasientsamhandling
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
            it.notat.shouldBeEmpty()

            it.foresporsel shouldHaveSize 1
            with(it.foresporsel.single()) {
                typeForesp shouldNot beNull()
                typeForesp.v shouldBe TypeOpplysningPasientsamhandling.ANNEN_HENVENDELSE.verdi
                typeForesp.dn shouldBe TypeOpplysningPasientsamhandling.ANNEN_HENVENDELSE.navn
                typeForesp.s shouldBe TypeOpplysningPasientsamhandling.ANNEN_HENVENDELSE.kodeverk
                typeForesp.ot should beNull()

                sporsmal shouldBe "${message.subject}: ${message.body}"
                formål should beNull()
                begrunnelse should beNull()
                hastegrad should beNull()
                fraDato should beNull()
                tilDato should beNull()
                dokIdForesp should beNull()

                rollerRelatertNotat shouldHaveSize 1
                with(rollerRelatertNotat.single()) {
                    rolleNotat should beNull()
                    person should beNull()

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
            }
        }
    }

})