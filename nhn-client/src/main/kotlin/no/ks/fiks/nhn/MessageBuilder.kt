package no.ks.fiks.nhn

import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.util.JAXBSource
import no.kith.xmlstds.base64container.Base64Container
import no.kith.xmlstds.dialog._2006_10_11.Dialogmelding
import no.kith.xmlstds.msghead._2006_05_24.*
import java.io.InputStream
import java.io.StringWriter
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*
import javax.xml.XMLConstants
import javax.xml.datatype.DatatypeFactory
import javax.xml.validation.SchemaFactory
import kotlin.random.Random.Default.nextBytes
import no.kith.xmlstds.msghead._2006_05_24.Organisation as NhnOrganisation
import no.kith.xmlstds.msghead._2006_05_24.Patient as NhnPatient
import no.kith.xmlstds.msghead._2006_05_24.Receiver as NhnReceiver

private const val MI_G_VERSION = "v1.2 2006-05-24" // Eneste gyldige verdi (?)

// Til alle dataelement av type CS er det angitt hvilket kodeverk som skal benyttes
// For de fleste dataelement av typen CV er det angitt et standard kodeverk, eller det er angitt eksempler på kodeverk som kan benyttes
object MessageBuilder {

    private val context = JAXBContext.newInstance(MsgHead::class.java, Dialogmelding::class.java, Base64Container::class.java)
    private val headSchema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
        .newSchema(ClassLoader.getSystemResource("xsd/MsgHead-v1_2.xsd"))

    fun buildNhnMessage(message: Message): String {
        val msgHead = buildMsgHead(message)
        val dialogmelding = DialogmeldingBuilder.buildDialogmelding()
        val vedlegg = buildVedlegg(message.vedlegg)

        msgHead.document = listOfNotNull(
            buildDialogmeldingDocument(dialogmelding),
            buildVedleggDocument(vedlegg),
        )

        return StringWriter()
            .also { createMarshaller().marshal(msgHead, it) }
            .toString()
    }

    private fun buildMsgHead(message: Message) = MsgHead()
        .apply {
            msgInfo = MsgInfo().apply {
                type = buildMsgInfoType(message.type)
                miGversion = MI_G_VERSION
                genDate = currentDateTime()
                msgId = createMsgId()
                sender = toSender(message.sender)
                receiver = when (message.receiver) {
                    is HerIdReceiver -> NhnReceiver().apply {
                        organisation = NhnOrganisation().apply {
                            organisationName = message.receiver.parent.name
                            ident = listOf(
                                toIdent(message.receiver.parent.id)
                            )
                            organisation = message.receiver.child
                                .let { it as? OrganisasjonHerIdReceiverChild }
                                ?.let {
                                    NhnOrganisation().apply {
                                        organisationName = it.name
                                        ident = listOf(
                                            toIdent(it.id)
                                        )
                                    }
                                }
                            healthcareProfessional = message.receiver.child
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
                patient = when (message.receiver) {
                    is HerIdReceiver -> NhnPatient().apply {
                        givenName = message.receiver.patient.firstName
                        middleName = message.receiver.patient.middleName
                        familyName = message.receiver.patient.lastName
                        ident = listOf(
                            Ident().apply {
                                id = message.receiver.patient.fnr
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
        .also { headSchema.newValidator().validate(JAXBSource(context, it)) }

    private fun buildMsgInfoType(type: MessageType) = CS().apply {
        v = type.verdi
        dn = type.navn
    }

    private fun currentDateTime() = DatatypeFactory.newInstance()
        .newXMLGregorianCalendar(
            DateTimeFormatter.ISO_OFFSET_DATE_TIME
                .format(ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS))
        )

    private fun createMsgId() = UUID.randomUUID().toString()

    private fun toSender(input: Organisation) = Sender().apply {
        organisation = toOrganisation(input)
    }

    private fun toOrganisation(input: Organisation): NhnOrganisation = NhnOrganisation().apply {
        organisationName = input.name
        ident = listOf(
            toIdent(input.id),
        )
        organisation = input.childOrganisation?.let { toOrganisation(it) }
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

    private fun buildVedlegg(inputStream: InputStream?) = inputStream?.let {
        Base64Container().apply {
            value = it.readAllBytes()
        }
    }

    private fun buildDialogmeldingDocument(dialogmelding: Dialogmelding) =
        Document().apply {
            refDoc = RefDoc().apply {
                msgType = CS().apply { // Kodeverk: 8114 Type dokumentreferanse
                    v = "XML"
                    dn = "XML-instans"
                }
                content = RefDoc.Content().apply {
                    any = listOf(
                        dialogmelding,
                    )
                }
            }
        }

    private fun buildVedleggDocument(vedlegg: Any?) = vedlegg?.let { vedleggNotNull ->
        Document().apply {
            refDoc = RefDoc().apply {
                issueDate = TS().apply {
                    v = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS)) // TODO: Dato for opprettelsen av vedlegget?
                }
                msgType = CS().apply { // Kodeverk: 8114 Type dokumentreferanse
                    v = "A"
                    dn = "Vedlegg"
                }
                mimeType = "application/pdf"
                description = "Blablabla" // TODO: Tittel på forsendelsen?
                content = RefDoc.Content().apply {
                    any = listOf(
                        vedleggNotNull
                    )
                }
            }
        }
    }

    private fun createMarshaller() = context.createMarshaller()

}
