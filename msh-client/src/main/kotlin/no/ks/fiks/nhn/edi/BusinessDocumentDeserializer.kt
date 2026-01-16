package no.ks.fiks.nhn.edi

import mu.KotlinLogging
import no.kith.xmlstds.base64container.Base64Container
import no.kith.xmlstds.msghead._2006_05_24.CS
import no.kith.xmlstds.msghead._2006_05_24.CV
import no.kith.xmlstds.msghead._2006_05_24.Ident
import no.kith.xmlstds.msghead._2006_05_24.MsgHead
import no.ks.fiks.hdir.*
import no.ks.fiks.nhn.msh.*
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import java.io.StringReader
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.GregorianCalendar
import java.util.TimeZone
import javax.xml.datatype.DatatypeConstants.FIELD_UNDEFINED
import javax.xml.datatype.XMLGregorianCalendar
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants
import javax.xml.transform.stream.StreamSource
import no.kith.xmlstds.dialog._2006_10_11.Dialogmelding as NhnDialogmelding1_0
import no.kith.xmlstds.dialog._2013_01_23.Dialogmelding as NhnDialogmelding1_1
import no.kith.xmlstds.msghead._2006_05_24.Organisation as NhnOrganisation
import no.ks.fiks.nhn.edi.v1_0.AppRecDeserializer as AppRecDeserializer1_0
import no.ks.fiks.nhn.edi.v1_1.AppRecDeserializer as AppRecDeserializer1_1

private const val MSG_HEAD_ROOT = "MsgHead"
private const val APP_REC_ROOT = "AppRec"

private const val MSG_HEAD_VERSION = "v1.2 2006-05-24"

private const val APPREC_VERSION_1_0 = "1.0 2004-11-21"
private const val APPREC_VERSION_1_1 = "v1.1 2012-02-15"

private const val DEFAULT_ZONE = "Europe/Oslo"

private val log = KotlinLogging.logger { }

object BusinessDocumentDeserializer {

    private val factory = XMLInputFactory.newInstance()

    fun deserializeMsgHead(msgHeadXml: String): IncomingBusinessDocument {
        validateRootElement(msgHeadXml, MSG_HEAD_ROOT)
        if (getVersion(msgHeadXml) != MSG_HEAD_VERSION) throw IllegalArgumentException("Invalid MIGversion. Only $MSG_HEAD_VERSION is supported.")
        XmlContext.validateXml(msgHeadXml)
        val msgHead = XmlContext.createUnmarshaller().unmarshal(StreamSource(StringReader(msgHeadXml)), MsgHead::class.java).value
        if (msgHead.msgInfo == null) throw IllegalArgumentException("Could not find MsgInfo in the provided XML. The message is invalid or of wrong type.")
        return IncomingBusinessDocument(
            id = msgHead.msgInfo.msgId,
            date = msgHead.msgInfo.genDate.toOffsetDateTime(),
            type = msgHead.getType(),
            sender = msgHead.getSender(),
            receiver = msgHead.getReceiver(),
            message = msgHead.getMessage(),
            vedlegg = msgHead.getVedlegg(),
        )
    }

    fun deserializeAppRec(appRecXml: String): IncomingApplicationReceipt {
        validateRootElement(appRecXml, APP_REC_ROOT)
        return when (getAppRecVersion(appRecXml)) {
            AppRecVersion.V1_0 -> AppRecDeserializer1_0.toApplicationReceipt(appRecXml)
            AppRecVersion.V1_1 -> AppRecDeserializer1_1.toApplicationReceipt(appRecXml)
        }
    }

    private fun validateRootElement(xml: String, expectedRoot: String) {
        getRootElement(xml).also { if (it != expectedRoot) throw IllegalArgumentException("Expected $expectedRoot as root element, but found $it") }
    }

    private fun getAppRecVersion(appRecXml: String) = getVersion(appRecXml).let { version ->
        when (version) {
            APPREC_VERSION_1_0 -> AppRecVersion.V1_0
            APPREC_VERSION_1_1 -> AppRecVersion.V1_1
            null -> throw IllegalArgumentException("Could not find MIGversion in XML")
            else -> throw IllegalArgumentException("Unknown version for AppRec: $version")
        }
    }

