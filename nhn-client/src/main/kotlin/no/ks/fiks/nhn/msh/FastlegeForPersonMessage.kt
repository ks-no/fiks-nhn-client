package no.ks.fiks.nhn.msh

import no.ks.fiks.hdir.MeldingensFunksjon
import java.io.InputStream

data class FastlegeForPersonMessage(
    val type: MeldingensFunksjon,
    val sender: Organisation,
    val personFnr: String,
    val vedlegg: InputStream?,
)
