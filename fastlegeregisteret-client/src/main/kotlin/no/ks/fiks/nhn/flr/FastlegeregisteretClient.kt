package no.ks.fiks.nhn.flr

import no.nhn.schemas.reg.flr.IFlrReadOperationsGetPatientGPDetailsGenericFaultFaultFaultMessage
import no.nhn.schemas.reg.flr.PatientToGPContractAssociation

class FastlegeregisteretClient(
    private val service: FastlegeregisteretService,
) {

    fun getPatientGP(patientId: String): PatientGP? =
        try {
            service.getPatientGPDetails(patientId)?.convert()
        } catch (e: IFlrReadOperationsGetPatientGPDetailsGenericFaultFaultFaultMessage) {
            throw FastlegeregisteretException(e.faultInfo?.errorCode?.value, e.faultInfo?.message?.value, e.message)
        }

    private fun PatientToGPContractAssociation.convert() = PatientGP(
        patientId = patientNIN.value,
        gpHerId = gpHerId.value,
    )

}
