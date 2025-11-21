package no.ks.fiks.nhn.msh

open class MshException(
    message: String?,
    cause: Throwable? = null,
) : RuntimeException(message, cause)