    private fun getVersion(xml: String): String? {
        val reader = factory.createXMLStreamReader(StringReader(xml))

        while (reader.hasNext()) {
            if (reader.next() == XMLStreamConstants.START_ELEMENT) {
                if (reader.localName == "MIGversion") {
                    reader.next()
                    return reader.text
                }
            }
        }
        return null
    }

    private fun getRootElement(xml: String): String? {
        val reader = factory.createXMLStreamReader(StringReader(xml))

        var iterations = 0
        while (reader.hasNext() && reader.next() != XMLStreamConstants.START_ELEMENT && iterations < 100) {
            iterations++
        }
        return reader.localName
    }

    private fun MsgHead.getType() = msgInfo.type.toMeldingensFunksjon()

    private fun MsgHead.getSender() =
        with(msgInfo.sender.organisation) {
            Sender(
                parent = getParent(),
                child = getChild(),
            )
        }

    private fun MsgHead.getReceiver() =
        with(msgInfo.receiver.organisation) {
            Receiver(
                parent = getParent(),
                child = getChild(),
                patient = getPatient(),
            )
        }

    private fun NhnOrganisation.getParent() = OrganizationCommunicationParty(
        ids = ident.getOrganisasjonId(),
        name = organisationName,
    )

    private fun NhnOrganisation.getChild() = organisation
        ?.let {
            with(organisation) {
                OrganizationCommunicationParty(
                    ids = ident.getOrganisasjonId(),
                    name = it.organisationName,
                )
            }
        }
        ?: with(healthcareProfessional) {
            PersonCommunicationParty(
                ids = ident.getPersonId(),
                firstName = givenName,
                middleName = middleName,
                lastName = familyName,
            )
        }

    private fun MsgHead.getPatient() =
        with(msgInfo.patient) {
            Patient(
                fnr = ident.getPersonId().let { ids -> ids.firstOrNull()?.id ?: throw IllegalArgumentException("Found multiple ids for patient: $ids") },
                firstName = givenName,
                middleName = middleName,
                lastName = familyName,
            )
        }

    private fun MsgHead.getMessage(): Dialogmelding? =
        document.firstOrNull()
            ?.refDoc
            ?.content
            ?.any
            ?.singleOrNull()
            ?.let {
                when (it) {
                    is NhnDialogmelding1_0 -> it.convert()
                    is NhnDialogmelding1_1 -> it.convert()
                    else -> throw IllegalArgumentException("Unsupported message type: $it")
                }
            }

    private fun NhnDialogmelding1_0.convert() = Dialogmelding(
        foresporsel = readForesporsel(),
        notat = readNotat(),
    )

    private fun NhnDialogmelding1_1.convert() = Dialogmelding(
        foresporsel = readForesporsel(),
        notat = readNotat(),
    )

    private fun NhnDialogmelding1_0.readForesporsel(): Foresporsel? =
        foresporsel
            ?.singleOrNull()
            ?.let { foresporsel ->
                Foresporsel(
                    type = TypeOpplysningPasientsamhandlingPleieOgOmsorg.entries.firstOrNull { it.verdi == foresporsel.typeForesp.v }
                        ?: throw IllegalArgumentException("Unknown type for typeForesp: ${foresporsel.typeForesp.v}, ${foresporsel.typeForesp.dn}, ${foresporsel.typeForesp.s}, ${foresporsel.typeForesp.ot}"),
                    sporsmal = foresporsel.sporsmal,
                )
            }

    private fun NhnDialogmelding1_0.readNotat(): Notat? =
        notat
            ?.singleOrNull()
            ?.let { notat ->
                Notat(
                    tema = KodeverkRegister.getKodeverk(notat.temaKodet.s, notat.temaKodet.v),
                    temaBeskrivelse = notat.tema,
                    innhold = notat.tekstNotatInnhold.getText(),
                    dato = notat.datoNotat?.toLocalDate(),
                )
            }

