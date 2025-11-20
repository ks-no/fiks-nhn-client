package no.ks.fiks.nhn.msh


class HttpClientException(status: Int, body: String) : HttpException(status, body)

