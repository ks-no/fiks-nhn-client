package no.ks.fiks.nhn.ar

open class AdresseregisteretException(
    message: String?,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class AdresseregisteretApiException(
    val errorCode: String?,
    val faultMessage: String?,
    message: String?,
    cause: Throwable? = null,
) : AdresseregisteretException(message, cause)

class AddressNotFoundException(
    message: String?,
) : AdresseregisteretException(message)
