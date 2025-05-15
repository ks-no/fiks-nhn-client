package no.ks.fiks.nhn.edi.v1_1

import jakarta.xml.bind.JAXBContext
import no.kith.xmlstds.apprec._2012_02_15.AppRec
import no.kith.xmlstds.apprec._2012_02_15.Inst
import no.ks.fiks.hdir.FeilmeldingForApplikasjonskvittering
import no.ks.fiks.hdir.OrganisasjonIdType
import no.ks.fiks.hdir.StatusForMottakAvMelding
import no.ks.fiks.nhn.msh.IncomingApplicationReceipt
import no.ks.fiks.nhn.msh.ApplicationReceiptError
import no.ks.fiks.nhn.msh.Id
import no.ks.fiks.nhn.msh.Organization
import java.io.StringReader
import javax.xml.transform.stream.StreamSource

object AppRecDeserializer {

    private val context = JAXBContext.newInstance(AppRec::class.java)

    fun toApplicationReceipt(appRecXml: String) = context.createUnmarshaller()
        .unmarshal(StreamSource(StringReader(appRecXml)), AppRec::class.java)
        .value
        .toApplicationReceipt()

    private fun AppRec.toApplicationReceipt() = IncomingApplicationReceipt(
        id = id,
        acknowledgedBusinessDocumentId = originalMsgId.id,
        status = StatusForMottakAvMelding.entries.firstOrNull { it.verdi == status.v && it.navn == status.dn } ?: throw IllegalArgumentException("Unknown app rec status: ${status}"),
        errors = error?.map { error ->
            ApplicationReceiptError(
                type = FeilmeldingForApplikasjonskvittering.entries.firstOrNull { it.verdi == error.v && it.navn == error.dn } ?: FeilmeldingForApplikasjonskvittering.UKJENT,
                details = error.ot,
            )
        } ?: emptyList(),
        sender = sender.hcp.inst.toOrganization(),
        receiver = receiver.hcp.inst.toOrganization(),
    )

    private fun Inst.toOrganization() = Organization(
        name = name,
        id = toId(),
        childOrganization = dept.firstOrNull()?.let {
            Organization(
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
