package no.ks.fiks.nhn.edi.v1_1

import jakarta.xml.bind.JAXBContext
import no.kith.xmlstds.apprec._2012_02_15.AppRec
import no.kith.xmlstds.apprec._2012_02_15.Dept
import no.kith.xmlstds.apprec._2012_02_15.HCPerson
import no.kith.xmlstds.apprec._2012_02_15.Inst
import no.ks.fiks.hdir.FeilmeldingForApplikasjonskvittering
import no.ks.fiks.hdir.OrganizationIdType
import no.ks.fiks.hdir.PersonIdType
import no.ks.fiks.hdir.StatusForMottakAvMelding
import no.ks.fiks.nhn.edi.v1_0.AppRecDeserializer
import no.ks.fiks.nhn.msh.*
import java.io.StringReader
import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory

object AppRecDeserializer {

    private val context = JAXBContext.newInstance(AppRec::class.java)
    private val headSchema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
        .newSchema(
            arrayOf(
                StreamSource(ClassLoader.getSystemResourceAsStream("xsd/kith.xsd")),
                StreamSource(ClassLoader.getSystemResourceAsStream("xsd/apprec-v1.1.xsd")),
            )
        )

    fun toApplicationReceipt(appRecXml: String): IncomingApplicationReceipt {
        headSchema.newValidator().validate(StreamSource(StringReader(appRecXml)))
        return context.createUnmarshaller()
            .unmarshal(StreamSource(StringReader(appRecXml)), AppRec::class.java)
            .value
            .toApplicationReceipt()
    }

    private fun AppRec.toApplicationReceipt() = IncomingApplicationReceipt(
        id = id,
        acknowledgedBusinessDocumentId = originalMsgId.id,
        status = StatusForMottakAvMelding.entries.firstOrNull { it.verdi == status.v } ?: throw IllegalArgumentException("Unknown app rec status: ${status.v}, ${status.dn}"),
        errors = error?.map { error ->
            IncomingApplicationReceiptError(
                type = FeilmeldingForApplikasjonskvittering.entries.firstOrNull { it.verdi == error.v } ?: FeilmeldingForApplikasjonskvittering.UKJENT,
                details = error.ot,
                errorCode = error.v,
                description = error.dn,
                oid = error.s,
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
