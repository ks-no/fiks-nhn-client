package no.ks.fiks.nhn.msh


class HttpRedirectException(status: Int, expectedStatus: Int? = null, body: String) : HttpException(status, expectedStatus, body)

