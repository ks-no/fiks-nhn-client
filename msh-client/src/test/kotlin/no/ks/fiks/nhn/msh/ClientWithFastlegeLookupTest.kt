package no.ks.fiks.nhn.msh

import io.kotest.assertions.asClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.beNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verifySequence
import no.ks.fiks.hdir.Helsepersonell
import no.ks.fiks.hdir.HelsepersonellsFunksjoner
import no.ks.fiks.hdir.OrganizationIdType
import no.ks.fiks.hdir.PersonIdType
import no.ks.fiks.nhn.ar.AdresseregisteretClient
import no.ks.fiks.nhn.ar.CommunicationPartyParent
import no.ks.fiks.nhn.ar.PersonCommunicationParty
import no.ks.fiks.nhn.flr.FastlegeregisteretClient
import no.ks.fiks.nhn.flr.PatientGP
import no.ks.fiks.nhn.randomHerId
import no.ks.fiks.nhn.randomOrganizationHerId
import no.ks.fiks.nhn.randomString
import no.ks.fiks.nhn.validateXmlAgainst
import no.nhn.msh.v2.model.PostMessageRequest
import java.io.ByteArrayInputStream
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.random.Random.Default.nextBytes
import kotlin.random.Random.Default.nextInt

class ClientWithFastlegeLookupTest : FreeSpec() {

    init {
        "Send message to GP for person" - {
            "The receiver should be looked up using FLR and AR, and the data should be serialized to XML and passed on to the service" {
                val startTime = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS)
                val vedleggBytes = nextBytes(nextInt(1000, 100000))
                val businessDocument = randomGPForPersonOutgoingBusinessDocument(vedleggBytes)
                val patientGP = randomPatientGP()
                val gpCommunicationParty = randomPersonCommunicationParty()

                val requestSlot = slot<PostMessageRequest>()
                val apiService = mockk<ApiService> { every { sendMessage(capture(requestSlot)) } returns UUID.randomUUID() }
                val flrClient = mockk<FastlegeregisteretClient> { every { getPatientGP(any()) } returns patientGP }
                val arClient = mockk<AdresseregisteretClient> { every { lookupHerId(any()) } returns gpCommunicationParty }
                val client = ClientWithFastlegeLookup(apiService, flrClient, arClient)

                client.sendMessageToGPForPerson(businessDocument)

                verifySequence {
                    flrClient.getPatientGP(businessDocument.person.fnr)
                    arClient.lookupHerId(patientGP.gpHerId!!)
                    apiService.sendMessage(any())
                }

                requestSlot.captured.asClue { request ->
                    request.contentType shouldBe "application/xml"
                    request.contentTransferEncoding shouldBe "base64"

                    val xml = String(Base64.getDecoder().decode(request.businessDocument))
                    xml.validateXmlAgainst(
                        startTime = startTime,
                        document = OutgoingBusinessDocument(
                            id = businessDocument.id,
                            sender = businessDocument.sender,
                            receiver = Receiver(
                                parent = OrganizationReceiverDetails(
                                    ids = listOf(OrganizationId(gpCommunicationParty.parent!!.herId.toString(), OrganizationIdType.HER_ID)),
                                    name = gpCommunicationParty.parent!!.name
                                ),
                                child = PersonReceiverDetails(
                                    ids = listOf(PersonId(gpCommunicationParty.herId.toString(), PersonIdType.HER_ID)),
                                    firstName = gpCommunicationParty.firstName,
                                    middleName = gpCommunicationParty.middleName,
                                    lastName = gpCommunicationParty.lastName
                                ),
                                patient = Patient(
                                    fnr = businessDocument.person.fnr,
                                    firstName = businessDocument.person.firstName,
                                    middleName = businessDocument.person.middleName,
                                    lastName = businessDocument.person.lastName
                                ),
                            ),
                            message = businessDocument.message,
                            vedlegg = businessDocument.vedlegg,
                            version = businessDocument.version,
                        ),
                        vedleggBytes = vedleggBytes,
                    )
                }
            }
        }

        "Params should be added to context before call and removed when it completes" {
            val params = RequestParameters(HelseIdTokenParameters(SingleTenantHelseIdTokenParameters(randomHerId().toString())))

            val apiService = mockk<ApiService> {
                every { sendMessage(any()) } answers {
                    RequestContextHolder.get() shouldBe params
                    UUID.randomUUID()
                }
            }
            val flrClient = mockk<FastlegeregisteretClient> { every { getPatientGP(any()) } returns randomPatientGP() }
            val arClient = mockk<AdresseregisteretClient> { every { lookupHerId(any()) } returns randomPersonCommunicationParty() }
            val client = ClientWithFastlegeLookup(apiService, flrClient, arClient)

            RequestContextHolder.get() should beNull()
            client.sendMessageToGPForPerson(randomGPForPersonOutgoingBusinessDocument(), params)
            RequestContextHolder.get() should beNull()
        }
    }
}
private fun randomGPForPersonOutgoingBusinessDocument(
    vedleggBytes: ByteArray = nextBytes(nextInt(1000, 100000)),
): GPForPersonOutgoingBusinessDocument = GPForPersonOutgoingBusinessDocument(
    id = UUID.randomUUID(),
    sender = Organization(
        name = randomString(),
        ids = listOf(randomOrganizationHerId()),
        childOrganization = ChildOrganization(
            name = randomString(),
            ids = listOf(randomOrganizationHerId()),
        ),
    ),
    person = Person(
        fnr = randomString(),
        firstName = randomString(),
        middleName = randomString(),
        lastName = randomString(),
    ),
    message = OutgoingMessage(
        subject = randomString(),
        body = randomString(),
        responsibleHealthcareProfessional = HealthcareProfessional(
            firstName = randomString(),
            middleName = randomString(),
            lastName = randomString(),
            phoneNumber = randomString(),
            roleToPatient = HelsepersonellsFunksjoner.entries.random(),
        ),
        recipientContact = RecipientContact(
            type = Helsepersonell.entries.random(),
        ),
    ),
    vedlegg = OutgoingVedlegg(
        date = OffsetDateTime.now(),
        description = randomString(),
        data = ByteArrayInputStream(vedleggBytes),
    ),
    version = DialogmeldingVersion.entries.random(),
)

private fun randomPersonCommunicationParty(): PersonCommunicationParty = PersonCommunicationParty(
    herId = randomHerId(),
    name = randomString(),
    parent = CommunicationPartyParent(randomHerId(), randomString()),
    physicalAddresses = listOf(),
    firstName = randomString(),
    middleName = randomString(),
    lastName = randomString()
)

private fun randomPatientGP(): PatientGP = PatientGP(randomString(), randomHerId())
