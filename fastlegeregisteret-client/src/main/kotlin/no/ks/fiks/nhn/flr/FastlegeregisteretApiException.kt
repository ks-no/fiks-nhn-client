package no.ks.fiks.nhn.flr

class FastlegeregisteretApiException(
    val errorCode: String?,
    val faultMessage: String?,
    message: String?,
    cause: Throwable? = null,
) : FastlegeregisteretException(message, cause)