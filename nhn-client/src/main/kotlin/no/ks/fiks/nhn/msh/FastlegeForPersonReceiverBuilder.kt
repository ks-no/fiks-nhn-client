package no.ks.fiks.nhn.msh

import no.ks.fiks.hdir.OrganisasjonIdType
import no.ks.fiks.hdir.PersonIdType
import no.ks.fiks.nhn.ar.AdresseregisteretClient
import no.ks.fiks.nhn.ar.CommunicationParty
import no.ks.fiks.nhn.flr.FastlegeregisteretClient

class FastlegeForPersonReceiverBuilder(
    private val flrClient: FastlegeregisteretClient,
    private val arClient: AdresseregisteretClient,
) {

    fun buildFastlegeForPersonReceiver(person: Person): HerIdReceiver {
        val fastlege = lookupFastlege(person.fnr)

        return HerIdReceiver(
            parent = HerIdReceiverParent(
                name = fastlege.parent.name,
                id = Id(
                    id = fastlege.parent.herId.toString(),
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

    private fun lookupFastlege(personId: String): CommunicationParty =
        flrClient.lookupFastlege(personId)
            ?.let {
                arClient.lookupHerId(it.herId ?: throw RuntimeException("Fastlege mangler herId"))
            }
            ?: throw RuntimeException("Fant ikke fastlege for person")


}