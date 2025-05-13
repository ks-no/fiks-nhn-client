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
import no.ks.fiks.nhn.edi.v1_0.XmlContext as XmlContext1_0
import no.ks.fiks.nhn.edi.v1_1.DialogmeldingBuilder as DialogmeldingBuilder1_1
import no.ks.fiks.nhn.edi.v1_1.XmlContext as XmlContext1_1

private const val MI_G_VERSION = "v1.2 2006-05-24" // Eneste gyldige verdi (?)

private const val MIME_TYPE_PDF = "application/pdf"

// Til alle dataelement av type CS er det angitt hvilket kodeverk som skal benyttes
// For de fleste dataelement av typen CV er det angitt et standard kodeverk, eller det er angitt eksempler på kodeverk som kan benyttes
object BusinessDocumentSerializer {

    fun serializeNhnMessage(businessDocument: OutgoingBusinessDocument): String {
        val msgHead = buildMsgHead(businessDocument)
        val dialogmelding: Any = when (businessDocument.version) {
            DialogmeldingVersion.V1_0 -> DialogmeldingBuilder1_0.buildDialogmelding(businessDocument.message)
            DialogmeldingVersion.V1_1 -> DialogmeldingBuilder1_1.buildDialogmelding(businessDocument.message)
        }

        msgHead.document = listOfNotNull(
            buildDialogmeldingDocument(dialogmelding),
            buildVedleggDocument(businessDocument.vedlegg),
        )

        return StringWriter()
            .also {
                when (businessDocument.version) {
                    DialogmeldingVersion.V1_0 -> XmlContext1_0.createMarshaller().marshal(msgHead, it)
                    DialogmeldingVersion.V1_1 -> XmlContext1_1.createMarshaller().marshal(msgHead, it)
                }
            }
            .toString()
    }

    private fun buildMsgHead(businessDocument: OutgoingBusinessDocument) = MsgHead()
        .apply {
            msgInfo = MsgInfo().apply {
                type = buildMsgInfoType(businessDocument.version)
                miGversion = MI_G_VERSION
                genDate = currentDateTime()
                msgId = businessDocument.id.toString()
                sender = toSender(businessDocument.sender)
                receiver = when (businessDocument.receiver) {
                    is HerIdReceiver -> NhnReceiver().apply {
                        organisation = NhnOrganisation().apply {
                            organisationName = businessDocument.receiver.parent.name
                            ident = listOf(
                                toIdent(businessDocument.receiver.parent.id)
                            )
                            organisation = businessDocument.receiver.child
                                .let { it as? OrganizationHerIdReceiverChild }
                                ?.let {
                                    NhnOrganisation().apply {
                                        organisationName = it.name
                                        ident = listOf(
                                            toIdent(it.id)
                                        )
                                    }
                                }
                            healthcareProfessional = businessDocument.receiver.child
                                .let { it as? PersonHerIdReceiverChild }
                                ?.let {
                                    HealthcareProfessional().apply {
                                        givenName = it.firstName
                                        middleName = it.middleName
                                        familyName = it.lastName
                                        ident = listOf(
                                            toIdent(it.id)
                                        )
                                    }
                                }
                        }
                    }
                }
                patient = when (businessDocument.receiver) {
                    is HerIdReceiver -> NhnPatient().apply {
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
                }
                document = listOf( // Kan ikke være tom før valideringen
                    Document().apply {
                        refDoc = RefDoc().apply {
                            msgType = CS()
                        }
                    }
                )
            }
        }
        .also { XmlContext1_1.validate(it) }

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
        ident = listOf(
            toIdent(input.id),
        )
        organisation = input.childOrganization?.let { toOrganisation(it) }
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

    private fun buildDialogmeldingDocument(dialogmelding: Any) =
        Document().apply {
            refDoc = RefDoc().apply {
                msgType = TypeDokumentreferanse.XML.toMsgHeadCS()
                content = RefDoc.Content().apply {
                    any = listOf(
                        dialogmelding,
                    )
                }
            }
        }

    private fun buildVedleggDocument(vedlegg: Vedlegg) = Document().apply {
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
