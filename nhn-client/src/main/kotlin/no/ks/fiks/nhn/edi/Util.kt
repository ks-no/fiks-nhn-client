package no.ks.fiks.nhn.edi

import no.kith.xmlstds.CS
import no.kith.xmlstds.CV
import no.ks.fiks.hdir.KodeverkVerdi


fun KodeverkVerdi.toCV() = CV().apply {
    v = verdi
    dn = navn
    s = kodeverk
}

fun KodeverkVerdi.toCS() = CS().apply {
    v = verdi
    dn = navn
}
