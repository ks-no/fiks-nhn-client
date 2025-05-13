package no.ks.fiks.nhn.edi

import no.kith.xmlstds.apprec._2004_11_21.AppRec
import no.kith.xmlstds.apprec._2004_11_21.Inst
import no.kith.xmlstds.base64container.Base64Container
import no.kith.xmlstds.msghead._2006_05_24.CS
import no.kith.xmlstds.msghead._2006_05_24.CV
import no.kith.xmlstds.msghead._2006_05_24.Ident
import no.kith.xmlstds.msghead._2006_05_24.MsgHead
import no.ks.fiks.hdir.*
import no.ks.fiks.nhn.msh.*
import java.io.ByteArrayInputStream
import java.io.StringReader
import javax.xml.transform.stream.StreamSource
import no.kith.xmlstds.dialog._2006_10_11.Dialogmelding as NhnDialogmelding
import no.kith.xmlstds.msghead._2006_05_24.Organisation as NhnOrganisation

object BusinessDocumentDeserializer {

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

    fun deserializeAppRec(appRecXml: String): ApplicationReceipt {
        val appRec = XmlContext.createUnmarshaller().unmarshal(StreamSource(StringReader(appRecXml)), AppRec::class.java).value
        return ApplicationReceipt(
            id = appRec.id,
            acknowledgedBusinessDocumentId = appRec.originalMsgId.id,
            status = StatusForMottakAvMelding.entries.firstOrNull { it.verdi == appRec.status.v && it.navn == appRec.status.dn } ?: throw IllegalArgumentException("Unknown app rec status: ${appRec.status}"),
            errors = appRec?.error?.map { error ->
                ApplicationReceiptError(
                    type = FeilmeldingForApplikasjonskvittering.entries.firstOrNull { it.verdi == error.v && it.navn == error.dn } ?: FeilmeldingForApplikasjonskvittering.UKJENT,
                    description = error.ot,
                )
            } ?: emptyList(),
            sender = appRec.sender.hcp.inst.toOrganisation(),
            receiver = appRec.receiver.hcp.inst.toOrganisation(),
        )
    }

    private fun MsgHead.getType() = msgInfo.type.toMeldingensFunksjon()

    private fun MsgHead.getSender() =
        with(msgInfo.sender.organisation) {
            Organisation(
                name = organisationName,
                id = getId(),
                childOrganisation = organisation?.let { childOrganisation ->
                    with(childOrganisation) {
                        Organisation(
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
                            OrganisasjonHerIdReceiverChild(
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

    private fun CV.toIdType() = KodeverkRegister.getKodeverk(s, v, dn) as? IdType ?: throw IllegalArgumentException("Expected kodeverk of a valid IdType, but got ($s, $v, $dn)")

    private fun Inst.toOrganisation() = Organisation(
        name = name,
        id = toId(),
        childOrganisation = dept.firstOrNull()?.let {
            Organisation(
                name = it.name,
                id = toId(),
            )
        }
    )

    private fun Inst.toId() = OrganisasjonIdType.entries.firstOrNull { it.verdi == typeId.v && it.navn == typeId.dn }
        ?.let { type ->
            Id(
                id = id,
                type = type,
            )
        }
        ?: throw IllegalArgumentException("Unknown type for organisation id: $typeId")

}