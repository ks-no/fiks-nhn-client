package no.ks.fiks.nhn.msh

class Environment(
    val url: String,
)
{
    companion object {
        val TEST = Environment(
            url = "https://api.tjener.test.melding.nhn.no",
        )

        val PROD = Environment(
            url = "",
        )
    }
}