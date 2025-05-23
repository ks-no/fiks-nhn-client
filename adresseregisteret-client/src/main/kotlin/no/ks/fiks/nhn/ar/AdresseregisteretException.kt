package no.ks.fiks.nhn.ar

class AdresseregisteretException(
    val errorCode: String?,
    message: String,
) : RuntimeException(message)