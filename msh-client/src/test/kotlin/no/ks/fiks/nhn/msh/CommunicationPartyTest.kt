package no.ks.fiks.nhn.msh

import io.kotest.assertions.asClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.beNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.ks.fiks.hdir.OrganizationIdType
import no.ks.fiks.hdir.PersonIdType
import no.ks.fiks.nhn.randomHerId
import java.util.UUID

class CommunicationPartyTest : FreeSpec({

    "Organization" - {
        "HER-id should be null for empty list" {
            OrganizationCommunicationParty(
                ids = listOf(),
                name = UUID.randomUUID().toString(),
            ).asClue {
                it.herId should beNull()
            }
        }

        "HER-id should be null if list does not have HER-id" {
            OrganizationCommunicationParty(
                ids = listOf(OrganizationId(UUID.randomUUID().toString(), OrganizationIdType.ENH)),
                name = UUID.randomUUID().toString(),
            ).asClue {
                it.herId should beNull()
            }
        }

        "HER-id should be null if list has invalid HER-id" {
            OrganizationCommunicationParty(
                ids = listOf(OrganizationId(UUID.randomUUID().toString(), OrganizationIdType.HER_ID)),
                name = UUID.randomUUID().toString(),
            ).asClue {
                it.herId should beNull()
            }
        }

        "Should return HER-id if list has valid HER-id" {
            val herId = randomHerId()
            OrganizationCommunicationParty(
                ids = listOf(OrganizationId(herId.toString(), OrganizationIdType.HER_ID)),
                name = UUID.randomUUID().toString(),
            ).asClue {
                it.herId shouldBe herId
            }
        }

        "Should return HER-id if list has multiple ids" {
            val herId = randomHerId()
            OrganizationCommunicationParty(
                ids = listOf(
                    OrganizationId(UUID.randomUUID().toString(), OrganizationIdType.ENH),
                    OrganizationId(herId.toString(), OrganizationIdType.HER_ID),
                ),
                name = UUID.randomUUID().toString(),
            ).asClue {
                it.herId shouldBe herId
            }
        }

        "Should return first HER-id if list has multiple HER-ids" {
            val herId = randomHerId()
            OrganizationCommunicationParty(
                ids = listOf(
                    OrganizationId(UUID.randomUUID().toString(), OrganizationIdType.ENH),
                    OrganizationId(herId.toString(), OrganizationIdType.HER_ID),
                    OrganizationId(randomHerId().toString(), OrganizationIdType.HER_ID),
                ),
                name = UUID.randomUUID().toString(),
            ).asClue {
                it.herId shouldBe herId
            }
        }
    }

    "Person" - {
        "HER-id should be null for empty list" {
            PersonCommunicationParty(
                ids = listOf(),
                firstName = UUID.randomUUID().toString(),
                middleName = UUID.randomUUID().toString(),
                lastName = UUID.randomUUID().toString(),
            ).asClue {
                it.herId should beNull()
            }
        }

        "HER-id should be null if list does not have HER-id" {
            PersonCommunicationParty(
                ids = listOf(PersonId(UUID.randomUUID().toString(), PersonIdType.FNR)),
                firstName = UUID.randomUUID().toString(),
                middleName = UUID.randomUUID().toString(),
                lastName = UUID.randomUUID().toString(),
            ).asClue {
                it.herId should beNull()
            }
        }

        "HER-id should be null if list has invalid HER-id" {
            PersonCommunicationParty(
                ids = listOf(PersonId(UUID.randomUUID().toString(), PersonIdType.HER_ID)),
                firstName = UUID.randomUUID().toString(),
                middleName = UUID.randomUUID().toString(),
                lastName = UUID.randomUUID().toString(),
            ).asClue {
                it.herId should beNull()
            }
        }

        "Should return HER-id if list has valid HER-id" {
            val herId = randomHerId()
            PersonCommunicationParty(
                ids = listOf(PersonId(herId.toString(), PersonIdType.HER_ID)),
                firstName = UUID.randomUUID().toString(),
                middleName = UUID.randomUUID().toString(),
                lastName = UUID.randomUUID().toString(),
            ).asClue {
                it.herId shouldBe herId
            }
        }

        "Should return HER-id if list has multiple ids" {
            val herId = randomHerId()
            PersonCommunicationParty(
                ids = listOf(
                    PersonId(UUID.randomUUID().toString(), PersonIdType.FNR),
                    PersonId(herId.toString(), PersonIdType.HER_ID),
                    PersonId(UUID.randomUUID().toString(), PersonIdType.HPR),
                ),
                firstName = UUID.randomUUID().toString(),
                middleName = UUID.randomUUID().toString(),
                lastName = UUID.randomUUID().toString(),
            ).asClue {
                it.herId shouldBe herId
            }
        }

        "Should return first HER-id if list has multiple HER-ids" {
            val herId = randomHerId()
            PersonCommunicationParty(
                ids = listOf(
                    PersonId(UUID.randomUUID().toString(), PersonIdType.FNR),
                    PersonId(herId.toString(), PersonIdType.HER_ID),
                    PersonId(randomHerId().toString(), PersonIdType.HER_ID),
                ),
                firstName = UUID.randomUUID().toString(),
                middleName = UUID.randomUUID().toString(),
                lastName = UUID.randomUUID().toString(),
            ).asClue {
                it.herId shouldBe herId
            }
        }
    }

})