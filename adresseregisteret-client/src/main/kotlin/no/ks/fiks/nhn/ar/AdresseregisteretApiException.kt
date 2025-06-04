package no.ks.fiks.nhn.ar

class AdresseregisteretApiException(
    val errorCode: String?,
    val faultMessage: String?,
    message: String?,
) : AdresseregisteretException(message)