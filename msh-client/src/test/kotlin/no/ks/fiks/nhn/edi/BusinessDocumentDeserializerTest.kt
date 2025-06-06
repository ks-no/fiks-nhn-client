package no.ks.fiks.nhn.edi

import io.kotest.assertions.asClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.beNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import io.kotest.matchers.types.shouldBeInstanceOf
import no.ks.fiks.hdir.MeldingensFunksjon
import no.ks.fiks.hdir.OrganizationIdType
import no.ks.fiks.hdir.TemaForHelsefagligDialog
import no.ks.fiks.hdir.TypeOpplysningPasientsamhandling
import no.ks.fiks.nhn.msh.Dialogmelding
import no.ks.fiks.nhn.msh.HelsefagligDialog
import no.ks.fiks.nhn.msh.OrganizationReceiverDetails
import no.ks.fiks.nhn.readResourceContent
import no.ks.fiks.nhn.readResourceContentAsString
import java.time.OffsetDateTime

class BusinessDocumentDeserializerTest : StringSpec({

    "Should be able to deserialize Dialogmelding 1.0 Forespørsel og svar" {
        BusinessDocumentDeserializer.deserializeMsgHead(
            readResourceContentAsString("dialogmelding/1.0/foresporsel-og-svar/samsvar-test-message.xml")
        ).asClue {
            it.id shouldBe "6ddb98ed-9e34-4efa-9163-62e4ea0cbf43"
            it.type shouldBe MeldingensFunksjon.DIALOG_FORESPORSEL

            with(it.sender) {
                name shouldBe "NORSK HELSENETT SF"
                id.id shouldBe "112374"
                id.type shouldBe OrganizationIdType.HER_ID

                with(childOrganization) {
                    name shouldBe "Meldingsvalidering"
                    id.id shouldBe "8094866"
                    id.type shouldBe OrganizationIdType.HER_ID
                }
            }

            with(it.receiver) {
                parent.name shouldBe "KS-DIGITALE FELLESTJENESTER AS"
                parent.id.id shouldBe "8142987"
                parent.id.type shouldBe OrganizationIdType.HER_ID

                child.shouldBeInstanceOf<OrganizationReceiverDetails>()
                with(child as OrganizationReceiverDetails) {
                    name shouldBe "Saksbehandling pasientopplysninger"
                    id.id shouldBe "8143060"
                    id.type shouldBe OrganizationIdType.HER_ID
                }
            }

            it.message.shouldBeInstanceOf<Dialogmelding>()
            with(it.message as Dialogmelding) {
                type shouldBe TypeOpplysningPasientsamhandling.ANNEN_HENVENDELSE
                sporsmal shouldBe "Pas går snart tom for cetirizin 10mg tabl og amlodipin 5mg tbl. Ber om fornyelse av resept."
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

    "Should be able to deserialize Dialogmelding 1.1 Helsefaglig dialog" {
        BusinessDocumentDeserializer.deserializeMsgHead(
            readResourceContentAsString("dialogmelding/1.1/helsefaglig-dialog/samsvar-test-message.xml")
        ).asClue {
            it.id shouldBe "615aba81-6a88-41d1-a9f5-90b28cfc8b73"
            it.type shouldBe MeldingensFunksjon.DIALOG_HELSEFAGLIG

            with(it.sender) {
                name shouldBe "NORSK HELSENETT SF"
                id.id shouldBe "112374"
                id.type shouldBe OrganizationIdType.HER_ID

                with(childOrganization) {
                    name shouldBe "Meldingsvalidering"
                    id.id shouldBe "8094866"
                    id.type shouldBe OrganizationIdType.HER_ID
                }
            }

            with(it.receiver) {
                parent.name shouldBe "KS-DIGITALE FELLESTJENESTER AS"
                parent.id.id shouldBe "8142987"
                parent.id.type shouldBe OrganizationIdType.HER_ID

                child.shouldBeInstanceOf<OrganizationReceiverDetails>()
                with(child as OrganizationReceiverDetails) {
                    name shouldBe "Saksbehandling pasientopplysninger"
                    id.id shouldBe "8143060"
                    id.type shouldBe OrganizationIdType.HER_ID
                }
            }

            it.message.shouldBeInstanceOf<HelsefagligDialog>()
            with(it.message as HelsefagligDialog) {
                tema shouldBe TemaForHelsefagligDialog.FORESPORSEL_HELSEOPPLYSNINGER
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

})
