package no.ks.fiks.nhn.msh

import io.kotest.assertions.asClue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.beNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import no.ks.fiks.hdir.FeilmeldingForApplikasjonskvittering
import no.ks.fiks.hdir.Helsepersonell
import no.ks.fiks.hdir.HelsepersonellsFunksjoner
import no.ks.fiks.hdir.MeldingensFunksjon
import no.ks.fiks.hdir.OrganizationIdType
import no.ks.fiks.hdir.PersonIdType
import no.ks.fiks.hdir.StatusForMottakAvMelding
import no.ks.fiks.nhn.*
import no.ks.fiks.nhn.msh.MshConfiguration as ClientMshConfiguration
import no.ks.fiks.nhn.msh.RejectMessageFilters as ClientRejectMessageFilters
import no.nhn.msh.v3.model.AppRecError as NhnAppRecError
import no.nhn.msh.v3.model.AppRecStatus as NhnAppRecStatus
import no.nhn.msh.v3.model.ApprecInfo as NhnApprecInfo
import no.nhn.msh.v3.model.DeliveryState as NhnDeliveryState
import no.nhn.msh.v3.model.GetBusinessDocumentResponse as NhnGetBusinessDocumentResponse
import no.nhn.msh.v3.model.GetMessageResponse as NhnGetMessageResponse
import no.nhn.msh.v3.model.PostAppRecRequest as NhnPostAppRecRequest
import no.nhn.msh.v3.model.PostMessageRequest as NhnPostMessageRequest
import no.nhn.msh.v3.model.StatusInfo as NhnStatusInfo
import no.nhn.msh.v3.model.UnreadNotification as NhnUnreadNotification
import java.io.ByteArrayInputStream
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.Base64
import java.util.UUID
import kotlin.random.Random.Default.nextBoolean
import kotlin.random.Random.Default.nextBytes
import kotlin.random.Random.Default.nextInt

class ClientTest : FreeSpec() {

