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
            throw FastlegeregisteretApiException(
                errorCode = e.faultInfo?.errorCode?.value,
                faultMessage = e.faultInfo?.message?.value,
                message = e.message,
                cause = e,
            )
        } catch (e: Exception) {
            throw FastlegeregisteretException(
                message = "Unknown error from Fastlegeregisteret",
                cause = e,
            )
        }

    private fun PatientToGPContractAssociation.convert() = PatientGP(
        patientId = patientNIN.value,
        gpHerId = gpHerId.value,
    )

}
