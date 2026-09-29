package no.ks.fiks.nhn.ar.rest

class AddressNotFoundException(
    message: String?,
) : AdresseregisteretApiException(404, message, null)