    init {

        "Send message" - {
            "The data should be serialized to XML and passed on to the service" {
                val startTime = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS)
                val vedleggBytes = nextBytes(nextInt(1000, 100000))
                val businessDocument = randomOutgoingBusinessDocument(vedleggBytes)

                val requestSlot = slot<NhnPostMessageRequest>()
                val internalClient = mockk<MshInternalClient> {
                    coEvery { postMessage(capture(requestSlot), any()) } returns UUID.randomUUID()
                }
                val client = Client(internalClient, testApplicationMetadata)

                client.sendMessage(businessDocument)

                coVerifySequence {
                    internalClient.postMessage(any(), any())
                }

                requestSlot.captured.asClue { request ->
                    request.contentType shouldBe "application/xml"
                    request.contentTransferEncoding shouldBe "base64"

                    val xml = String(Base64.getDecoder().decode(request.businessDocument))
                    xml.validateXmlAgainst(
                        startTime = startTime,
                        document = businessDocument,
                        vedleggBytes = listOf(vedleggBytes),
                    )
                }
            }

            "Params should be passed on" {
                val params = RequestParameters(HelseIdTokenParameters(SingleTenantHelseIdTokenParameters(randomHerId().toString())))

                val internalClient = mockk<MshInternalClient> {
                    coEvery { postMessage(any(), any()) } returns UUID.randomUUID()
                }
                val client = Client(internalClient, testApplicationMetadata)

                client.sendMessage(randomOutgoingBusinessDocument(), params)
                coVerifySequence {
                    internalClient.postMessage(any(), params)
                }
            }

            "Should be able to send OK receipt without errors" {
                val returnedId = UUID.randomUUID()
                val requestSlot = slot<NhnPostAppRecRequest>()
                val internalClient = mockk<MshInternalClient> {
                    coEvery { postAppRec(any(), capture(requestSlot), any()) } returns returnedId
                }
                val client = Client(internalClient, testApplicationMetadata)

                val receipt = OutgoingApplicationReceipt(UUID.randomUUID(), randomHerId(), StatusForMottakAvMelding.OK, emptyList())
                client.sendApplicationReceipt(receipt).asClue {
                    it shouldBe returnedId
                }

                coVerifySequence {
                    internalClient.postAppRec(receipt.acknowledgedId, any(), null)
                }

                requestSlot.captured.asClue { request ->
                    request.appRecStatus shouldBe NhnAppRecStatus.OK
                    request.appRecErrorList.shouldBeEmpty()
                    request.transportMetadataOverrides should beNull()
                }
            }

            "Should be able to send OK_FEIL_I_DELMELDING with errors" {
                val requestSlot = slot<NhnPostAppRecRequest>()
                val internalClient = mockk<MshInternalClient> { coEvery { postAppRec(any(), capture(requestSlot), any()) } returns UUID.randomUUID() }
                val client = Client(internalClient, testApplicationMetadata)

                val receipt = OutgoingApplicationReceipt(
                    acknowledgedId = UUID.randomUUID(),
                    senderHerId = randomHerId(),
                    status = StatusForMottakAvMelding.OK_FEIL_I_DELMELDING,
                    errors = List(nextInt(1, 5)) { randomOutgoingApplicationReceiptError() },
                )
                client.sendApplicationReceipt(receipt)

                coVerifySequence {
                    internalClient.postAppRec(receipt.acknowledgedId, any(), null)
                }

                requestSlot.captured.asClue { request ->
                    val requestAppRecErrorList = request.appRecErrorList ?: emptyList()
                    request.appRecStatus shouldBe NhnAppRecStatus.OK_ERROR_IN_MESSAGE_PART
                    requestAppRecErrorList shouldHaveSize receipt.errors.orEmpty().size
                    receipt.errors.orEmpty().forEach {
                        requestAppRecErrorList shouldContain NhnAppRecError().apply {
                            errorCode = it.type.verdi
                            description = it.type.navn
                            oid = it.type.kodeverk
                            details = it.details
                        }
                    }
                    request.transportMetadataOverrides should beNull()
                }
            }

            "Should be able to send AVVIST with errors" {
                val requestSlot = slot<NhnPostAppRecRequest>()
                val internalClient = mockk<MshInternalClient> { coEvery { postAppRec(any(), capture(requestSlot), any()) } returns UUID.randomUUID() }
                val client = Client(internalClient, testApplicationMetadata)

                val receipt = OutgoingApplicationReceipt(
                    acknowledgedId = UUID.randomUUID(),
                    senderHerId = randomHerId(),
                    status = StatusForMottakAvMelding.AVVIST,
                    errors = List(nextInt(1, 5)) { randomOutgoingApplicationReceiptError() },
                )
                client.sendApplicationReceipt(receipt)

                coVerifySequence {
                    internalClient.postAppRec(receipt.acknowledgedId, any(), null)
                }

                requestSlot.captured.asClue { request ->
                    val requestAppRecErrorList = request.appRecErrorList ?: emptyList()
                    request.appRecStatus shouldBe NhnAppRecStatus.REJECTED
                    requestAppRecErrorList shouldHaveSize receipt.errors.orEmpty().size
                    receipt.errors.orEmpty().forEach {
                        requestAppRecErrorList shouldContain NhnAppRecError().apply {
                            errorCode = it.type.verdi
                            description = it.type.navn
                            oid = it.type.kodeverk
                            details = it.details
                        }
                    }
                    request.transportMetadataOverrides should beNull()
                }
            }

            "Trying to send an OK receipt with errors should cause an exception to be thrown" {
                val client = Client(mockk<MshInternalClient> { coEvery { postAppRec(any(), any(), any()) } returns UUID.randomUUID() }, testApplicationMetadata)

                val receipt = OutgoingApplicationReceipt(
                    acknowledgedId = UUID.randomUUID(),
                    senderHerId = randomHerId(),
                    status = StatusForMottakAvMelding.OK,
                    errors = listOf(randomOutgoingApplicationReceiptError()),
                )

                shouldThrow<IllegalArgumentException> { client.sendApplicationReceipt(receipt) }.asClue {
                    it.message shouldBe "Error messages are not allowed when status is OK"
                }
            }

            "Trying to send an OK_FEIL_I_DELMELDING receipt without errors should cause an exception to be thrown" {
                val client = Client(mockk<MshInternalClient> { coEvery { postAppRec(any(), any(), any()) } returns UUID.randomUUID() }, testApplicationMetadata)

                val receipt = OutgoingApplicationReceipt(
                    acknowledgedId = UUID.randomUUID(),
                    senderHerId = randomHerId(),
                    status = StatusForMottakAvMelding.OK_FEIL_I_DELMELDING,
                    errors = emptyList(),
                )

                shouldThrow<IllegalArgumentException> { client.sendApplicationReceipt(receipt) }.asClue {
                    it.message shouldBe "Must provide at least one error message if status is not OK"
                }
            }

            "Trying to send an AVVIST receipt without errors should cause an exception to be thrown" {
                val client = Client(mockk<MshInternalClient> { coEvery { postAppRec(any(), any(), any()) } returns UUID.randomUUID() }, testApplicationMetadata)

                val receipt = OutgoingApplicationReceipt(
                    acknowledgedId = UUID.randomUUID(),
                    senderHerId = randomHerId(),
                    status = StatusForMottakAvMelding.AVVIST,
                    errors = emptyList(),
                )

                shouldThrow<IllegalArgumentException> { client.sendApplicationReceipt(receipt) }.asClue {
                    it.message shouldBe "Must provide at least one error message if status is not OK"
                }
            }

            "Should use child HER-id precedence and include v3 metadata for each dialog version" {
                val applicationMetadata = ApplicationMetadata("fiks-nhn-client-test-app", "7.8.9")

                listOf(DialogmeldingVersion.V1_0, DialogmeldingVersion.V1_1).forEach { version ->
                    val senderChildHerId = 101
                    val senderParentHerId = 102
                    val receiverChildHerId = 201
                    val receiverParentHerId = 202

                    val requestSlot = slot<NhnPostMessageRequest>()
                    val internalClient = mockk<MshInternalClient> {
                        coEvery { postMessage(capture(requestSlot), any()) } returns UUID.randomUUID()
                    }
                    val client = Client(internalClient, applicationMetadata)
                    val businessDocument = OutgoingBusinessDocument(
                        id = UUID.randomUUID(),
                        sender = Sender(
                            parent = OrganizationCommunicationParty(
                                ids = listOf(OrganizationId(senderParentHerId.toString(), OrganizationIdType.HER_ID)),
                                address = null,
                                name = "Parent sender",
                            ),
                            child = OrganizationCommunicationParty(
                                ids = listOf(OrganizationId(senderChildHerId.toString(), OrganizationIdType.HER_ID)),
                                address = null,
                                name = "Child sender",
                            ),
                        ),
                        receiver = Receiver(
                            parent = OrganizationCommunicationParty(
                                ids = listOf(OrganizationId(receiverParentHerId.toString(), OrganizationIdType.HER_ID)),
                                address = null,
                                name = "Parent receiver",
                            ),
                            child = PersonCommunicationParty(
                                ids = listOf(PersonId(receiverChildHerId.toString(), PersonIdType.HER_ID)),
                                address = null,
                                firstName = "Receiver",
                                middleName = null,
                                lastName = "Child",
                            ),
                            patient = Patient(
                                fnr = "01010112345",
                                firstName = "Patient",
                                middleName = null,
                                lastName = "Name",
                            ),
                        ),
                        message = OutgoingMessage(
                            subject = "Subject",
                            body = "Body",
                            responsibleHealthcareProfessional = HealthcareProfessional(
                                firstName = "Doc",
                                middleName = null,
                                lastName = "Doctor",
                                phoneNumber = "12345678",
                                roleToPatient = HelsepersonellsFunksjoner.BEHANDLINGSANSVARLIG_LEGE,
                            ),
                            recipientContact = RecipientContact(type = Helsepersonell.LEGE),
                        ),
                        vedlegg = emptyList(),
                        version = version,
                        conversationRef = null,
                    )

                    client.sendMessage(businessDocument)

                    requestSlot.captured.asClue { request ->
                        request.senderHerId shouldBe senderChildHerId
                        request.receiverHerIds shouldBe listOf(receiverChildHerId)
                        request.messageTypeIdentificator shouldBe when (version) {
                            DialogmeldingVersion.V1_0 -> MeldingensFunksjon.DIALOG_FORESPORSEL.verdi
                            DialogmeldingVersion.V1_1 -> MeldingensFunksjon.DIALOG_HELSEFAGLIG.verdi
                        }
                        request.applicationName shouldBe applicationMetadata.applicationName
                        request.applicationVersion shouldBe applicationMetadata.applicationVersion
                    }
                }
            }

            "Should fall back to parent HER-id when child HER-id is missing" {
                val applicationMetadata = ApplicationMetadata("fiks-nhn-client-test-app-fallback", "8.9.10")

                listOf(DialogmeldingVersion.V1_0, DialogmeldingVersion.V1_1).forEach { version ->
                    val senderParentHerId = 303
                    val receiverParentHerId = 404

                    val requestSlot = slot<NhnPostMessageRequest>()
                    val internalClient = mockk<MshInternalClient> {
                        coEvery { postMessage(capture(requestSlot), any()) } returns UUID.randomUUID()
                    }
                    val client = Client(internalClient, applicationMetadata)
                    val businessDocument = OutgoingBusinessDocument(
                        id = UUID.randomUUID(),
                        sender = Sender(
                            parent = OrganizationCommunicationParty(
                                ids = listOf(OrganizationId(senderParentHerId.toString(), OrganizationIdType.HER_ID)),
                                address = null,
                                name = "Fallback sender parent",
                            ),
                            child = OrganizationCommunicationParty(
                                ids = listOf(OrganizationId("1234567890", OrganizationIdType.ENH)),
                                address = null,
                                name = "Fallback sender child",
                            ),
                        ),
                        receiver = Receiver(
                            parent = OrganizationCommunicationParty(
                                ids = listOf(OrganizationId(receiverParentHerId.toString(), OrganizationIdType.HER_ID)),
                                address = null,
                                name = "Fallback receiver parent",
                            ),
                            child = PersonCommunicationParty(
                                ids = listOf(PersonId("01010112345", PersonIdType.FNR)),
                                address = null,
                                firstName = "Receiver",
                                middleName = null,
                                lastName = "Fallback",
                            ),
                            patient = Patient(
                                fnr = "01010112345",
                                firstName = "Patient",
                                middleName = null,
                                lastName = "Name",
                            ),
                        ),
                        message = OutgoingMessage(
                            subject = "Subject",
                            body = "Body",
                            responsibleHealthcareProfessional = HealthcareProfessional(
                                firstName = "Doc",
                                middleName = null,
                                lastName = "Doctor",
                                phoneNumber = "12345678",
                                roleToPatient = HelsepersonellsFunksjoner.BEHANDLINGSANSVARLIG_LEGE,
                            ),
                            recipientContact = RecipientContact(type = Helsepersonell.LEGE),
                        ),
                        vedlegg = emptyList(),
                        version = version,
                        conversationRef = null,
                    )

                    client.sendMessage(businessDocument)

                    requestSlot.captured.asClue { request ->
                        request.senderHerId shouldBe senderParentHerId
                        request.receiverHerIds shouldBe listOf(receiverParentHerId)
                        request.messageTypeIdentificator shouldBe when (version) {
                            DialogmeldingVersion.V1_0 -> MeldingensFunksjon.DIALOG_FORESPORSEL.verdi
                            DialogmeldingVersion.V1_1 -> MeldingensFunksjon.DIALOG_HELSEFAGLIG.verdi
                        }
                        request.applicationName shouldBe applicationMetadata.applicationName
                        request.applicationVersion shouldBe applicationMetadata.applicationVersion
                    }
                }
            }

            "Should reject missing sender HER-id" {
                val client = Client(
                    mockk<MshInternalClient> {
                        coEvery { postMessage(any(), any()) } returns UUID.randomUUID()
                    },
                    testApplicationMetadata,
                )
                val document = OutgoingBusinessDocument(
                    id = UUID.randomUUID(),
                    sender = Sender(
                        parent = OrganizationCommunicationParty(
                            ids = listOf(OrganizationId("123456789", OrganizationIdType.ENH)),
                            address = null,
                            name = "Sender parent",
                        ),
                        child = OrganizationCommunicationParty(
                            ids = listOf(OrganizationId("987654321", OrganizationIdType.ENH)),
                            address = null,
                            name = "Sender child",
                        ),
                    ),
                    receiver = Receiver(
                        parent = OrganizationCommunicationParty(
                            ids = listOf(OrganizationId("5555", OrganizationIdType.HER_ID)),
                            address = null,
                            name = "Receiver parent",
                        ),
                        child = PersonCommunicationParty(
                            ids = listOf(PersonId("01010112345", PersonIdType.FNR)),
                            address = null,
                            firstName = "Receiver",
                            middleName = null,
                            lastName = "Child",
                        ),
                        patient = Patient(
                            fnr = "01010112345",
                            firstName = "Patient",
                            middleName = null,
                            lastName = "Name",
                        ),
                    ),
                    message = OutgoingMessage(
                        subject = "Subject",
                        body = "Body",
                        responsibleHealthcareProfessional = HealthcareProfessional(
                            firstName = "Doc",
                            middleName = null,
                            lastName = "Doctor",
                            phoneNumber = "12345678",
                            roleToPatient = HelsepersonellsFunksjoner.BEHANDLINGSANSVARLIG_LEGE,
                        ),
                        recipientContact = RecipientContact(type = Helsepersonell.LEGE),
                    ),
                    vedlegg = emptyList(),
                    version = DialogmeldingVersion.V1_0,
                    conversationRef = null,
                )

                shouldThrow<IllegalArgumentException> { client.sendMessage(document) }.asClue {
                    it.message shouldBe "Outgoing business document is missing sender HER-id"
                }
            }

            "Should reject missing receiver HER-id" {
                val client = Client(
                    mockk<MshInternalClient> {
                        coEvery { postMessage(any(), any()) } returns UUID.randomUUID()
                    },
                    testApplicationMetadata,
                )
                val document = OutgoingBusinessDocument(
                    id = UUID.randomUUID(),
                    sender = Sender(
                        parent = OrganizationCommunicationParty(
                            ids = listOf(OrganizationId("9999", OrganizationIdType.HER_ID)),
                            address = null,
                            name = "Sender parent",
                        ),
                        child = OrganizationCommunicationParty(
                            ids = listOf(OrganizationId("1001", OrganizationIdType.HER_ID)),
                            address = null,
                            name = "Sender child",
                        ),
                    ),
                    receiver = Receiver(
                        parent = OrganizationCommunicationParty(
                            ids = listOf(OrganizationId("555555555", OrganizationIdType.ENH)),
                            address = null,
                            name = "Receiver parent",
                        ),
                        child = PersonCommunicationParty(
                            ids = listOf(PersonId("01010112345", PersonIdType.FNR)),
                            address = null,
                            firstName = "Receiver",
                            middleName = null,
                            lastName = "Child",
                        ),
                        patient = Patient(
                            fnr = "01010112345",
                            firstName = "Patient",
                            middleName = null,
                            lastName = "Name",
                        ),
                    ),
                    message = OutgoingMessage(
                        subject = "Subject",
                        body = "Body",
                        responsibleHealthcareProfessional = HealthcareProfessional(
                            firstName = "Doc",
                            middleName = null,
                            lastName = "Doctor",
                            phoneNumber = "12345678",
                            roleToPatient = HelsepersonellsFunksjoner.BEHANDLINGSANSVARLIG_LEGE,
                        ),
                        recipientContact = RecipientContact(type = Helsepersonell.LEGE),
                    ),
                    vedlegg = emptyList(),
                    version = DialogmeldingVersion.V1_1,
                    conversationRef = null,
                )

                shouldThrow<IllegalArgumentException> { client.sendMessage(document) }.asClue {
                    it.message shouldBe "Outgoing business document is missing receiver HER-id"
                }
            }

            "Params should be passed on" {
                val params = RequestParameters(HelseIdTokenParameters(SingleTenantHelseIdTokenParameters(randomHerId().toString())))

                val internalClient = mockk<MshInternalClient> {
                    coEvery { postAppRec(any(), any(), any()) } returns UUID.randomUUID()
                }
                val client = Client(internalClient, testApplicationMetadata)

                client.sendApplicationReceipt(
                    OutgoingApplicationReceipt(
                        acknowledgedId = UUID.randomUUID(),
                        senderHerId = randomHerId(),
                        status = StatusForMottakAvMelding.OK,
                        errors = emptyList(),
                    ), params
                )
                coVerifySequence {
                    internalClient.postAppRec(any(), any(), params)
                }
            }
        }

