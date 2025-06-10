package no.ks.fiks.nhn.edi.v1_0

import io.kotest.assertions.asClue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.ks.fiks.hdir.FeilmeldingForApplikasjonskvittering
import no.ks.fiks.hdir.OrganizationIdType
import no.ks.fiks.hdir.PersonIdType
import no.ks.fiks.hdir.StatusForMottakAvMelding
import no.ks.fiks.nhn.msh.*
import no.ks.fiks.nhn.readResourceContentAsString

// Example files were found in the standard defined at https://sarepta.helsedir.no/standard/Applikasjonskvittering/1.0
class AppRecDeserializerTest : StringSpec({

    "Test converting receipt with status OK" {
        AppRecDeserializer.toApplicationReceipt(readResourceContentAsString("app-rec/1.0/example-ok.xml")).asClue {
            it.id shouldBe "1d6b0e00-33a1-11de-9255-0002a5d5c51b"
            it.acknowledgedBusinessDocumentId shouldBe "a6967e6a-8c0a-4be4-a647-c921d3086423"
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
                    name = "Rita Lin",
                    id = PersonId(
                        id = "258521",
                        type = PersonIdType.HER_ID,
                    )
                ),
            )
            it.receiver shouldBe Institution(
                name = "Testsykehuset HF",
                id = OrganizationId(
                    id = "123459",
                    type = OrganizationIdType.HER_ID,
                ),
                department = Department(
                    name = "Medisinsk mikrobiologi",
                    id = OrganizationId(
                        id = "91126",
                        type = OrganizationIdType.HER_ID,
                    ),
                ),
                person = null,
            )
        }
    }

    "Test converting receipt with status Avvist" {
        AppRecDeserializer.toApplicationReceipt(readResourceContentAsString("app-rec/1.0/example-avvist.xml")).asClue {
            it.id shouldBe "8d018d60-336b-11de-8bd7-0002a5d5c51b"
            it.acknowledgedBusinessDocumentId shouldBe "a6967e6a-8c0a-4be4-a647-c921d3086423"
            it.status shouldBe StatusForMottakAvMelding.AVVIST
            it.errors shouldHaveSize 1
            it.errors[0].type shouldBe FeilmeldingForApplikasjonskvittering.MOTTAKER_FINNES_IKKE
            it.errors[0].details shouldBe "Legen har sluttet"
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
                name = "Testsykehuset HF",
                id = OrganizationId(
                    id = "123459",
                    type = OrganizationIdType.HER_ID,
                ),
                department = Department(
                    name = "Medisinsk mikrobiologi",
                    id = OrganizationId(
                        id = "91126",
                        type = OrganizationIdType.HER_ID,
                    ),
                ),
                person = null,
            )
        }
    }

    "Test converting receipt with status OK, feil i delmelding" {
        AppRecDeserializer.toApplicationReceipt(readResourceContentAsString("app-rec/1.0/example-ok-feil-i-delmelding.xml")).asClue {
            it.id shouldBe "97499dc0-33a8-11de-a044-0002a5d5c51b"
            it.acknowledgedBusinessDocumentId shouldBe "37de51e0-33a9-11de-a4cb-0002a5d5c51b"
            it.status shouldBe StatusForMottakAvMelding.OK_FEIL_I_DELMELDING
            it.errors shouldHaveSize 2
            it.errors[0].type shouldBe FeilmeldingForApplikasjonskvittering.UKJENT // This example receipt uses kodeverk that is not mentioned in the documentation and is marked as inactive
            it.errors[0].details shouldBe "93495"
            it.errors[1].type shouldBe FeilmeldingForApplikasjonskvittering.UKJENT
            it.errors[1].details shouldBe "93498"
            it.sender shouldBe Institution(
                name = "NAV",
                id = OrganizationId(
                    id = "974760924",
                    type = OrganizationIdType.ENH,
                ),
                department = null,
                person = null,
            )
            it.receiver shouldBe Institution(
                name = "Vassenden legekontor",
                id = OrganizationId(
                    id = "974793539",
                    type = OrganizationIdType.ENH,
                ),
                department = null,
                person = InstitutionPerson(
                    name = "Rita Lin",
                    id = PersonId(
                        id = "9144900",
                        type = PersonIdType.HPR,
                    ),
                ),
            )
        }
    }

    "Test converting receipt with all data" {
        AppRecDeserializer.toApplicationReceipt(readResourceContentAsString("app-rec/1.0/all-data.xml")).asClue {
            it.id shouldBe "a761e9b9-3495-4f5a-a964-c13d544d2ceb"
            it.acknowledgedBusinessDocumentId shouldBe "36bbe907-5c2a-4ca9-be92-c7e9c5f2e4d6"
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
            AppRecDeserializer.toApplicationReceipt(readResourceContentAsString("app-rec/1.0/unknown-status.xml"))
        }.asClue { it.message shouldBe "Unknown app rec status: 73, Some status" }
    }

})
