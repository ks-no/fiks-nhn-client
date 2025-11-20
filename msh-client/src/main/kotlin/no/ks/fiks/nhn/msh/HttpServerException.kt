package no.ks.fiks.nhn.msh


class HttpServerException(status: Int, body: String) : HttpException(status, body)

