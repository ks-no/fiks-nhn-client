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
                        type = null,
                        streetAdr = it.address,
                        postalCode = it.postalCode,
                        city = it.city,
                        postbox = it.postalBox,
                        county = null,
                        country = null
                    )
                },
                firstName = "",
                middleName = null,
                lastName = fastlege.name ?: "",
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


}