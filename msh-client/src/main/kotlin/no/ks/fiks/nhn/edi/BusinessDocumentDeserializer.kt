package no.ks.fiks.nhn.edi

import no.kith.xmlstds.base64container.Base64Container
import no.kith.xmlstds.msghead._2006_05_24.CS
import no.kith.xmlstds.msghead._2006_05_24.CV
import no.kith.xmlstds.msghead._2006_05_24.Ident
import no.kith.xmlstds.msghead._2006_05_24.MsgHead
import no.ks.fiks.hdir.*
import no.ks.fiks.nhn.msh.*
import java.io.ByteArrayInputStream
import java.io.StringReader
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants
import javax.xml.transform.stream.StreamSource
import no.kith.xmlstds.dialog._2006_10_11.Dialogmelding as NhnDialogmelding
import no.kith.xmlstds.msghead._2006_05_24.Organisation as NhnOrganisation
import no.ks.fiks.nhn.edi.v1_0.AppRecDeserializer as AppRecDeserializer1_0
import no.ks.fiks.nhn.edi.v1_1.AppRecDeserializer as AppRecDeserializer1_1

private const val VERSION_1_0 = "1.0 2004-11-21"
private const val VERSION_1_1 = "v1.1 2012-02-15"

object BusinessDocumentDeserializer {

    private val factory = XMLInputFactory.newInstance()

    fun deserializeMsgHead(msgHeadXml: String): IncomingBusinessDocument {
        val msgHead = XmlContext.createUnmarshaller().unmarshal(StreamSource(StringReader(msgHeadXml)), MsgHead::class.java).value
        return IncomingBusinessDocument(
            id = msgHead.msgInfo.msgId,
            type = msgHead.getType(),
            sender = msgHead.getSender(),
            receiver = msgHead.getReceiver(),
            dialogmelding = msgHead.getDialogmelding(),
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
                        Organization(
                            name = organisationName,
                            id = getId(),
                        )
                    }
                }
            )
        }

    private fun MsgHead.getReceiver() =
        with(msgInfo.receiver.organisation) {
            HerIdReceiver(
                parent = HerIdReceiverParent(
                    name = organisationName,
                    id = getId(),
                ),
                child = organisation
                    ?.let {
                        with(organisation) {
                            OrganizationHerIdReceiverChild(
                                name = it.organisationName,
                                id = getId(),
                            )
                        }
                    }
                    ?: with(healthcareProfessional) {
                        PersonHerIdReceiverChild(
                            id = ident.getId(),
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
                fnr = ident.getId().let {
                    if (it.type !is PersonIdType) throw IllegalArgumentException("Expected id of type PersonIdType, but got ${it.type}")
                    else it.id
                },
                firstName = givenName,
                middleName = middleName,
                lastName = familyName,
            )
        }

    private fun MsgHead.getDialogmelding() =
        document.firstOrNull()?.refDoc?.content?.any?.singleOrNull()?.let { any ->
            (any as? NhnDialogmelding)
                ?.let { dialogmelding ->
                    dialogmelding.foresporsel?.singleOrNull()?.let { foresporsel ->
                        Dialogmelding(
                            type = TypeOpplysningPasientsamhandling.entries.firstOrNull { it.verdi == foresporsel.typeForesp.v && it.navn == foresporsel.typeForesp.dn }
                                ?: throw IllegalArgumentException("Unknown type for typeForesp: ${foresporsel.typeForesp.v}, ${foresporsel.typeForesp.dn}, ${foresporsel.typeForesp.s}, ${foresporsel.typeForesp.ot}"),
                            sporsmal = foresporsel.sporsmal,
                        )
                    }
                }
        }

    private fun MsgHead.getVedlegg() =
        document.drop(1).singleOrNull()?.let { doc ->
            val content = doc.refDoc.content.any.single()
            (content as? Base64Container)
                ?.let { ByteArrayInputStream(it.value) }
                ?: throw IllegalArgumentException("Expected Base64Container, but got $content")
        }

    private fun CS.toMeldingensFunksjon() = MeldingensFunksjon.entries.first { it.verdi == v && it.navn == dn }

    private fun NhnOrganisation.getId() = ident.getId()

    private fun List<Ident>.getId() =
        with(single()) {
            Id(
                id = id,
                type = typeId.toIdType(),
            )
        }

    private fun CV.toIdType() = KodeverkRegister.getKodeverk(s, v) as? IdType ?: throw IllegalArgumentException("Expected kodeverk of a valid IdType, but got ($s, $v, $dn)")

}

private enum class AppRecVersion {
    V1_0,
    V1_1,
}
