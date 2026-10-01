package no.ks.fiks.nhn.ar.rest

open class AdresseregisteretApiException(
    val statusCode: Int,
    message: String?,
    cause: Throwable? = null,
) : AdresseregisteretException(message, cause)

