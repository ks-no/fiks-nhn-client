package no.ks.fiks.nhn.msh

data class StatusInfo(
    val receiverHerId: Int,
    val deliveryState: DeliveryState,
    val appRecStatus: AppRecStatus?,
)

enum class DeliveryState {
    UNCONFIRMED,
    ACKNOWLEDGED,
    REJECTED,
}

enum class AppRecStatus {
    OK,
    REJECTED,
    OK_ERROR_IN_MESSAGE_PART,
}