        "Get message" - {
            "Should be retrieved and mapped" {
                val message = randomApiMessageWithMetadata()

                val internalClient = mockk<MshInternalClient> {
                    coEvery { getMessage(any(), any()) } returns message
                }
                val client = Client(internalClient, testApplicationMetadata)

                val messageId = UUID.randomUUID()
                client.getMessage(messageId).asClue {
                    it.id shouldBe UUID.fromString(message.id)
                    it.contentType shouldBe message.contentType
                    it.receiverHerIds shouldBe message.receiverHerIds?.toList()
                    it.senderHerId shouldBe message.senderHerId
                    it.businessDocumentId shouldBe message.businessDocumentId
                    it.businessDocumentDate shouldBe message.businessDocumentGenDate?.let { OffsetDateTime.parse(it) }
                    it.businessDocumentMsgType shouldBe message.businessDocumentMsgType
                }

                coVerifySequence {
                    internalClient.getMessage(messageId)
                }
            }

            "Should map message with only id when metadata is unavailable" {
                val message = NhnGetMessageResponse().apply {
                    id = UUID.randomUUID().toString()
                }

                val internalClient = mockk<MshInternalClient> {
                    coEvery { getMessage(any(), any()) } returns message
                }
                val client = Client(internalClient, testApplicationMetadata)

                client.getMessage(UUID.randomUUID()).asClue {
                    it.id shouldBe UUID.fromString(message.id)
                    it.contentType should beNull()
                    it.receiverHerIds should beNull()
                    it.senderHerId should beNull()
                    it.businessDocumentId should beNull()
                    it.businessDocumentDate should beNull()
                    it.businessDocumentMsgType should beNull()
                }
            }

            "Params should be passed on" {
                val params = RequestParameters(HelseIdTokenParameters(SingleTenantHelseIdTokenParameters(randomHerId().toString())))

                val internalClient = mockk<MshInternalClient> {
                    coEvery { getMessage(any(), any()) } returns randomApiMessageWithMetadata()
                }
                val client = Client(internalClient, testApplicationMetadata)

                client.getMessage(UUID.randomUUID(), params)
                coVerifySequence {
                    internalClient.getMessage(any(), params)
                }
            }
        }

