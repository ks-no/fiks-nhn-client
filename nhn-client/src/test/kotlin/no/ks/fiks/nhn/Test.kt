package no.ks.fiks.nhn

import io.kotest.core.spec.style.StringSpec

class Test : StringSpec({

    "test" {
        val xml = MessageBuilder.buildNhnMessage(
            Message(
                type = MessageType.DIALOG_FORESPORSEL,
                sender = Organisation(
                    name = "KS-DIGITALE FELLESTJENESTER AS",
                    id = Id(
                        id = "8142987",
                        type = OrganisasjonIdType.HER_ID,
                    ),
                    childOrganisation = Organisation(
                        name = "KS-DIGITALE FELLESTJENESTER AS",
                        id = Id(
                            id = "8142987",
                            type = OrganisasjonIdType.HER_ID,
                        ),
                    )
                ),
//            receiver = FastlegeForPersonReceiver(
//                fnr = "12345678910",
//            ),
                receiver = HerIdReceiver(
                    parent = HerIdReceiverParent(
                        name = "Legekontor",
                        id = Id(
                            id = "9999999",
                            type = OrganisasjonIdType.HER_ID,
                        ),
                    ),
//                child = OrganisasjonHerIdReceiverChild(
//                    name = "Organisasjonen",
//                    id = Id(
//                        id = "88888",
//                        type = PersonIdType.HER_ID,
//                    ),
//                ),
                    child = PersonHerIdReceiverChild(
                        firstName = "Fornavn",
                        middleName = "Mellomnavn",
                        lastName = "Etternavn",
                        id = Id(
                            id = "88888",
                            type = PersonIdType.HER_ID,
                        ),
                    ),
                    patient = Patient(
                        fnr = "12345678910",
                        firstName = "Ola",
                        middleName = null,
                        lastName = "Nordmann",
                    ),
                )
            )
        )
        println(xml)
    }

})
