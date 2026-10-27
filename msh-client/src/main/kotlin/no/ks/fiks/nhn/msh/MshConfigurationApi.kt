package no.ks.fiks.nhn.msh

data class MshConfiguration(
    val herId: Int,
    val receiveNotificationChannel: ReceiveNotificationChannel,
    val clientLocked: Boolean? = null,
    val rejectMessageFilters: RejectMessageFilters? = null,
)

enum class ReceiveNotificationChannel {
    Api,
    Kafka,
}

data class RejectMessageFilters(
    val messageFunctions: List<String>,
)
