package no.ks.fiks.nhn

import io.kotest.assertions.asClue
import io.kotest.matchers.date.shouldBeBetween
import io.kotest.matchers.shouldBe
import no.ks.fiks.hdir.*
import no.ks.fiks.nhn.msh.DialogmeldingVersion
import no.ks.fiks.nhn.msh.OrganizationReceiverDetails
import no.ks.fiks.nhn.msh.OutgoingBusinessDocument
import no.ks.fiks.nhn.msh.PersonReceiverDetails
import java.io.ByteArrayInputStream
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathFactory

fun String.validateXmlAgainst(startTime: OffsetDateTime, document: OutgoingBusinessDocument, vedleggBytes: ByteArray) {
    asClue { xml ->
        val xmlDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(ByteArrayInputStream(xml.toByteArray()))
        val xPath = XPathFactory.newInstance().newXPath()

        when (document.version) {
            DialogmeldingVersion.V1_0 -> {
                xPath.evaluate("/MsgHead/MsgInfo/Type/@V", xmlDoc) shouldBe MeldingensFunksjon.DIALOG_FORESPORSEL.verdi
                xPath.evaluate("/MsgHead/MsgInfo/Type/@DN", xmlDoc) shouldBe MeldingensFunksjon.DIALOG_FORESPORSEL.navn
            }
            DialogmeldingVersion.V1_1 -> {
                xPath.evaluate("/MsgHead/MsgInfo/Type/@V", xmlDoc) shouldBe MeldingensFunksjon.DIALOG_HELSEFAGLIG.verdi
                xPath.evaluate("/MsgHead/MsgInfo/Type/@DN", xmlDoc) shouldBe MeldingensFunksjon.DIALOG_HELSEFAGLIG.navn
            }
        }
        xPath.evaluate("/MsgHead/MsgInfo/MIGversion", xmlDoc) shouldBe "v1.2 2006-05-24"
        OffsetDateTime.parse(xPath.evaluate("/MsgHead/MsgInfo/GenDate", xmlDoc)).shouldBeBetween(startTime.minusSeconds(1), OffsetDateTime.now())

        xPath.evaluate("/MsgHead/MsgInfo/Sender/Organisation/OrganisationName", xmlDoc) shouldBe document.sender.name
        xPath.evaluate("/MsgHead/MsgInfo/Sender/Organisation/Ident/Id", xmlDoc) shouldBe document.sender.ids.single().id
        xPath.evaluate("/MsgHead/MsgInfo/Sender/Organisation/Ident/TypeId/@V", xmlDoc) shouldBe document.sender.ids.single().type.verdi
        xPath.evaluate("/MsgHead/MsgInfo/Sender/Organisation/Ident/TypeId/@S", xmlDoc) shouldBe document.sender.ids.single().type.kodeverk
        xPath.evaluate("/MsgHead/MsgInfo/Sender/Organisation/Ident/TypeId/@DN", xmlDoc) shouldBe document.sender.ids.single().type.navn
        xPath.evaluate("/MsgHead/MsgInfo/Sender/Organisation/Organisation/OrganisationName", xmlDoc) shouldBe document.sender.childOrganization?.name
        xPath.evaluate("/MsgHead/MsgInfo/Sender/Organisation/Organisation/Ident/Id", xmlDoc) shouldBe document.sender.childOrganization?.ids?.single()?.id
        xPath.evaluate("/MsgHead/MsgInfo/Sender/Organisation/Organisation/Ident/TypeId/@V", xmlDoc) shouldBe document.sender.childOrganization?.ids?.single()?.type?.verdi
        xPath.evaluate("/MsgHead/MsgInfo/Sender/Organisation/Organisation/Ident/TypeId/@S", xmlDoc) shouldBe document.sender.childOrganization?.ids?.single()?.type?.kodeverk
        xPath.evaluate("/MsgHead/MsgInfo/Sender/Organisation/Organisation/Ident/TypeId/@DN", xmlDoc) shouldBe document.sender.childOrganization?.ids?.single()?.type?.navn

        xPath.evaluate("/MsgHead/MsgInfo/Receiver/Organisation/OrganisationName", xmlDoc) shouldBe document.receiver.parent.name
        xPath.evaluate("/MsgHead/MsgInfo/Receiver/Organisation/Ident/Id", xmlDoc) shouldBe document.receiver.parent.ids.single().id
        xPath.evaluate("/MsgHead/MsgInfo/Receiver/Organisation/Ident/TypeId/@V", xmlDoc) shouldBe document.receiver.parent.ids.single().type.verdi
        xPath.evaluate("/MsgHead/MsgInfo/Receiver/Organisation/Ident/TypeId/@S", xmlDoc) shouldBe document.receiver.parent.ids.single().type.kodeverk
        xPath.evaluate("/MsgHead/MsgInfo/Receiver/Organisation/Ident/TypeId/@DN", xmlDoc) shouldBe document.receiver.parent.ids.single().type.navn

        when (val receiverChild = document.receiver.child) {
            is PersonReceiverDetails -> {
                xPath.evaluate("/MsgHead/MsgInfo/Receiver/Organisation/HealthcareProfessional/GivenName", xmlDoc) shouldBe receiverChild.firstName
                xPath.evaluate("/MsgHead/MsgInfo/Receiver/Organisation/HealthcareProfessional/MiddleName", xmlDoc) shouldBe receiverChild.middleName
                xPath.evaluate("/MsgHead/MsgInfo/Receiver/Organisation/HealthcareProfessional/FamilyName", xmlDoc) shouldBe receiverChild.lastName

                xPath.evaluate("/MsgHead/MsgInfo/Receiver/Organisation/HealthcareProfessional/Ident/Id", xmlDoc) shouldBe document.receiver.child.ids.single().id
                xPath.evaluate("/MsgHead/MsgInfo/Receiver/Organisation/HealthcareProfessional/Ident/TypeId/@V", xmlDoc) shouldBe document.receiver.child.ids.single().type.verdi
                xPath.evaluate("/MsgHead/MsgInfo/Receiver/Organisation/HealthcareProfessional/Ident/TypeId/@S", xmlDoc) shouldBe document.receiver.child.ids.single().type.kodeverk
                xPath.evaluate("/MsgHead/MsgInfo/Receiver/Organisation/HealthcareProfessional/Ident/TypeId/@DN", xmlDoc) shouldBe document.receiver.child.ids.single().type.navn
            }
            is OrganizationReceiverDetails -> {
                xPath.evaluate("/MsgHead/MsgInfo/Receiver/Organisation/Organisation/OrganisationName", xmlDoc) shouldBe receiverChild.name

                xPath.evaluate("/MsgHead/MsgInfo/Receiver/Organisation/Organisation/Ident/Id", xmlDoc) shouldBe document.receiver.child.ids.single().id
                xPath.evaluate("/MsgHead/MsgInfo/Receiver/Organisation/Organisation/Ident/TypeId/@V", xmlDoc) shouldBe document.receiver.child.ids.single().type.verdi
                xPath.evaluate("/MsgHead/MsgInfo/Receiver/Organisation/Organisation/Ident/TypeId/@S", xmlDoc) shouldBe document.receiver.child.ids.single().type.kodeverk
                xPath.evaluate("/MsgHead/MsgInfo/Receiver/Organisation/Organisation/Ident/TypeId/@DN", xmlDoc) shouldBe document.receiver.child.ids.single().type.navn
            }
        }

        xPath.evaluate("/MsgHead/MsgInfo/Patient/GivenName", xmlDoc) shouldBe document.receiver.patient.firstName
        xPath.evaluate("/MsgHead/MsgInfo/Patient/MiddleName", xmlDoc) shouldBe document.receiver.patient.middleName
        xPath.evaluate("/MsgHead/MsgInfo/Patient/FamilyName", xmlDoc) shouldBe document.receiver.patient.lastName
        xPath.evaluate("/MsgHead/MsgInfo/Patient/Ident/Id", xmlDoc) shouldBe document.receiver.patient.fnr
        xPath.evaluate("/MsgHead/MsgInfo/Patient/Ident/TypeId/@V", xmlDoc) shouldBe PersonIdType.FNR.verdi
        xPath.evaluate("/MsgHead/MsgInfo/Patient/Ident/TypeId/@S", xmlDoc) shouldBe PersonIdType.FNR.kodeverk
        xPath.evaluate("/MsgHead/MsgInfo/Patient/Ident/TypeId/@DN", xmlDoc) shouldBe PersonIdType.FNR.navn

        xPath.evaluate("/MsgHead/Document[1]/RefDoc/MsgType/@V", xmlDoc) shouldBe TypeDokumentreferanse.XML.verdi
        xPath.evaluate("/MsgHead/Document[1]/RefDoc/MsgType/@DN", xmlDoc) shouldBe TypeDokumentreferanse.XML.navn

        when (document.version) {
            DialogmeldingVersion.V1_0 -> {
                xPath.evaluate("/MsgHead/Document[1]/RefDoc/Content/Dialogmelding/Foresporsel/TypeForesp/@V", xmlDoc) shouldBe TypeOpplysningPasientsamhandlingPleieOgOmsorg.ANNEN_HENVENDELSE.verdi
                xPath.evaluate("/MsgHead/Document[1]/RefDoc/Content/Dialogmelding/Foresporsel/TypeForesp/@S", xmlDoc) shouldBe TypeOpplysningPasientsamhandlingPleieOgOmsorg.ANNEN_HENVENDELSE.kodeverk
                xPath.evaluate("/MsgHead/Document[1]/RefDoc/Content/Dialogmelding/Foresporsel/TypeForesp/@DN", xmlDoc) shouldBe TypeOpplysningPasientsamhandlingPleieOgOmsorg.ANNEN_HENVENDELSE.navn
                xPath.evaluate("/MsgHead/Document[1]/RefDoc/Content/Dialogmelding/Foresporsel/Sporsmal", xmlDoc) shouldBe "${document.message.subject}: ${document.message.body}"
                xPath.evaluate("/MsgHead/Document[1]/RefDoc/Content/Dialogmelding/Foresporsel/RollerRelatertNotat/RoleToPatient/@V", xmlDoc) shouldBe document.message.responsibleHealthcareProfessional.roleToPatient.verdi
                xPath.evaluate("/MsgHead/Document[1]/RefDoc/Content/Dialogmelding/Foresporsel/RollerRelatertNotat/RoleToPatient/@S", xmlDoc) shouldBe document.message.responsibleHealthcareProfessional.roleToPatient.kodeverk
                xPath.evaluate("/MsgHead/Document[1]/RefDoc/Content/Dialogmelding/Foresporsel/RollerRelatertNotat/RoleToPatient/@DN", xmlDoc) shouldBe document.message.responsibleHealthcareProfessional.roleToPatient.navn
                xPath.evaluate("/MsgHead/Document[1]/RefDoc/Content/Dialogmelding/Foresporsel/RollerRelatertNotat/HealthcareProfessional/GivenName", xmlDoc) shouldBe document.message.responsibleHealthcareProfessional.firstName
                xPath.evaluate("/MsgHead/Document[1]/RefDoc/Content/Dialogmelding/Foresporsel/RollerRelatertNotat/HealthcareProfessional/MiddleName", xmlDoc) shouldBe document.message.responsibleHealthcareProfessional.middleName
                xPath.evaluate("/MsgHead/Document[1]/RefDoc/Content/Dialogmelding/Foresporsel/RollerRelatertNotat/HealthcareProfessional/FamilyName", xmlDoc) shouldBe document.message.responsibleHealthcareProfessional.lastName
                xPath.evaluate("/MsgHead/Document[1]/RefDoc/Content/Dialogmelding/Foresporsel/RollerRelatertNotat/HealthcareProfessional/TeleCom/TeleAddress/@V", xmlDoc) shouldBe "tel:${document.message.responsibleHealthcareProfessional.phoneNumber}"
            }
            DialogmeldingVersion.V1_1 -> {
                xPath.evaluate("/MsgHead/Document[1]/RefDoc/Content/Dialogmelding/Notat/TemaKodet/@V", xmlDoc) shouldBe TemaForHelsefagligDialog.HENVENDELSE_OM_PASIENT.verdi
                xPath.evaluate("/MsgHead/Document[1]/RefDoc/Content/Dialogmelding/Notat/TemaKodet/@S", xmlDoc) shouldBe TemaForHelsefagligDialog.HENVENDELSE_OM_PASIENT.kodeverk
                xPath.evaluate("/MsgHead/Document[1]/RefDoc/Content/Dialogmelding/Notat/TemaKodet/@DN", xmlDoc) shouldBe TemaForHelsefagligDialog.HENVENDELSE_OM_PASIENT.navn
                xPath.evaluate("/MsgHead/Document[1]/RefDoc/Content/Dialogmelding/Notat/Tema", xmlDoc) shouldBe document.message.subject
                xPath.evaluate("/MsgHead/Document[1]/RefDoc/Content/Dialogmelding/Notat/TekstNotatInnhold", xmlDoc) shouldBe document.message.body
                xPath.evaluate("/MsgHead/Document[1]/RefDoc/Content/Dialogmelding/Notat/RollerRelatertNotat[1]/RoleToPatient/@V", xmlDoc) shouldBe document.message.responsibleHealthcareProfessional.roleToPatient.verdi
                xPath.evaluate("/MsgHead/Document[1]/RefDoc/Content/Dialogmelding/Notat/RollerRelatertNotat[1]/RoleToPatient/@S", xmlDoc) shouldBe document.message.responsibleHealthcareProfessional.roleToPatient.kodeverk
                xPath.evaluate("/MsgHead/Document[1]/RefDoc/Content/Dialogmelding/Notat/RollerRelatertNotat[1]/RoleToPatient/@DN", xmlDoc) shouldBe document.message.responsibleHealthcareProfessional.roleToPatient.navn
                xPath.evaluate("/MsgHead/Document[1]/RefDoc/Content/Dialogmelding/Notat/RollerRelatertNotat[1]/HealthcareProfessional/GivenName", xmlDoc) shouldBe document.message.responsibleHealthcareProfessional.firstName
                xPath.evaluate("/MsgHead/Document[1]/RefDoc/Content/Dialogmelding/Notat/RollerRelatertNotat[1]/HealthcareProfessional/MiddleName", xmlDoc) shouldBe document.message.responsibleHealthcareProfessional.middleName
                xPath.evaluate("/MsgHead/Document[1]/RefDoc/Content/Dialogmelding/Notat/RollerRelatertNotat[1]/HealthcareProfessional/FamilyName", xmlDoc) shouldBe document.message.responsibleHealthcareProfessional.lastName
                xPath.evaluate("/MsgHead/Document[1]/RefDoc/Content/Dialogmelding/Notat/RollerRelatertNotat[1]/HealthcareProfessional/TeleCom/TeleAddress/@V", xmlDoc) shouldBe "tel:${document.message.responsibleHealthcareProfessional.phoneNumber}"
                xPath.evaluate("/MsgHead/Document[1]/RefDoc/Content/Dialogmelding/Notat/RollerRelatertNotat[2]/RoleToPatient/@V", xmlDoc) shouldBe HelsepersonellsFunksjoner.KONTAKT_HOS_MOTTAKER.verdi
                xPath.evaluate("/MsgHead/Document[1]/RefDoc/Content/Dialogmelding/Notat/RollerRelatertNotat[2]/RoleToPatient/@S", xmlDoc) shouldBe HelsepersonellsFunksjoner.KONTAKT_HOS_MOTTAKER.kodeverk
                xPath.evaluate("/MsgHead/Document[1]/RefDoc/Content/Dialogmelding/Notat/RollerRelatertNotat[2]/RoleToPatient/@DN", xmlDoc) shouldBe HelsepersonellsFunksjoner.KONTAKT_HOS_MOTTAKER.navn
                xPath.evaluate("/MsgHead/Document[1]/RefDoc/Content/Dialogmelding/Notat/RollerRelatertNotat[2]/HealthcareProfessional/TypeHealthcareProfessional/@V", xmlDoc) shouldBe document.message.recipientContact.type.verdi
                xPath.evaluate("/MsgHead/Document[1]/RefDoc/Content/Dialogmelding/Notat/RollerRelatertNotat[2]/HealthcareProfessional/TypeHealthcareProfessional/@DN", xmlDoc) shouldBe document.message.recipientContact.type.navn
            }
        }

        OffsetDateTime.parse(xPath.evaluate("/MsgHead/Document[2]/RefDoc/IssueDate/@V", xmlDoc)) shouldBe document.vedlegg.date.truncatedTo(ChronoUnit.SECONDS)
        xPath.evaluate("/MsgHead/Document[2]/RefDoc/MsgType/@V", xmlDoc) shouldBe TypeDokumentreferanse.VEDLEGG.verdi
        xPath.evaluate("/MsgHead/Document[2]/RefDoc/MsgType/@DN", xmlDoc) shouldBe TypeDokumentreferanse.VEDLEGG.navn
        xPath.evaluate("/MsgHead/Document[2]/RefDoc/MimeType", xmlDoc) shouldBe "application/pdf"
        xPath.evaluate("/MsgHead/Document[2]/RefDoc/Description", xmlDoc) shouldBe document.vedlegg.description
        Base64.getDecoder().decode(xPath.evaluate("/MsgHead/Document[2]/RefDoc/Content/Base64Container", xmlDoc)) shouldBe vedleggBytes
    }
}