        "Get business document" - {
            "Only Base64 encoding is supported" {
                val response = NhnGetBusinessDocumentResponse().apply {
                    businessDocument = UUID.randomUUID().toString()
                    contentType = UUID.randomUUID().toString()
                    contentTransferEncoding = UUID.randomUUID().toString()
                }

                val internalClient = mockk<MshInternalClient> {
                    coEvery { getBusinessDocument(any(), any()) } returns response
                }
                val client = Client(internalClient, testApplicationMetadata)

                val id = UUID.randomUUID()
                shouldThrow<IllegalArgumentException> { client.getBusinessDocument(id) }.asClue {
                    it.message shouldBe "'${response.contentTransferEncoding}' is not a supported transfer encoding"
                }
            }

            "Only XML is supported" {
                val response = NhnGetBusinessDocumentResponse().apply {
                    businessDocument = UUID.randomUUID().toString()
                    contentType = UUID.randomUUID().toString()
                    contentTransferEncoding = "base64"
                }

                val internalClient = mockk<MshInternalClient> {
                    coEvery { getBusinessDocument(any(), any()) } returns response
                }
                val client = Client(internalClient, testApplicationMetadata)

                val id = UUID.randomUUID()
                shouldThrow<IllegalArgumentException> { client.getBusinessDocument(id) }.asClue {
                    it.message shouldBe "'${response.contentType}' is not a supported content type"
                }
            }

            "When encoding and type is correct, the document should be retrieved, deserialized and mapped" {
                val response = NhnGetBusinessDocumentResponse().apply {
                    businessDocument = Base64.getEncoder().encodeToString(readResourceContent("dialogmelding/1.0/foresporsel-og-svar/dialog-foresporsel-samsvar-test.xml"))
                    contentType = "application/xml"
                    contentTransferEncoding = "base64"
                }

                val internalClient = mockk<MshInternalClient> {
                    coEvery { getBusinessDocument(any(), any()) } returns response
                }
                val client = Client(internalClient, testApplicationMetadata)

                val id = UUID.randomUUID()
                client.getBusinessDocument(id).asClue {
                    it.id shouldBe "6ddb98ed-9e34-4efa-9163-62e4ea0cbf43"
                }

                coVerifySequence {
                    internalClient.getBusinessDocument(id)
                }
            }

            "If the returned message is of wrong type, an exception should be thrown" {
                val response = NhnGetBusinessDocumentResponse().apply {
                    businessDocument = Base64.getEncoder().encodeToString(readResourceContent("app-rec/1.0/all-data.xml"))
                    contentType = "application/xml"
                    contentTransferEncoding = "base64"
                }

                val internalClient = mockk<MshInternalClient> {
                    coEvery { getBusinessDocument(any(), any()) } returns response
                }
                val client = Client(internalClient, testApplicationMetadata)

                val id = UUID.randomUUID()
                shouldThrow<IllegalArgumentException> { client.getBusinessDocument(id) }.asClue {
                    it.message shouldBe "Expected MsgHead as root element, but found AppRec"
                }

                coVerifySequence {
                    internalClient.getBusinessDocument(id)
                }
            }

            "Params should be passed on" {
                val params = RequestParameters(HelseIdTokenParameters(SingleTenantHelseIdTokenParameters(randomHerId().toString())))

                val internalClient = mockk<MshInternalClient> {
                    coEvery { getBusinessDocument(any(), any()) } returns NhnGetBusinessDocumentResponse().apply {
                        businessDocument = Base64.getEncoder().encodeToString(readResourceContent("dialogmelding/1.0/foresporsel-og-svar/dialog-foresporsel-samsvar-test.xml"))
                        contentType = "application/xml"
                        contentTransferEncoding = "base64"
                    }
                }
                val client = Client(internalClient, testApplicationMetadata)

                client.getBusinessDocument(UUID.randomUUID(), params)
                coVerifySequence {
                    internalClient.getBusinessDocument(any(), params)
                }
            }
        }

