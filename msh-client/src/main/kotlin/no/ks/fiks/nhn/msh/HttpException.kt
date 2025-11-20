package no.ks.fiks.nhn.msh

private const val MESSAGE_MAX_BODY_LENGTH = 500

open class HttpException(val status: Int, val body: String) : MshException("Got HTTP status $status: ${body.truncate()}")

private fun String.truncate() =
    if (length > MESSAGE_MAX_BODY_LENGTH) take(MESSAGE_MAX_BODY_LENGTH - 3) + "..."
    else this
