package no.ks.fiks.nhn.msh


class HttpClientException(status: Int, expectedStatus: Int? = null, body: String) : HttpException(status, expectedStatus, body)