        "Get application receipt" - {
            "Only Base64 encoding is supported" {
                val response = NhnGetBusinessDocumentResponse().apply {
                    businessDocument = UUID.randomUUID().toString()
                    contentType = UUID.randomUUID().toString()
                    contentTransferEncoding = UUID.randomUUID().toString()
                }

                val internalClient = mockk<MshInternalClient> {
                    coEvery { getBusinessDocument(any(), any()) } returns response
                }
                val client = Client(internalClient, testApplicationMetadata)

                val id = UUID.randomUUID()
                shouldThrow<IllegalArgumentException> { client.getApplicationReceipt(id) }.asClue {
                    it.message shouldBe "'${response.contentTransferEncoding}' is not a supported transfer encoding"
                }
            }

            "Only XML is supported" {
                val response = NhnGetBusinessDocumentResponse().apply {
                    businessDocument = UUID.randomUUID().toString()
                    contentType = UUID.randomUUID().toString()
                    contentTransferEncoding = "base64"
                }

                val internalClient = mockk<MshInternalClient> {
                    coEvery { getBusinessDocument(any(), any()) } returns response
                }
                val client = Client(internalClient, testApplicationMetadata)

                val id = UUID.randomUUID()
                shouldThrow<IllegalArgumentException> { client.getApplicationReceipt(id) }.asClue {
                    it.message shouldBe "'${response.contentType}' is not a supported content type"
                }
            }

            "When encoding and type is correct, the document should be retrieved, deserialized and mapped" {
                val response = NhnGetBusinessDocumentResponse().apply {
                    businessDocument = Base64.getEncoder().encodeToString(readResourceContent("app-rec/1.0/all-data.xml"))
                    contentType = "application/xml"
                    contentTransferEncoding = "base64"
                }

                val internalClient = mockk<MshInternalClient> {
                    coEvery { getBusinessDocument(any(), any()) } returns response
                }
                val client = Client(internalClient, testApplicationMetadata)

                val id = UUID.randomUUID()
                client.getApplicationReceipt(id).asClue {
                    it.id shouldBe "a761e9b9-3495-4f5a-a964-c13d544d2ceb"
                }

                coVerifySequence {
                    internalClient.getBusinessDocument(id)
                }
            }

            "If the returned message is of wrong type, an exception should be thrown" {
                val response = NhnGetBusinessDocumentResponse().apply {
                    businessDocument = Base64.getEncoder().encodeToString(readResourceContent("dialogmelding/1.0/foresporsel-og-svar/dialog-foresporsel-samsvar-test.xml"))
                    contentType = "application/xml"
                    contentTransferEncoding = "base64"
                }
                val internalClient = mockk<MshInternalClient> {
                    coEvery { getBusinessDocument(any(), any()) } returns response
                }
                val client = Client(internalClient, testApplicationMetadata)

                val id = UUID.randomUUID()
                shouldThrow<IllegalArgumentException> { client.getApplicationReceipt(id) }.asClue {
                    it.message shouldBe "Expected AppRec as root element, but found MsgHead"
                }

                coVerifySequence {
                    internalClient.getBusinessDocument(id)
                }
            }

            "Params should be passed on" {
                val params = RequestParameters(HelseIdTokenParameters(SingleTenantHelseIdTokenParameters(randomHerId().toString())))

                val internalClient = mockk<MshInternalClient> {
                    coEvery { getBusinessDocument(any(), any()) } returns NhnGetBusinessDocumentResponse().apply {
                        businessDocument = Base64.getEncoder().encodeToString(readResourceContent("app-rec/1.0/all-data.xml"))
                        contentType = "application/xml"
                        contentTransferEncoding = "base64"
                    }
                }
                val client = Client(internalClient, testApplicationMetadata)

                client.getApplicationReceipt(UUID.randomUUID(), params)
                coVerifySequence {
                    internalClient.getBusinessDocument(any(), params)
                }
            }
        }

