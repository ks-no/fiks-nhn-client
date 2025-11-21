package no.ks.fiks.nhn.msh


class HttpServerException(status: Int, expectedStatus: Int? = null, body: String) : HttpException(status, expectedStatus, body)

