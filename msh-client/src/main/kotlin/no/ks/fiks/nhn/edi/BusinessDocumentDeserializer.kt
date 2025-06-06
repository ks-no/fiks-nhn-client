package no.ks.fiks.nhn.edi

import mu.KotlinLogging
import no.kith.xmlstds.base64container.Base64Container
import no.kith.xmlstds.msghead._2006_05_24.CS
import no.kith.xmlstds.msghead._2006_05_24.CV
import no.kith.xmlstds.msghead._2006_05_24.Ident
import no.kith.xmlstds.msghead._2006_05_24.MsgHead
import no.ks.fiks.hdir.*
import no.ks.fiks.nhn.msh.*
import java.io.ByteArrayInputStream
import java.io.StringReader
import java.time.OffsetDateTime
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants
import javax.xml.transform.stream.StreamSource
import no.kith.xmlstds.dialog._2006_10_11.Dialogmelding as NhnDialogmelding1_0
import no.kith.xmlstds.dialog._2013_01_23.Dialogmelding as NhnDialogmelding1_1
import no.kith.xmlstds.msghead._2006_05_24.Organisation as NhnOrganisation
import no.ks.fiks.nhn.edi.v1_0.AppRecDeserializer as AppRecDeserializer1_0
import no.ks.fiks.nhn.edi.v1_1.AppRecDeserializer as AppRecDeserializer1_1

private const val VERSION_1_0 = "1.0 2004-11-21"
private const val VERSION_1_1 = "v1.1 2012-02-15"

private val log = KotlinLogging.logger { }

object BusinessDocumentDeserializer {

    private val factory = XMLInputFactory.newInstance()

    fun deserializeMsgHead(msgHeadXml: String): IncomingBusinessDocument {
        val msgHead = XmlContext.createUnmarshaller().unmarshal(StreamSource(StringReader(msgHeadXml)), MsgHead::class.java).value
        return IncomingBusinessDocument(
            id = msgHead.msgInfo.msgId,
            type = msgHead.getType(),
            sender = msgHead.getSender(),
            receiver = msgHead.getReceiver(),
            message = msgHead.getMessage(),
            vedlegg = msgHead.getVedlegg(),
        )
    }

    fun deserializeAppRec(appRecXml: String): IncomingApplicationReceipt =
        when (getAppRecVersion(appRecXml)) {
            AppRecVersion.V1_0 -> AppRecDeserializer1_0.toApplicationReceipt(appRecXml)
            AppRecVersion.V1_1 -> AppRecDeserializer1_1.toApplicationReceipt(appRecXml)
        }

    private fun getAppRecVersion(appRecXml: String) = getVersion(appRecXml).let { version ->
        when (version) {
            VERSION_1_0 -> AppRecVersion.V1_0
            VERSION_1_1 -> AppRecVersion.V1_1
            null -> throw RuntimeException("Could not find MIGversion in XML")
            else -> throw RuntimeException("Unknown version for AppRec: $version")
        }
    }

    private fun getVersion(appRecXml: String): String? {
        val reader = factory.createXMLStreamReader(StringReader(appRecXml))

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

    private fun MsgHead.getType() = msgInfo.type.toMeldingensFunksjon()

    private fun MsgHead.getSender() =
        with(msgInfo.sender.organisation) {
            Organization(
                name = organisationName,
                id = getId(),
                childOrganization = organisation?.let { childOrganisation ->
                    with(childOrganisation) {
                        ChildOrganization(
                            name = organisationName,
                            id = getId(),
                        )
                    }
                } ?: throw IllegalArgumentException("No child organization for ${msgInfo.sender.organisation}")
            )
        }

    private fun MsgHead.getReceiver() =
        with(msgInfo.receiver.organisation) {
            Receiver(
                parent = OrganizationReceiverDetails(
                    id = ident.getOrganisasjonId(),
                    name = organisationName,
                ),
                child = organisation
                    ?.let {
                        with(organisation) {
                            OrganizationReceiverDetails(
                                id = ident.getOrganisasjonId(),
                                name = it.organisationName,
                            )
                        }
                    }
                    ?: with(healthcareProfessional) {
                        PersonReceiverDetails(
                            id = ident.getPersonId(),
                            firstName = givenName,
                            middleName = middleName,
                            lastName = familyName,
                        )
                    },
                patient = getPatient(),
            )
        }

    private fun MsgHead.getPatient() =
        with(msgInfo.patient) {
            Patient(
                fnr = ident.getPersonId().id,
                firstName = givenName,
                middleName = middleName,
                lastName = familyName,
            )
        }

    private fun MsgHead.getMessage(): IncomingMessage? =
        document.firstOrNull()
            ?.refDoc
            ?.content
            ?.any
            ?.singleOrNull()
            ?.let {
                when (it) {
                    is NhnDialogmelding1_0 -> it.getContent()
                    is NhnDialogmelding1_1 -> it.getContent()
                    else -> throw RuntimeException("Unsupported message type: $it")
                }
            }

    private fun NhnDialogmelding1_0.getContent(): IncomingMessage? = getDialogmelding()

    private fun NhnDialogmelding1_1.getContent(): IncomingMessage? = getHelsefagligDialog()

    private fun NhnDialogmelding1_0.getDialogmelding(): IncomingMessage? =
        foresporsel
            ?.singleOrNull()
            ?.let { foresporsel ->
                Dialogmelding(
                    type = TypeOpplysningPasientsamhandling.entries.firstOrNull { it.verdi == foresporsel.typeForesp.v }
                        ?: throw IllegalArgumentException("Unknown type for typeForesp: ${foresporsel.typeForesp.v}, ${foresporsel.typeForesp.dn}, ${foresporsel.typeForesp.s}, ${foresporsel.typeForesp.ot}"),
                    sporsmal = foresporsel.sporsmal,
                )
            }

    private fun NhnDialogmelding1_1.getHelsefagligDialog(): IncomingMessage? =
        notat
            ?.singleOrNull()
            ?.let { notat ->
                HelsefagligDialog(
                    tema = TemaForHelsefagligDialog.entries.firstOrNull { it.verdi == notat.temaKodet.v }
                        ?: throw IllegalArgumentException("Unknown tema: ${notat.temaKodet.v}, ${notat.temaKodet.dn}, ${notat.temaKodet.s}, ${notat.temaKodet.ot}"),
                )
            }

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

    private fun CS.toMeldingensFunksjon() = MeldingensFunksjon.entries.first { it.verdi == v }

    private fun CS.toTypeDokumentreferanse() = TypeDokumentreferanse.entries.firstOrNull { it.verdi == v }

    private fun NhnOrganisation.getId() = ident.getId()

    private fun List<Ident>.getPersonId() = getId().let {
        it as? PersonId ?: throw IllegalArgumentException("Expected id with type PersonId, but got $it")
    }

    private fun List<Ident>.getOrganisasjonId() = getId().let {
        it as? OrganizationId ?: throw IllegalArgumentException("Expected id with type OrganisasjonId, but got $it")
    }

    private fun List<Ident>.getId() =
        with(single()) {
            when (val type = typeId.toIdType()) {
                is PersonIdType -> PersonId(id, type)
                is OrganizationIdType -> OrganizationId(id, type)
            }
        }

    private fun CV.toIdType() = KodeverkRegister.getKodeverk(s, v) as? IdType ?: throw IllegalArgumentException("Expected kodeverk of a valid IdType, but got ($s, $v, $dn)")

}

private enum class AppRecVersion {
    V1_0,
    V1_1,
}
