package no.ks.fiks.nhn.edi.v1_0

import jakarta.xml.bind.JAXBContext
import no.kith.xmlstds.apprec._2004_11_21.AppRec
import no.kith.xmlstds.apprec._2004_11_21.Dept
import no.kith.xmlstds.apprec._2004_11_21.HCPerson
import no.kith.xmlstds.apprec._2004_11_21.Inst
import no.ks.fiks.hdir.FeilmeldingForApplikasjonskvittering
import no.ks.fiks.hdir.OrganizationIdType
import no.ks.fiks.hdir.PersonIdType
import no.ks.fiks.hdir.StatusForMottakAvMelding
import no.ks.fiks.nhn.msh.*
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
        status = StatusForMottakAvMelding.entries.firstOrNull { it.verdi == status.v } ?: throw IllegalArgumentException("Unknown app rec status: ${status.v}, ${status.dn}"),
        errors = error?.map { error ->
            ApplicationReceiptError(
                type = FeilmeldingForApplikasjonskvittering.entries.firstOrNull { it.verdi == error.v } ?: FeilmeldingForApplikasjonskvittering.UKJENT,
                details = error.ot,
            )
        } ?: emptyList(),
        sender = sender.hcp.inst.toInstitution(),
        receiver = receiver.hcp.inst.toInstitution(),
    )

    private fun Inst.toInstitution() = Institution(
        name = name,
        id = toId(),
        department = dept.firstOrNull()?.let {
            Department(
                name = it.name,
                id = it.toId(),
            )
        },
        person = hcPerson.firstOrNull()?.let {
            InstitutionPerson(
                name = it.name,
                id = it.toId(),
            )
        }
    )

    private fun Inst.toId() = OrganizationIdType.entries.firstOrNull { it.verdi == typeId.v }
        ?.let { type ->
            OrganizationId(
                id = id,
                type = type,
            )
        }
        ?: throw IllegalArgumentException("Unknown type for organisation id: ${typeId.v}, ${typeId.dn}")

    private fun Dept.toId() = OrganizationIdType.entries.firstOrNull { it.verdi == typeId.v }
        ?.let { type ->
            OrganizationId(
                id = id,
                type = type,
            )
        }
        ?: throw IllegalArgumentException("Unknown type for organisation id: ${typeId.v}, ${typeId.dn}")

    private fun HCPerson.toId() = PersonIdType.entries.firstOrNull { it.verdi == typeId.v }
        ?.let { type ->
            PersonId(
                id = id,
                type = type,
            )
        }
        ?: throw IllegalArgumentException("Unknown type for person id: ${typeId.v}, ${typeId.dn}")

}
