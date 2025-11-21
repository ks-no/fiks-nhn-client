package no.ks.fiks.nhn.msh

private const val MESSAGE_MAX_BODY_LENGTH = 500

open class HttpException(val status: Int, expectedStatus: Int? = null, val body: String) : MshException("${buildStatusMessage(status, expectedStatus)}: ${body.truncate()}")

private fun String.truncate() =
    if (length > MESSAGE_MAX_BODY_LENGTH) take(MESSAGE_MAX_BODY_LENGTH - 3) + "..."
    else this

private fun buildStatusMessage(status: Int, expectedStatus: Int?) =
    if (expectedStatus == null) "Got HTTP status $status"
    else "Got HTTP status $status when expecting $expectedStatus"
