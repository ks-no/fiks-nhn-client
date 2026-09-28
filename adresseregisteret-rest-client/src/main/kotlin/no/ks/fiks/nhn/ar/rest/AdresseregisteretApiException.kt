package no.ks.fiks.nhn.ar.rest

class AdresseregisteretApiException(
    val statusCode: Int,
    message: String?,
    cause: Throwable? = null,
) : AdresseregisteretException(message, cause)

