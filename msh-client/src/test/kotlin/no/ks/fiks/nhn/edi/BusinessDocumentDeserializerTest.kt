package no.ks.fiks.nhn.edi

import io.kotest.assertions.asClue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.beNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import io.kotest.matchers.types.shouldBeInstanceOf
import no.ks.fiks.hdir.*
import no.ks.fiks.nhn.msh.*
import no.ks.fiks.nhn.readResourceContent
import no.ks.fiks.nhn.readResourceContentAsString
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

class BusinessDocumentDeserializerTest : StringSpec({

    "Should be able to deserialize Dialogmelding 1.0 Forespørsel" {
        BusinessDocumentDeserializer.deserializeMsgHead(
            readResourceContentAsString("dialogmelding/1.0/foresporsel-og-svar/dialog-foresporsel-samsvar-test.xml")
        ).asClue {
            it.id shouldBe "6ddb98ed-9e34-4efa-9163-62e4ea0cbf43"
            it.date shouldBe OffsetDateTime.of(2025, 5, 13, 11, 51, 1, 0, ZoneOffset.ofHours(2))
            it.type shouldBe MeldingensFunksjon.DIALOG_FORESPORSEL

            with(it.sender) {
                parent.name shouldBe "NORSK HELSENETT SF"
                parent.ids.single().id shouldBe "112374"
                parent.ids.single().type shouldBe OrganizationIdType.HER_ID

                child.shouldBeInstanceOf<OrganizationCommunicationParty>()
                with(child) {
                    name shouldBe "Meldingsvalidering"
                    ids.single().id shouldBe "8094866"
                    ids.single().type shouldBe OrganizationIdType.HER_ID
                }
            }

            with(it.receiver) {
                parent.name shouldBe "KS-DIGITALE FELLESTJENESTER AS"
                parent.ids.single().id shouldBe "8142987"
                parent.ids.single().type shouldBe OrganizationIdType.HER_ID

                child.shouldBeInstanceOf<OrganizationCommunicationParty>()
                with(child) {
                    name shouldBe "Saksbehandling pasientopplysninger"
                    ids.single().id shouldBe "8143060"
                    ids.single().type shouldBe OrganizationIdType.HER_ID
                }

                with(patient) {
                    fnr shouldBe "15720255178"
                    firstName shouldBe "BRUN"
                    middleName should beNull()
                    lastName shouldBe "LENESTOL"
                }
            }

            it.message shouldNot beNull()
            with(it.message!!) {
                foresporsel shouldNot beNull()
                with(foresporsel!!) {
                    type shouldBe TypeOpplysningPasientsamhandlingPleieOgOmsorg.ANNEN_HENVENDELSE
                    sporsmal shouldBe "Pas går snart tom for cetirizin 10mg tabl og amlodipin 5mg tbl. Ber om fornyelse av resept."
                }
                notat should beNull()
            }

            it.vedlegg shouldNot beNull()
            with(it.vedlegg!!) {
                date shouldBe OffsetDateTime.parse("2025-05-13T11:51:01.5833859Z")
                description shouldBe "small2.pdf"
                mimeType shouldBe "application/pdf"
                data shouldNot beNull()
                data!!.readAllBytes() shouldBe readResourceContent("small.pdf")
            }
        }
    }

    "Should be able to deserialize Dialogmelding 1.0 Svar" {
        BusinessDocumentDeserializer.deserializeMsgHead(
            readResourceContentAsString("dialogmelding/1.0/foresporsel-og-svar/dialog-svar-webmed-test.xml")
        ).asClue { doc ->
            doc.id shouldBe "a748bb20-4e0f-4922-9b06-ec2c101eb9c1"
            doc.date shouldBe OffsetDateTime.of(2025, 6, 10, 10, 55, 20, 0, ZoneOffset.ofHours(3))
            doc.type shouldBe MeldingensFunksjon.DIALOG_SVAR

            with(doc.sender) {
                parent.name shouldBe "WebMed Feature PPS"
                parent.ids shouldHaveSize 2
                parent.ids shouldHaveSingleElement { it.id == "8142952" && it.type == OrganizationIdType.HER_ID }
                parent.ids shouldHaveSingleElement { it.id == "999988939" && it.type == OrganizationIdType.ENH }

                child.shouldBeInstanceOf<PersonCommunicationParty>()
                with(child) {
                    firstName shouldBe "Grønn"
                    middleName should beNull()
                    lastName shouldBe "Vits"

                    ids shouldHaveSize 2
                    ids[0].id shouldBe "565501872"
                    ids[0].type shouldBe PersonIdType.HPR
                    ids[1].id shouldBe "8143025"
                    ids[1].type shouldBe PersonIdType.HER_ID
                }
            }

            with(doc.receiver) {
                parent.name shouldBe "KS-DIGITALE FELLESTJENESTER AS"
                parent.ids.single().id shouldBe "8142987"
                parent.ids.single().type shouldBe OrganizationIdType.HER_ID

                child.shouldBeInstanceOf<OrganizationCommunicationParty>()
                with(child) {
                    name shouldBe "SvarUt meldingsformidler"
                    ids.single().id shouldBe "8143060"
                    ids.single().type shouldBe OrganizationIdType.HER_ID
                }

                with(patient) {
                    fnr shouldBe "15720255178"
                    firstName shouldBe "BRUN"
                    middleName should beNull()
                    lastName shouldBe "LENESTOL"
                }
            }

            doc.message shouldNot beNull()
            with(doc.message!!) {
                foresporsel should beNull()
                notat shouldNot beNull()
                with(notat!!) {
                    tema shouldBe TypeOpplysningPasientsamhandlingLege.ANNEN_HENVENDELSE
                    temaBeskrivelse should beNull()
                    innhold shouldBe """Lege svarer med notat.
                            Innkommende melding vises OK. """
                    dato shouldBe LocalDate.of(2025, 6, 10)
                }
            }

            doc.vedlegg should beNull()
        }
    }

    "Should be able to deserialize Dialogmelding 1.1 Helsefaglig dialog" {
        BusinessDocumentDeserializer.deserializeMsgHead(
            readResourceContentAsString("dialogmelding/1.1/helsefaglig-dialog/samsvar-test-message.xml")
        ).asClue {
            it.id shouldBe "615aba81-6a88-41d1-a9f5-90b28cfc8b73"
            it.type shouldBe MeldingensFunksjon.DIALOG_HELSEFAGLIG

            with(it.sender) {
                parent.name shouldBe "NORSK HELSENETT SF"
                parent.ids.single().id shouldBe "112374"
                parent.ids.single().type shouldBe OrganizationIdType.HER_ID

                child.shouldBeInstanceOf<OrganizationCommunicationParty>()
                with(child) {
                    name shouldBe "Meldingsvalidering"
                    ids.single().id shouldBe "8094866"
                    ids.single().type shouldBe OrganizationIdType.HER_ID
                }
            }

            with(it.receiver) {
                parent.name shouldBe "KS-DIGITALE FELLESTJENESTER AS"
                parent.ids.single().id shouldBe "8142987"
                parent.ids.single().type shouldBe OrganizationIdType.HER_ID

                child.shouldBeInstanceOf<OrganizationCommunicationParty>()
                with(child) {
                    name shouldBe "Saksbehandling pasientopplysninger"
                    ids.single().id shouldBe "8143060"
                    ids.single().type shouldBe OrganizationIdType.HER_ID
                }
            }

            it.message shouldNot beNull()
            with(it.message!!) {
                foresporsel should beNull()
                notat shouldNot beNull()
                with(notat!!) {
                    tema shouldBe TemaForHelsefagligDialog.FORESPORSEL_HELSEOPPLYSNINGER
                    temaBeskrivelse shouldBe "EKG tatt i dag"
                    innhold shouldBe "Pasienten er henvist til kardiologisk poliklinikk for utredning med spørsmål om cardial årsak og mulig angina pectoris. Ettersender EKG tatt i dag uten funn. Se vedlegg."
                    dato should beNull()
                }
            }

            it.vedlegg shouldNot beNull()
            with(it.vedlegg!!) {
                date shouldBe OffsetDateTime.parse("2025-05-13T11:56:21.7607390Z")
                description shouldBe "small2.pdf"
                mimeType shouldBe "application/pdf"
                data shouldNot beNull()
                data!!.readAllBytes() shouldBe readResourceContent("small.pdf")
            }
        }
    }

    "Should be able to deserialize AppRec 1.0" {
        BusinessDocumentDeserializer.deserializeAppRec(
            readResourceContentAsString("app-rec/1.0/all-data.xml")
        ).asClue {
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

    "Should be able to deserialize AppRec 1.1" {
        BusinessDocumentDeserializer.deserializeAppRec(
            readResourceContentAsString("app-rec/1.1/all-data.xml")
        ).asClue {
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

    "AppRec missing version should cause exception to be thrown" {
        shouldThrow< IllegalArgumentException> {
            BusinessDocumentDeserializer.deserializeAppRec(readResourceContentAsString("app-rec/missing-migversion.xml"))
        }.asClue { it.message shouldBe "Could not find MIGversion in XML" }
    }

    "AppRec with unknown version should cause exception to be thrown" {
        shouldThrow< IllegalArgumentException> {
            BusinessDocumentDeserializer.deserializeAppRec(readResourceContentAsString("app-rec/unknown-migversion.xml"))
        }.asClue { it.message shouldBe "Unknown version for AppRec: 123" }
    }

})
