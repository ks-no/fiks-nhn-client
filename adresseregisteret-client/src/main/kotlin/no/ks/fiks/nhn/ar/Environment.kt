package no.ks.fiks.nhn.ar

class Environment(
    val url: String,
)
{
    companion object {
        val TEST = Environment(
            url = "https://ws-web.test.nhn.no/v1/AR",
        )

        val PROD = Environment(
            url = "",
        )
    }
}