    private fun NhnDialogmelding1_1.readForesporsel(): Foresporsel? =
        foresporsel
            ?.singleOrNull()
            ?.let { foresporsel ->
                Foresporsel(
                    type = TypeOpplysningPasientsamhandlingPleieOgOmsorg.entries.firstOrNull { it.verdi == foresporsel.typeForesp.v }
                        ?: throw IllegalArgumentException("Unknown type for typeForesp: ${foresporsel.typeForesp.v}, ${foresporsel.typeForesp.dn}, ${foresporsel.typeForesp.s}, ${foresporsel.typeForesp.ot}"),
                    sporsmal = foresporsel.sporsmal as? String,
                )
            }

    private fun NhnDialogmelding1_1.readNotat(): Notat? =
        notat
            ?.singleOrNull()
            ?.let { notat ->
                Notat(
                    tema = KodeverkRegister.getKodeverk(notat.temaKodet.s, notat.temaKodet.v),
                    temaBeskrivelse = notat.tema,
                    innhold = notat.tekstNotatInnhold.getText(),
                    dato = notat.datoNotat?.toLocalDate(),
                )
            }

    private fun Any?.getText() = (this as? Node)?.firstChild?.nodeValue

    private fun XMLGregorianCalendar.toLocalDate() = toZonedDateTime().withZoneSameInstant(ZoneId.of(DEFAULT_ZONE)).toLocalDate()

    private fun XMLGregorianCalendar.toOffsetDateTime() = toZonedDateTime().toOffsetDateTime()

    private fun XMLGregorianCalendar.toZonedDateTime() = toGregorianCalendarWithZone().toZonedDateTime()

    private fun XMLGregorianCalendar.toGregorianCalendarWithZone() =
        if (timezone == FIELD_UNDEFINED)
            toGregorianCalendar(TimeZone.getTimeZone(DEFAULT_ZONE), null, null) // If XML timestamp does not have offset, consider it to be Norwegian timezone
        else
            toGregorianCalendar() // Use offset from XML timestamp

    private fun MsgHead.getVedlegg() =
        document.drop(1).singleOrNull()?.let { doc ->
            doc.refDoc.let { refDoc ->
                refDoc
                    .takeIf { refDoc.msgType?.toTypeDokumentreferanse() == TypeDokumentreferanse.VEDLEGG }
                    .also { if (it == null) log.info { "Ignoring ref doc of type ${refDoc.msgType.v}, as only 'A, Vedlegg' is supported" } }
                    ?.let {
                        IncomingVedlegg(
                            date = refDoc.issueDate.v?.let { OffsetDateTime.parse(it) },
                            description = refDoc.description,
                            mimeType = refDoc.mimeType,
                            data = (refDoc.content.any.single() as? Base64Container)
                                ?.let { ByteArrayInputStream(it.value) }
                                ?: throw IllegalArgumentException("Expected Base64Container, but got ${refDoc.content}"),
                        )
                    }
            }
        }

    private fun CS.toMeldingensFunksjon() = MeldingensFunksjon.entries.firstOrNull { it.verdi == v } ?: throw IllegalArgumentException("Unknown message type: $v, $dn")

    private fun CS.toTypeDokumentreferanse() = TypeDokumentreferanse.entries.firstOrNull { it.verdi == v }

    private fun NhnOrganisation.getId() = ident.getId()

    private fun List<Ident>.getPersonId() = getId().map {
        it as? PersonId ?: throw IllegalArgumentException("Expected id with type PersonId, but got $it")
    }

    private fun List<Ident>.getOrganisasjonId() = getId().map {
        it as? OrganizationId ?: throw IllegalArgumentException("Expected id with type OrganisasjonId, but got $it")
    }

    private fun List<Ident>.getId() =
        map {
            when (val type = it.typeId.toIdType()) {
                is PersonIdType -> PersonId(it.id, type)
                is OrganizationIdType -> OrganizationId(it.id, type)
            }
        }

    private fun CV.toIdType() = KodeverkRegister.getKodeverk(s, v) as? IdType ?: throw IllegalArgumentException("Expected kodeverk of a valid IdType, but got ($s, $v, $dn)")

}

private enum class AppRecVersion {
    V1_0,
    V1_1,
}
