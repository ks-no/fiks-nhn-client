package no.ks.fiks.nhn.ar

class AdresseregisteretException(
    val errorCode: String?,
    val faultMessage: String?,
    message: String?,
) : RuntimeException(message)