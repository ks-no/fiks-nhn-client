package no.ks.fiks.nhn.edi

import no.kith.xmlstds.base64container.Base64Container
import no.kith.xmlstds.msghead._2006_05_24.*
import no.kith.xmlstds.msghead._2006_05_24.HealthcareProfessional
import no.ks.fiks.hdir.*
import no.ks.fiks.nhn.msh.*
import java.io.InputStream
import java.io.StringWriter
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.xml.datatype.DatatypeFactory
import no.kith.xmlstds.msghead._2006_05_24.Organisation as NhnOrganisation
import no.kith.xmlstds.msghead._2006_05_24.Patient as NhnPatient
import no.kith.xmlstds.msghead._2006_05_24.Receiver as NhnReceiver
import no.ks.fiks.nhn.edi.v1_0.DialogmeldingBuilder as DialogmeldingBuilder1_0
import no.ks.fiks.nhn.edi.v1_1.DialogmeldingBuilder as DialogmeldingBuilder1_1

private const val MSG_HEAD_VERSION = "v1.2 2006-05-24"

private const val MIME_TYPE_PDF = "application/pdf"

object BusinessDocumentSerializer {

    fun serializeNhnMessage(businessDocument: OutgoingBusinessDocument): String {
        val msgHead = buildMsgHead(businessDocument)
            .apply {
                document = listOfNotNull(
                    buildDialogmeldingDocument(businessDocument),
                    buildVedleggDocument(businessDocument.vedlegg), // TODO: Limit vedlegg size to ~18 MB
                )
            }

        return StringWriter()
            .also { XmlContext.createMarshaller().marshal(msgHead, it) }
            .toString()
    }

    private fun buildMsgHead(businessDocument: OutgoingBusinessDocument) = MsgHead()
        .apply {
            msgInfo = MsgInfo().apply {
                type = buildMsgInfoType(businessDocument.version)
                miGversion = MSG_HEAD_VERSION
                genDate = currentDateTime()
                msgId = businessDocument.id.toString()
                sender = toSender(businessDocument.sender)
                receiver = NhnReceiver().apply {
                    organisation = NhnOrganisation().apply {
                        organisationName = businessDocument.receiver.parent.name
                        ident = businessDocument.receiver.parent.ids.map { toIdent(it) }
                        organisation = businessDocument.receiver.child
                            .let { it as? OrganizationReceiverDetails }
                            ?.let { org ->
                                NhnOrganisation().apply {
                                    organisationName = org.name
                                    ident = org.ids.map { toIdent(it) }
                                }
                            }
                        healthcareProfessional = businessDocument.receiver.child
                            .let { it as? PersonReceiverDetails }
                            ?.let { person ->
                                HealthcareProfessional().apply {
                                    givenName = person.firstName
                                    middleName = person.middleName
                                    familyName = person.lastName
                                    ident = person.ids.map { toIdent(it) }
                                }
                            }
                    }
                }
                patient = NhnPatient().apply {
                    givenName = businessDocument.receiver.patient.firstName
                    middleName = businessDocument.receiver.patient.middleName
                    familyName = businessDocument.receiver.patient.lastName
                    ident = listOf(
                        Ident().apply {
                            id = businessDocument.receiver.patient.fnr
                            typeId = toCv(PersonIdType.FNR)
                        }
                    )
                }
                document = listOf( // Add empty doc for validation, which is overwritten later
                    Document().apply {
                        refDoc = RefDoc().apply {
                            msgType = CS()
                        }
                    }
                )
            }
        }
        .also { XmlContext.validate(it) }

    private fun buildMsgInfoType(version: DialogmeldingVersion) =
        when (version) {
            DialogmeldingVersion.V1_0 -> MeldingensFunksjon.DIALOG_FORESPORSEL
            DialogmeldingVersion.V1_1 -> MeldingensFunksjon.DIALOG_HELSEFAGLIG
        }.run {
            CS().apply {
                v = verdi
                dn = navn
            }
        }

    private fun currentDateTime() = DatatypeFactory.newInstance()
        .newXMLGregorianCalendar(
            DateTimeFormatter.ISO_OFFSET_DATE_TIME
                .format(ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS))
        )

    private fun toSender(input: Organization) = Sender().apply {
        organisation = toOrganisation(input)
    }

    private fun toOrganisation(input: Organization): NhnOrganisation = NhnOrganisation().apply {
        organisationName = input.name
        ident = input.ids.map { toIdent(it) }
        organisation = input.childOrganization?.let { toOrganisation(it) }
    }

    private fun toOrganisation(input: ChildOrganization): NhnOrganisation = NhnOrganisation().apply {
        organisationName = input.name
        ident = input.ids.map { toIdent(it) }
    }

    private fun toIdent(input: Id) = Ident().apply {
        id = input.id
        typeId = toCv(input.type)
    }

    private fun toCv(type: IdType) = CV().apply {
        v = type.verdi
        dn = type.navn
        s = type.kodeverk
    }

    private fun buildDialogmeldingDocument(businessDocument: OutgoingBusinessDocument) =
        Document().apply {
            refDoc = RefDoc().apply {
                msgType = TypeDokumentreferanse.XML.toMsgHeadCS()
                content = RefDoc.Content().apply {
                    any = listOf(
                        when (businessDocument.version) {
                            DialogmeldingVersion.V1_0 -> DialogmeldingBuilder1_0.buildDialogmelding(businessDocument.message)
                            DialogmeldingVersion.V1_1 -> DialogmeldingBuilder1_1.buildDialogmelding(businessDocument.message)
                        },
                    )
                }
            }
        }

    private fun buildVedleggDocument(vedlegg: OutgoingVedlegg) = Document().apply {
        refDoc = RefDoc().apply {
            issueDate = TS().apply {
                v = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(vedlegg.date.truncatedTo(ChronoUnit.SECONDS))
            }
            msgType = TypeDokumentreferanse.VEDLEGG.toMsgHeadCS()
            mimeType = MIME_TYPE_PDF
            description = vedlegg.description
            content = RefDoc.Content().apply {
                any = listOf(
                    buildContainer(vedlegg.data)
                )
            }
        }
    }

    private fun buildContainer(data: InputStream) =
        Base64Container().apply {
            value = data.readAllBytes()
        }

}

private fun KodeverkVerdi.toMsgHeadCS() = CS().apply {
    v = verdi
    dn = navn
}
