package no.ks.fiks.nhn.msh

import no.ks.fiks.hdir.OrganisasjonIdType
import no.ks.fiks.hdir.PersonIdType
import no.ks.fiks.nhn.ar.AdresseregisteretClient
import no.ks.fiks.nhn.ar.CommunicationParty
import no.ks.fiks.nhn.ar.PersonCommunicationParty
import no.ks.fiks.nhn.flr.FastlegeregisteretClient

class GpForPersonReceiverBuilder(
    private val flrClient: FastlegeregisteretClient,
    private val arClient: AdresseregisteretClient,
) {

    fun buildGpForPersonReceiver(person: Person): HerIdReceiver {
        val fastlege = lookupFastlege(person.fnr)
        val parent = fastlege.parent ?: throw GpNotFoundException("GP does not have a parent", person.fnr)

        return HerIdReceiver(
            parent = HerIdReceiverParent(
                name = parent.name,
                id = Id(
                    id = parent.herId.toString(),
                    type = OrganisasjonIdType.HER_ID,
                ),
            ),
            child = PersonHerIdReceiverChild(
                id = Id(
                    id = fastlege.herId.toString(),
                    type = PersonIdType.HER_ID,
                ),
                firstName = fastlege.firstName,
                middleName = fastlege.middleName,
                lastName = fastlege.lastName,
            ),
            patient = Patient(
                fnr = person.fnr,
                firstName = person.firstName,
                middleName = person.middleName,
                lastName = person.lastName,
            ),
        )
    }

    private fun lookupFastlege(personId: String): PersonCommunicationParty =
        flrClient.getPatientGP(personId)
            ?.let { patientGP ->
                arClient
                    .lookupHerId(patientGP.gpHerId ?: throw GpNotFoundException("GP does not have HER-id", personId))
                    .let { it as? PersonCommunicationParty ?: throw GpNotFoundException("Adresseregisteret returned a communication party that is not a person", personId) }
            }
            ?: throw GpNotFoundException("Could not find GP for person", personId)


}