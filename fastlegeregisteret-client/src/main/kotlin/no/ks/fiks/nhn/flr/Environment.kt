package no.ks.fiks.nhn.flr

class Environment(
    val url: String,
) {
    companion object {
        val TEST = Environment(
            url = "https://ws-web.test.nhn.no/v2/flr",
        )

        val PROD = Environment(
            url = "https://ws.nhn.no/v2/flr",
        )
    }
}