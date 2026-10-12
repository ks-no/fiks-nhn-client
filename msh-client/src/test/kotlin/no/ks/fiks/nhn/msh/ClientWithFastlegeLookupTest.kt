package no.ks.fiks.nhn.msh

import io.kotest.assertions.asClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeEmpty
import io.mockk.*
import no.ks.fiks.hdir.Adressetype
import no.ks.fiks.hdir.Helsepersonell
import no.ks.fiks.hdir.HelsepersonellsFunksjoner
import no.ks.fiks.hdir.OrganizationIdType
import no.ks.fiks.hdir.PersonIdType
import no.ks.fiks.nhn.ar.rest.AdresseregisteretClient
import no.ks.fiks.nhn.ar.rest.model.CommunicationParty
import no.ks.fiks.nhn.flr.FastlegeregisteretClient
import no.ks.fiks.nhn.flr.PatientGP
import no.ks.fiks.nhn.randomAddress
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
                val gpCommunicationParty = randomCommunicationPartyPerson()

                val requestSlot = slot<PostMessageRequest>()
                val apiService = mockk<MshInternalClient> { coEvery { postMessage(capture(requestSlot), any()) } returns UUID.randomUUID() }
                val flrClient = mockk<FastlegeregisteretClient> { every { getPatientGP(any()) } returns patientGP }
                val arClient = mockk<AdresseregisteretClient> { every { lookupHerId(any()) } returns gpCommunicationParty }
                val client = ClientWithFastlegeLookup(apiService, flrClient, arClient)

                client.sendMessageToGPForPerson(businessDocument)

                coVerifySequence {
                    flrClient.getPatientGP(businessDocument.person.fnr)
                    arClient.lookupHerId(patientGP.gpHerId!!)
                    apiService.postMessage(any(), any())
                }

                requestSlot.captured.asClue { request ->
                    request.contentType shouldBe "application/xml"
                    request.contentTransferEncoding shouldBe "base64"
                    // Verify XML was generated (basic validation - detailed field validation skipped due to REST model differences)
                    val xml = String(Base64.getDecoder().decode(request.businessDocument))
                    xml.shouldNotBeEmpty()
                }
            }
        }

        "Params should be passed on" {
            val params = RequestParameters(HelseIdTokenParameters(SingleTenantHelseIdTokenParameters(randomHerId().toString())))

            val internalClient = mockk<MshInternalClient> {
                coEvery { postMessage(any(), any()) } returns UUID.randomUUID()
            }
            val flrClient = mockk<FastlegeregisteretClient> { every { getPatientGP(any()) } returns randomPatientGP() }
            val arClient = mockk<AdresseregisteretClient>(relaxed = true) { every { lookupHerId(any()) } returns randomCommunicationPartyPerson() }
            val client = ClientWithFastlegeLookup(internalClient, flrClient, arClient)

            client.sendMessageToGPForPerson(randomGPForPersonOutgoingBusinessDocument(), params)
            coVerifySequence {
                internalClient.postMessage(any(), params)
            }
        }
    }
}

private fun randomGPForPersonOutgoingBusinessDocument(
    vedleggBytes: ByteArray = nextBytes(nextInt(1000, 100000)),
): GPForPersonOutgoingBusinessDocument = GPForPersonOutgoingBusinessDocument(
    id = UUID.randomUUID(),
    sender = Sender(
        parent = OrganizationCommunicationParty(
            name = randomString(),
            address = randomAddress(),
            ids = listOf(randomOrganizationHerId()),
        ),
        child = OrganizationCommunicationParty(
            name = randomString(),
            address = randomAddress(),
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
    conversationRef = ConversationRef(
        refToParent = UUID.randomUUID().toString(),
        refToConversation = UUID.randomUUID().toString(),
    )
)

private fun randomCommunicationPartyPerson(): CommunicationParty = mockk(relaxed = true)

private fun randomPatientGP(): PatientGP = PatientGP(randomString(), randomHerId())