        "Get application receipts for message" - {
            "Should be retrieved and mapped" {
                val statusInfo = NhnStatusInfo().apply {
                    receiverHerId = randomHerId()
                    apprecInfo = NhnApprecInfo().apply {
                        appRecStatus = NhnAppRecStatus.OK
                        appRecErrorList = emptyList()
                    }
                }

                val internalClient = mockk<MshInternalClient> {
                    coEvery { getStatus(any(), any()) } returns listOf(statusInfo)
                }
                val client = Client(internalClient, testApplicationMetadata)

                val messageId = UUID.randomUUID()
                client.getApplicationReceiptsForMessage(messageId).asClue {
                    it shouldHaveSize 1
                    with(it.single()) {
                        receiverHerId shouldBe statusInfo.receiverHerId
                        status shouldBe StatusForMottakAvMelding.OK
                        errors.shouldBeEmpty()
                    }
                }

                coVerifySequence {
                    internalClient.getStatus(messageId, null)
                }
            }

            "Should handle rejected with error" {
                val statusInfo = NhnStatusInfo().apply {
                    receiverHerId = randomHerId()
                    apprecInfo = NhnApprecInfo().apply {
                        appRecStatus = NhnAppRecStatus.REJECTED
                        appRecErrorList = listOf(
                            NhnAppRecError().apply {
                                errorCode = "E10"
                                description = "Ugyldig meldingsidentifikator"
                                oid = "2.16.578.1.12.4.1.1.8221"
                                details = "Extra details about error"
                            }
                        )
                    }
                }

                val client = Client(mockk<MshInternalClient> {
                    coEvery { getStatus(any(), any()) } returns listOf(statusInfo)
                }, testApplicationMetadata)

                client.getApplicationReceiptsForMessage(UUID.randomUUID()).asClue {
                    it shouldHaveSize 1
                    with(it.single()) {
                        receiverHerId shouldBe statusInfo.receiverHerId
                        status shouldBe StatusForMottakAvMelding.AVVIST
                        errors shouldHaveSize 1
                        with(errors.single()) {
                            type shouldBe FeilmeldingForApplikasjonskvittering.UGYLDIG_ID
                            errorCode shouldBe "E10"
                            description shouldBe "Ugyldig meldingsidentifikator"
                            oid shouldBe "2.16.578.1.12.4.1.1.8221"
                            details shouldBe "Extra details about error"
                        }
                    }
                }
            }

            "Should handle ok error in message with errors" {
                val statusInfo = NhnStatusInfo().apply {
                    receiverHerId = randomHerId()
                    apprecInfo = NhnApprecInfo().apply {
                        appRecStatus = NhnAppRecStatus.OK_ERROR_IN_MESSAGE_PART
                        appRecErrorList = listOf(
                            NhnAppRecError().apply {
                                errorCode = "E32"
                                description = "Pasientens navn mangler"
                                oid = "2.16.578.1.12.4.1.1.8221"
                                details = "Noe gikk galt"
                            },
                            NhnAppRecError().apply {
                                errorCode = "T02"
                                description = "XML validerer ikke"
                                oid = "2.16.578.1.12.4.1.1.8221"
                                details = "Feil i XML"
                            }
                        )
                    }
                }

                val client = Client(mockk<MshInternalClient> {
                    coEvery { getStatus(any(), any()) } returns listOf(statusInfo)
                }, testApplicationMetadata)

                client.getApplicationReceiptsForMessage(UUID.randomUUID()).asClue {
                    it shouldHaveSize 1
                    with(it.single()) {
                        receiverHerId shouldBe statusInfo.receiverHerId
                        status shouldBe StatusForMottakAvMelding.OK_FEIL_I_DELMELDING
                        errors shouldHaveSize 2
                        with(errors[0]) {
                            type shouldBe FeilmeldingForApplikasjonskvittering.PASIENT_MANGLER_NAVN
                            errorCode shouldBe "E32"
                            description shouldBe "Pasientens navn mangler"
                            oid shouldBe "2.16.578.1.12.4.1.1.8221"
                            details shouldBe "Noe gikk galt"
                        }
                        with(errors[1]) {
                            type shouldBe FeilmeldingForApplikasjonskvittering.VALIDERINGSFEIL_XML
                            errorCode shouldBe "T02"
                            description shouldBe "XML validerer ikke"
                            oid shouldBe "2.16.578.1.12.4.1.1.8221"
                            details shouldBe "Feil i XML"
                        }
                    }
                }
            }

            "Missing status should map to null" {
                val statusInfo = NhnStatusInfo().apply {
                    receiverHerId = randomHerId()
                    apprecInfo = null
                }

                val client = Client(mockk<MshInternalClient> {
                    coEvery { getStatus(any(), any()) } returns listOf(statusInfo)
                }, testApplicationMetadata)

                val messageId = UUID.randomUUID()
                client.getApplicationReceiptsForMessage(messageId).asClue {
                    it shouldHaveSize 1
                    with(it.single()) {
                        receiverHerId shouldBe statusInfo.receiverHerId
                        status should beNull()
                        errors.shouldBeEmpty()
                    }
                }
            }

            "Unknown error code should be mapped to UKJENT" {
                val statusInfo = NhnStatusInfo().apply {
                    receiverHerId = randomHerId()
                    apprecInfo = NhnApprecInfo().apply {
                        appRecStatus = NhnAppRecStatus.REJECTED
                        appRecErrorList = listOf(
                            NhnAppRecError().apply {
                                errorCode = "A123"
                                description = "Kan ikke håndteres"
                                oid = "1.2.3.4.5.6.7.8.9"
                                details = "Error X"
                            },
                        )
                    }
                }

                val client = Client(mockk<MshInternalClient> {
                    coEvery { getStatus(any(), any()) } returns listOf(statusInfo)
                }, testApplicationMetadata)

                val messageId = UUID.randomUUID()
                client.getApplicationReceiptsForMessage(messageId).asClue {
                    it shouldHaveSize 1
                    with(it.single()) {
                        receiverHerId shouldBe statusInfo.receiverHerId
                        status shouldBe StatusForMottakAvMelding.AVVIST
                        errors shouldHaveSize 1
                        with(errors.single()) {
                            type shouldBe FeilmeldingForApplikasjonskvittering.UKJENT
                            errorCode shouldBe "A123"
                            description shouldBe "Kan ikke håndteres"
                            oid shouldBe "1.2.3.4.5.6.7.8.9"
                            details shouldBe "Error X"
                        }
                    }
                }
            }

            "Null errors should be mapped to empty list" {
                val statusInfo = NhnStatusInfo().apply {
                    receiverHerId = randomHerId()
                    apprecInfo = NhnApprecInfo().apply {
                        appRecStatus = NhnAppRecStatus.REJECTED
                        appRecErrorList = null
                    }
                }

                val client = Client(mockk<MshInternalClient> {
                    coEvery { getStatus(any(), any()) } returns listOf(statusInfo)
                }, testApplicationMetadata)

                val messageId = UUID.randomUUID()
                client.getApplicationReceiptsForMessage(messageId).asClue {
                    it shouldHaveSize 1
                    with(it.single()) {
                        receiverHerId shouldBe statusInfo.receiverHerId
                        status shouldBe StatusForMottakAvMelding.AVVIST
                        errors shouldNot beNull()
                        errors.shouldBeEmpty()
                    }
                }
            }
        }

