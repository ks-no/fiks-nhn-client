# NHN Client

Uses the Meldingstjener REST API provided by NHN:
- [Documentation](https://utviklerportal.nhn.no/informasjonstjenester/meldingstjener/edi-20/edi-20-ekstern-docs/docs/meldingstjener-rest-apimd)
- [API spec](https://api.tjener.test.melding.nhn.no/swagger/index.html)

### Import using Maven
```xml
<dependency>
    <groupId>no.ks.fiks</groupId>
    <artifactId>msh-client</artifactId>
    <version>X.X.X</version>
</dependency>
```

### Setting up the client
```kotlin
val client = Client(
    Configuration(
        environments = Environments.TEST,
        sourceSystem = "<Name describing the system using the client>",
        helseId = HelseIdConfiguration(
            clientId = "<helse-id-client-id>",
            jwk = "<helse-id-jwk-string>",
        ),
        fastlegeregisteret = Credentials(
            username = "<flr-username>",
            password = "<flr-password>>",
        ),
        adresseregisteret = Credentials(
            username = "<ar-username>>",
            password = "<ar-password>",
        ),
    )
)
```

### Sending messages

#### Send message to GP of person

```kotlin
client.sendMessageToGPForPerson(
    GPForPersonOutgoingBusinessDocument(
        id = UUID.randomUUID(), // This id will be used to connect an application receipt to this message
        sender = Organization(
            name = "<Name of sender organization (virksomhet)>",
            id = Id(
                id = "<Sender organization HER-id>",
                type = OrganisasjonIdType.HER_ID,
            ),
            childOrganization = Organization(
                name = "<Name of the sending service (tjeneste), which is owned by the organization specified above>",
                id = Id(
                    id = "<The HER-id of this service>",
                    type = OrganisasjonIdType.HER_ID,
                ),
            )
        ),
        person = Person(
            fnr = "<Id of the patient the message is about (fødselsnummer, etc.)>",
            firstName = "<First name>",
            middleName = "<Middle name>",
            lastName = "<Last name>",
        ),
        message = BusinessDocumentMessage(
            subject = "<Message subject>",
            body = "<Message body>",
            responsibleHealthcareProfessional = HealthcareProfessional(
                // Person responsible for this message at the sender
                firstName = "<First name>",
                middleName = "<Middle name>",
                lastName = "<Last name>",
                phoneNumber = "11223344",
                roleToPatient = HelsepersonellsFunksjoner.HELSEFAGLIG_KONTAKT, // This persons role with respect to the patient
            ),
            recipientContact = RecipientContact(
                // Required for Dialogmelding 1.1 Helsefaglig Dialog when temaKodet is "Henvendelse om pasient" (which is currently the only option and is chosen automatically)
                type = Helsepersonell.LEGE, // Professional group of the healthcare professional recieving the message
            )
        ),
        vedlegg = Vedlegg( // Only PDF is supported
            date = OffsetDateTime.now(), // Creation time for the attachment
            description = "<Description of the attachment>",
            data = File("some.pdf").inputStream(), // InputStream containing the bytes for the attached PDF
        ),
        version = DialogmeldingVersion.V1_1, // 1.0 is also supported, but 1.1 is preferred and will be the main focus
    )
)
```

#### Send message to receiver by HER-id
```kotlin
        client.sendMessage(
            OutgoingBusinessDocument(
                id = UUID.randomUUID(), // This id will be used to connect an application receipt to this message
                sender = Organization(
                    name = "<Name of sender organization (virksomhet)>",
                    id = Id(
                        id = "<Sender organization HER-id>",
                        type = OrganisasjonIdType.HER_ID,
                    ),
                    childOrganization = Organization(
                        name = "<Name of the sending service (tjeneste), which is owned by the organization specified above>",
                        id = Id(
                            id = "<The HER-id of this service>",
                            type = OrganisasjonIdType.HER_ID,
                        ),
                    )
                ),
                receiver = HerIdReceiver(
                    parent = HerIdReceiverParent(
                        name = "<Name of the receiving organization (virksomhet)>",
                        id = Id(
                            id = "<Receiving organization HER-id>",
                            type = OrganisasjonIdType.HER_ID,
                        ),
                    ),
                    child = OrganizationHerIdReceiverChild(
                        name = "<Name of the receiving service (tjeneste), which is owned by the organization specified above>",
                        id = Id(
                            id = "<The HER-id of this service>",
                            type = PersonIdType.HER_ID,
                        ),
                    ),
                    patient = Patient(
                        fnr = "<Id of the patient the message is about (fødselsnummer, etc.)>",
                        firstName = "<First name>",
                        middleName = "<Middle name>",
                        lastName = "<Last name>",
                    ),
                ),
                message = BusinessDocumentMessage(
                    subject = "<Message subject>",
                    body = "<Message body>",
                    responsibleHealthcareProfessional = HealthcareProfessional(
                        // Person responsible for this message at the sender
                        firstName = "<First name>",
                        middleName = "<Middle name>",
                        lastName = "<Last name>",
                        phoneNumber = "11223344",
                        roleToPatient = HelsepersonellsFunksjoner.HELSEFAGLIG_KONTAKT, // This persons role with respect to the patient
                    ),
                    recipientContact = RecipientContact(
                        // Required for Dialogmelding 1.1 Helsefaglig Dialog when temaKodet is "Henvendelse om pasient" (which is currently the only option and is chosen automatically)
                        type = Helsepersonell.LEGE, // Professional group of the healthcare professional recieving the message
                    )
                ),
                vedlegg = Vedlegg( // Only PDF is supported
                    date = OffsetDateTime.now(), // Creation time for the attachment
                    description = "<Description of the attachment>",
                    data = File("some.pdf").inputStream(), // InputStream containing the bytes for the attached PDF
                ),
                version = DialogmeldingVersion.V1_1, // 1.0 is also supported, but 1.1 is preferred and will be the main focus
            )
        )
```

#### Send application receipt
Sends a receipt to the sender of the message.
```kotlin
client.sendApplicationReceipt(
    OutgoingApplicationReceipt(
        acknowledgedId = UUID.randomUUID(), // Id of the received message that the app rec is acknowledging
        senderHerId = senderHerId,
        status = StatusForMottakAvMelding.AVVIST,
        errors = listOf(ApplicationReceiptError(FeilmeldingForApplikasjonskvittering.ANNEN_FEIL, "Something went wrong")), // Not allowed when status is OK, required otherwise
    )
)
```

#### Mark message as read
This will cause a received message to not be returned in future calls to get messages.
```kotlin
client.markMessageRead(idOfMessageToBeMarked, receiverHerId)
```

### Retrieving messages

#### Get messages
Messages only include id and HER-id of the receiver.
```kotlin
client.getMessages(receiverHerId)
```

#### Get messages with extra metadata
Messages include extra metadata, including the business document id, and whether it is an app rec (application receipt).
```kotlin
client.getMessagesWithMetadata(receiverHerId)
```

#### Get application receipt
Gets a business document that is an app rec (application receipt), using the business document id returned when getting messages with metadata.
Includes the status sent by the recipient of the original message and, if it is an error, any relevant error messages.
```kotlin
client.getApplicationReceipt(id)
```

#### Get business document (currently only supports very specific message types)
Gets a business document that is not an app rec (application receipt), using the business document id returned when getting messages with metadata.
```kotlin
client.getBusinessDocument(id)
```
