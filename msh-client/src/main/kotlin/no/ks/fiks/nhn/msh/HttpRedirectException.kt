package no.ks.fiks.nhn.msh


class HttpRedirectException(status: Int, body: String) : HttpException(status, body)

