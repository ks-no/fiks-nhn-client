package no.ks.fiks.nhn.flr

class FastlegeregisteretException(
    val errorCode: String?,
    val faultMessage: String?,
    message: String?,
) : RuntimeException(message)