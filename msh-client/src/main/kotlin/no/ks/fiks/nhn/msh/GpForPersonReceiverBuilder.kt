package no.ks.fiks.nhn.msh

import no.ks.fiks.hdir.Adressetype
import no.ks.fiks.hdir.OrganizationIdType
import no.ks.fiks.hdir.PersonIdType
import no.ks.fiks.nhn.ar.rest.AdresseregisteretClient
import no.ks.fiks.nhn.ar.rest.model.CommunicationParty
import no.ks.fiks.nhn.flr.FastlegeregisteretClient

class GpForPersonReceiverBuilder(
    private val flrClient: FastlegeregisteretClient,
    private val arClient: AdresseregisteretClient,
) {

    fun buildGpForPersonReceiver(person: Person): Receiver {
        val fastlege = lookupFastlege(person.fnr)
        val parent = fastlege.personDetails?.parentOrganization ?: throw GpNotFoundException("GP does not have a parent", person.fnr)
        val gpName = splitName(fastlege.name)

        return Receiver(
            parent = OrganizationCommunicationParty(
                ids = listOf(
                    OrganizationId(
                        id = parent.herId.toString(),
                        type = OrganizationIdType.HER_ID,
                    )
                ),
                address = null,
                name = parent.name ?: "",
            ),
            child = PersonCommunicationParty(
                ids = listOf(
                    PersonId(
                        id = fastlege.herId.toString(),
                        type = PersonIdType.HER_ID,
                    )
                ),
                address = fastlege.postalAddress?.let {
                    Address(
                        type = Adressetype.POSTADRESSE,
                        streetAdr = it.address,
                        postalCode = it.postalCode,
                        city = it.city,
                        postbox = it.postalBox,
                        county = null,
                        country = null
                    )
                },
                firstName = gpName.firstName,
                middleName = null,
                lastName = gpName.lastName,
            ),
            patient = Patient(
                fnr = person.fnr,
                firstName = person.firstName,
                middleName = person.middleName,
                lastName = person.lastName,
            ),
        )
    }

    private fun lookupFastlege(personId: String): CommunicationParty =
        flrClient.getPatientGP(personId)
            ?.let { patientGP ->
                arClient
                    .lookupHerId(patientGP.gpHerId ?: throw GpNotFoundException("GP does not have HER-id", personId))
                    ?.takeIf { it.personDetails != null }
                    ?: throw GpNotFoundException("Adresseregisteret returned a communication party that is not a person", personId)
            }
            ?: throw GpNotFoundException("Could not find GP for person", personId)

    private fun splitName(fullName: String?): ParsedName {
        val nameParts = fullName
            ?.trim()
            ?.split(Regex("\\s+"))
            ?.filter { it.isNotBlank() }
            .orEmpty()

        return when {
            nameParts.isEmpty() -> ParsedName(firstName = "", lastName = "")
            nameParts.size == 1 -> ParsedName(firstName = "", lastName = nameParts.single())
            else -> ParsedName(
                firstName = nameParts.dropLast(1).joinToString(" "),
                lastName = nameParts.last(),
            )
        }
    }

    private data class ParsedName(
        val firstName: String,
        val lastName: String,
    )


}