        "Get unread notifications" - {
            "Unread notifications should be retrieved and mapped" {
                val apiNotifications = List(nextInt(1, 5)) { randomApiUnreadNotification() }

                val internalClient = mockk<MshInternalClient> {
                    coEvery { getUnreadNotifications(any(), any(), any()) } returns apiNotifications
                }
                val client = Client(internalClient, testApplicationMetadata)

                val herIds = listOf(randomHerId(), randomHerId())
                val notificationsToFetch = 10
                client.getUnreadNotifications(herIds, notificationsToFetch).asClue {
                    it.forEachIndexed { i, notification ->
                        notification.notificationId shouldBe UUID.fromString(apiNotifications[i].notificationId)
                        notification.relatedMessageId shouldBe apiNotifications[i].relatedMessageId?.let { UUID.fromString(it) }
                        notification.type.name shouldBe apiNotifications[i].type.value
                        notification.notificationReceiverHerId shouldBe apiNotifications[i].notificationReceiverHerId
                        notification.notificationTriggeredByHerId shouldBe apiNotifications[i].notificationTriggeredByHerId
                        notification.description shouldBe apiNotifications[i].description
                        notification.createdAt shouldBe apiNotifications[i].createdAt
                    }
                }

                coVerifySequence {
                    internalClient.getUnreadNotifications(herIds, notificationsToFetch, null)
                }
            }

            "Notification without createdAt should be mapped with null timestamp without failing the batch" {
                val withoutTimestamp = no.nhn.msh.v3.model.UnreadNotification().apply {
                    notificationId = UUID.randomUUID().toString()
                    type = no.nhn.msh.v3.model.NotificationType.NEW_MESSAGE
                    notificationReceiverHerId = randomHerId()
                }
                val withTimestamp = randomApiUnreadNotification()

                val internalClient = mockk<MshInternalClient> {
                    coEvery { getUnreadNotifications(any(), any(), any()) } returns listOf(withoutTimestamp, withTimestamp)
                }
                val client = Client(internalClient, testApplicationMetadata)

                client.getUnreadNotifications(listOf(randomHerId())).asClue {
                    it shouldHaveSize 2
                    with(it[0]) {
                        notificationId shouldBe UUID.fromString(withoutTimestamp.notificationId)
                        type shouldBe NotificationType.NewMessage
                        notificationReceiverHerId shouldBe withoutTimestamp.notificationReceiverHerId
                        relatedMessageId should beNull()
                        notificationTriggeredByHerId should beNull()
                        description should beNull()
                        createdAt should beNull()
                    }
                    it[1].createdAt shouldBe withTimestamp.createdAt
                }
            }

            "Params should be passed on" {
                val params = RequestParameters(HelseIdTokenParameters(SingleTenantHelseIdTokenParameters(randomHerId().toString())))

                val internalClient = mockk<MshInternalClient> {
                    coEvery { getUnreadNotifications(any(), any(), any()) } returns emptyList()
                }
                val client = Client(internalClient, testApplicationMetadata)

                client.getUnreadNotifications(listOf(randomHerId()), 10, params)
                coVerifySequence {
                    internalClient.getUnreadNotifications(any(), any(), params)
                }
            }
        }

        "Delete notifications" - {
            "Notification ids should be passed on" {
                val notificationIds = listOf(UUID.randomUUID(), UUID.randomUUID())

                val internalClient = mockk<MshInternalClient> {
                    coEvery { deleteNotifications(any(), any()) } just runs
                }
                val client = Client(internalClient, testApplicationMetadata)

                client.deleteNotifications(notificationIds)

                coVerifySequence {
                    internalClient.deleteNotifications(notificationIds, null)
                }
            }

            "Params should be passed on" {
                val params = RequestParameters(HelseIdTokenParameters(SingleTenantHelseIdTokenParameters(randomHerId().toString())))

                val internalClient = mockk<MshInternalClient> {
                    coEvery { deleteNotifications(any(), any()) } just runs
                }
                val client = Client(internalClient, testApplicationMetadata)

                client.deleteNotifications(listOf(UUID.randomUUID()), params)
                coVerifySequence {
                    internalClient.deleteNotifications(any(), params)
                }
            }
        }

        "Set MSH configurations" - {
            "Configurations should be passed on to the internal client" {
                val configurations = List(nextInt(1, 5)) { randomMshConfiguration() }
                val generatedConfigurations = configurations.map { toNhnMshConfiguration(it) }

                val internalClient = mockk<MshInternalClient> {
                    coEvery { setMshConfigurations(any(), any()) } just runs
                }
                val client = Client(internalClient, testApplicationMetadata)

                client.setMshConfigurations(configurations)

                coVerifySequence {
                    internalClient.setMshConfigurations(generatedConfigurations, null)
                }
            }

            "Params should be passed on" {
                val params = RequestParameters(HelseIdTokenParameters(SingleTenantHelseIdTokenParameters(randomHerId().toString())))

                val internalClient = mockk<MshInternalClient> {
                    coEvery { setMshConfigurations(any(), any()) } just runs
                }
                val client = Client(internalClient, testApplicationMetadata)

                client.setMshConfigurations(listOf(randomMshConfiguration()), params)
                coVerifySequence {
                    internalClient.setMshConfigurations(any(), params)
                }
            }
        }

        "Delete MSH configurations" - {
            "HerIds should be passed on to the internal client" {
                val herIds = listOf(randomHerId(), randomHerId())

                val internalClient = mockk<MshInternalClient> {
                    coEvery { deleteMshConfigurations(any(), any()) } just runs
                }
                val client = Client(internalClient, testApplicationMetadata)

                client.deleteMshConfigurations(herIds)

                coVerifySequence {
                    internalClient.deleteMshConfigurations(herIds, null)
                }
            }

            "Params should be passed on" {
                val params = RequestParameters(HelseIdTokenParameters(SingleTenantHelseIdTokenParameters(randomHerId().toString())))

                val internalClient = mockk<MshInternalClient> {
                    coEvery { deleteMshConfigurations(any(), any()) } just runs
                }
                val client = Client(internalClient, testApplicationMetadata)

                client.deleteMshConfigurations(listOf(randomHerId()), params)
                coVerifySequence {
                    internalClient.deleteMshConfigurations(any(), params)
                }
            }
        }

        "Mark message downloaded" - {
            "The data should be serialized to XML and passed on to the service" {
                val internalClient = mockk<MshInternalClient> {
                    coEvery { markMessageDownloaded(any(), any(), any()) } just runs
                }
                val client = Client(internalClient, testApplicationMetadata)

                val id = UUID.randomUUID()
                val receiverHerId = randomHerId()
                client.markMessageDownloaded(id, receiverHerId)

                coVerifySequence {
                    internalClient.markMessageDownloaded(id, receiverHerId)
                }
            }

            "Params should be passed on" {
                val params = RequestParameters(HelseIdTokenParameters(SingleTenantHelseIdTokenParameters(randomHerId().toString())))

                val internalClient = mockk<MshInternalClient> {
                    coEvery { markMessageDownloaded(any(), any(), any()) } just runs
                }
                val client = Client(internalClient, testApplicationMetadata)

                client.markMessageDownloaded(UUID.randomUUID(), randomHerId(), params)
                coVerifySequence {
                    internalClient.markMessageDownloaded(any(), any(), params)
                }
            }
        }

