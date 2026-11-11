package no.ks.fiks.nhn.msh

data class GetStatusResponse(
    val statusList: List<StatusInfo>?,
)

data class StatusInfo(
    val receiverHerId: Int,
    val sent: Boolean,
    val deliveryState: DeliveryState,
    val apprecInfo: AppRecInfo?,
)

data class AppRecInfo(
    val appRecStatus: AppRecStatus?,
    val appRecId: String?,
    val appRecErrors: List<IncomingApplicationReceiptError> = emptyList(),
)

enum class DeliveryState(val value: String) {
    UNCONFIRMED("Unconfirmed"),
    ACKNOWLEDGED("Acknowledged"),
    REJECTED("Rejected"),
    ABANDONED("Abandoned"),
    ;

    override fun toString() = value
}

enum class AppRecStatus(val value: String) {
    OK("Ok"),
    REJECTED("Rejected"),
    OK_ERROR_IN_MESSAGE_PART("OkErrorInMessagePart"),
    ;

    override fun toString() = value
}
