package no.ks.fiks.nhn.ar

import java.time.OffsetDateTime
import java.time.ZoneId
import javax.xml.datatype.XMLGregorianCalendar

private val zoneOslo = ZoneId.of("Europe/Oslo")

fun XMLGregorianCalendar.toOffsetDateTime(): OffsetDateTime = OffsetDateTime.ofInstant(toGregorianCalendar().toInstant(), zoneOslo)