        "Get status" - {
            "Should be able to get status for message" {
                val statuses = List(nextInt(0, 10)) { randomStatusInfo() }

                val internalClient = mockk<MshInternalClient> {
                    coEvery { getStatus(any(), any()) } returns statuses
                }
                val client = Client(internalClient, testApplicationMetadata)

                val id = UUID.randomUUID()
                client.getStatus(id).asClue {
                    it.forEachIndexed { i, status ->
                        status.receiverHerId shouldBe statuses[i].receiverHerId
                        status.deliveryState.name shouldBe statuses[i].transportDeliveryState.name
                        status.appRecStatus?.name shouldBe statuses[i].apprecInfo?.appRecStatus?.name
                    }
                }

                coVerifySequence {
                    internalClient.getStatus(id, null)
                }
            }

            "Params should be passed on" {
                val params = RequestParameters(HelseIdTokenParameters(SingleTenantHelseIdTokenParameters(randomHerId().toString())))

                val internalClient = mockk<MshInternalClient> {
                    coEvery { getStatus(any(), any()) } returns emptyList()
                }
                val client = Client(internalClient, testApplicationMetadata)

                client.getStatus(UUID.randomUUID(), params)
                coVerifySequence {
                    internalClient.getStatus(any(), params)
                }
            }
        }

    }

}

private fun randomOutgoingBusinessDocument(
    vedleggBytes: ByteArray = nextBytes(nextInt(1000, 100000)),
): OutgoingBusinessDocument = OutgoingBusinessDocument(
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
    receiver = Receiver(
        parent = OrganizationCommunicationParty(
            ids = listOf(randomOrganizationHerId()),
            address = randomAddress(),
            name = randomString(),
        ),
        child = PersonCommunicationParty(
            ids = listOf(randomPersonHerId()),
            address = randomAddress(),
            firstName = randomString(),
            middleName = randomString(),
            lastName = randomString(),
        ),
        patient = Patient(
            fnr = randomString(),
            firstName = randomString(),
            middleName = randomString(),
            lastName = randomString(),
        )
    ),
    message = OutgoingMessage(
        subject = randomString(),
        body = randomString(),
        responsibleHealthcareProfessional = HealthcareProfessional(
            firstName = randomString(),
            middleName = randomString(),
            lastName = randomString(),
            phoneNumber = randomString(),
            roleToPatient = HelsepersonellsFunksjoner.BEHANDLINGSANSVARLIG_LEGE,
        ),
        recipientContact = RecipientContact(
            type = Helsepersonell.entries.random(),
        ),
    ),
    vedlegg = listOf(
        OutgoingVedlegg(
            date = OffsetDateTime.now(),
            description = randomString(),
            data = ByteArrayInputStream(vedleggBytes),
        )
    ),
    version = DialogmeldingVersion.entries.random(),
    conversationRef = ConversationRef(
        refToParent = UUID.randomUUID().toString(),
        refToConversation = UUID.randomUUID().toString(),
    )
)

private fun randomApiMessageWithMetadata() = NhnGetMessageResponse().apply {
    id = UUID.randomUUID().toString()
    contentType = UUID.randomUUID().toString()
    receiverHerIds = setOf(randomHerId())
    senderHerId = randomHerId()
    businessDocumentId = UUID.randomUUID().toString()
    businessDocumentGenDate = OffsetDateTime.now().toString()
    businessDocumentMsgType = nextBoolean().let { if (it) "APPREC" else null }
}

private fun randomOutgoingApplicationReceiptError() =
    FeilmeldingForApplikasjonskvittering.entries.minus(FeilmeldingForApplikasjonskvittering.UKJENT).random().let { type ->
        OutgoingApplicationReceiptError(
            type = type,
            details = UUID.randomUUID().toString(),
        )
    }

private fun randomStatusInfo() = NhnStatusInfo().apply {
    receiverHerId = randomHerId()
    sent = nextBoolean()
    transportDeliveryState = NhnDeliveryState.entries.random()
    apprecInfo = NhnApprecInfo().apply {
        appRecStatus = NhnAppRecStatus.entries.plus(null).random()
    }
}

private fun randomMshConfiguration() = ClientMshConfiguration(
    herId = randomHerId(),
    receiveNotificationChannel = ReceiveNotificationChannel.entries.random(),
    clientLocked = nextBoolean(),
    rejectMessageFilters = if (nextBoolean()) {
        ClientRejectMessageFilters(messageFunctions = listOf(UUID.randomUUID().toString()))
    } else {
        null
    },
)

private fun toNhnMshConfiguration(configuration: no.ks.fiks.nhn.msh.MshConfiguration): no.nhn.msh.v3.model.MshConfiguration = no.nhn.msh.v3.model.MshConfiguration()
    .herId(configuration.herId)
    .receiveNotificationChannel(
        when (configuration.receiveNotificationChannel) {
            ReceiveNotificationChannel.Api -> no.nhn.msh.v3.model.ReceiveNotificationChannel.API
            ReceiveNotificationChannel.Kafka -> no.nhn.msh.v3.model.ReceiveNotificationChannel.KAFKA
        }
    )
    .clientLocked(configuration.clientLocked)
    .rejectMessageFilters(
        configuration.rejectMessageFilters?.let { filters ->
            no.nhn.msh.v3.model.MshConfigurationRejectMessageFilters().messageFunction(filters.messageFunctions)
        }
    )

private fun randomApiUnreadNotification() = no.nhn.msh.v3.model.UnreadNotification().apply {
    notificationId = UUID.randomUUID().toString()
    relatedMessageId = UUID.randomUUID().toString().takeIf { nextBoolean() }
    type = no.nhn.msh.v3.model.NotificationType.entries.random()
    notificationReceiverHerId = randomHerId()
    notificationTriggeredByHerId = randomHerId().takeIf { nextBoolean() }
    description = UUID.randomUUID().toString().takeIf { nextBoolean() }
    createdAt = OffsetDateTime.now()
}

