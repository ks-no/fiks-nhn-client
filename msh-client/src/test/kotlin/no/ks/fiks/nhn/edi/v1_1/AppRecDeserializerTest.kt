package no.ks.fiks.nhn.edi.v1_1

import io.kotest.assertions.asClue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.beNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.ks.fiks.hdir.FeilmeldingForApplikasjonskvittering
import no.ks.fiks.hdir.OrganizationIdType
import no.ks.fiks.hdir.PersonIdType
import no.ks.fiks.hdir.StatusForMottakAvMelding
import no.ks.fiks.nhn.msh.*
import no.ks.fiks.nhn.readResourceContentAsString

// Example files were found in the standard defined at https://sarepta.helsedir.no/standard/Applikasjonskvittering/1.1
class AppRecDeserializerTest : StringSpec({

    "Test converting receipt with status OK" {
        AppRecDeserializer.toApplicationReceipt(readResourceContentAsString("app-rec/1.1/example-ok.xml")).asClue {
            it.id shouldBe "5e4f20c0-c41b-11e0-962b-0800200c9a66"
            it.acknowledgedBusinessDocumentId shouldBe "4c793d90-c41b-11e0-962b-0800200c9a66"
            it.status shouldBe StatusForMottakAvMelding.OK
            it.errors.shouldBeEmpty()
            it.sender shouldBe Institution(
                name = "Kattskinnet legesenter",
                id = OrganizationId(
                    id = "56704",
                    type = OrganizationIdType.HER_ID,
                ),
                department = null,
                person = InstitutionPerson(
                    name = "August September",
                    id = PersonId(
                        id = "369767",
                        type = PersonIdType.HER_ID,
                    )
                ),
            )
            it.receiver shouldBe Institution(
                name = "SYKEHUSET I VESTFOLD HF",
                id = OrganizationId(
                    id = "69",
                    type = OrganizationIdType.HER_ID,
                ),
                department = Department(
                    name = "Kirurgi",
                    id = OrganizationId(
                        id = "89583",
                        type = OrganizationIdType.HER_ID,
                    ),
                ),
                person = null,
            )
        }
    }

    "Test converting receipt with status Avvist" {
        AppRecDeserializer.toApplicationReceipt(readResourceContentAsString("app-rec/1.1/example-avvist.xml")).asClue {
            it.id shouldBe "8d018d60-336b-11de-8bd7-0002a5d5c51b"
            it.acknowledgedBusinessDocumentId shouldBe "e0e21a90-5ec8-11e1-b86c-0800200c9a66"
            it.status shouldBe StatusForMottakAvMelding.AVVIST
            it.errors shouldHaveSize 1
            it.errors[0].type shouldBe FeilmeldingForApplikasjonskvittering.MOTTAKER_FINNES_IKKE
            it.errors[0].details should beNull()
            it.sender shouldBe Institution(
                name = "Kattskinnet legesenter",
                id = OrganizationId(
                    id = "56704",
                    type = OrganizationIdType.HER_ID,
                ),
                department = null,
                person = InstitutionPerson(
                    name = "Rita Lin",
                    id = PersonId(
                        id = "258521",
                        type = PersonIdType.HER_ID,
                    )
                ),
            )
            it.receiver shouldBe Institution(
                name = "SYKEHUSET I VESTFOLD HF",
                id = OrganizationId(
                    id = "69",
                    type = OrganizationIdType.HER_ID,
                ),
                department = Department(
                    name = "Kirurgi",
                    id = OrganizationId(
                        id = "89583",
                        type = OrganizationIdType.HER_ID,
                    ),
                ),
                person = null,
            )
        }
    }

    "Test converting receipt with all data" {
        AppRecDeserializer.toApplicationReceipt(readResourceContentAsString("app-rec/1.1/all-data.xml")).asClue {
            it.id shouldBe "f6d24037-a43f-4509-8dfa-88b578554858"
            it.acknowledgedBusinessDocumentId shouldBe "5d79bd68-a58c-4599-a4e7-9db28ea20099"
            it.status shouldBe StatusForMottakAvMelding.OK
            it.errors.shouldBeEmpty()
            it.sender shouldBe Institution(
                name = "KS-DIGITALE FELLESTJENESTER AS",
                id = OrganizationId(
                    id = "8142987",
                    type = OrganizationIdType.HER_ID,
                ),
                department = Department(
                    name = "Saksbehandling pasientopplysninger",
                    id = OrganizationId(
                        id = "8143060",
                        type = OrganizationIdType.HER_ID,
                    ),
                ),
                person = InstitutionPerson(
                    name = "Test Testperson",
                    id = PersonId(
                        id = "123456",
                        type = PersonIdType.HER_ID,
                    )
                ),
            )
            it.receiver shouldBe Institution(
                name = "Test Testinstitusjon",
                id = OrganizationId(
                    id = "998877",
                    type = OrganizationIdType.HER_ID,
                ),
                department = Department(
                    name = "Testavdelingen",
                    id = OrganizationId(
                        id = "555111",
                        type = OrganizationIdType.HER_ID,
                    ),
                ),
                person = InstitutionPerson(
                    name = "Person Testesen",
                    id = PersonId(
                        id = "9512",
                        type = PersonIdType.HER_ID,
                    )
                ),
            )
        }
    }

    "Unknown status should cause an exception to be thrown" {
        shouldThrow<IllegalArgumentException> {
            AppRecDeserializer.toApplicationReceipt(readResourceContentAsString("app-rec/1.1/unknown-status.xml"))
        }.asClue { it.message shouldBe "Unknown app rec status: 123, Some status" }
    }

})
