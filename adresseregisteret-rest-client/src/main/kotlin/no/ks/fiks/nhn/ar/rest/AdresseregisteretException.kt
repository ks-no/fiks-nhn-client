package no.ks.fiks.nhn.ar.rest

open class AdresseregisteretException(
    message: String?,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class AddressNotFoundException(
    message: String?,
) : AdresseregisteretException(message)
