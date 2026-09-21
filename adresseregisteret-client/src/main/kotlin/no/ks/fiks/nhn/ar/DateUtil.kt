package no.ks.fiks.nhn.ar

import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone
import javax.xml.datatype.XMLGregorianCalendar

private val zoneOslo = ZoneId.of("Europe/Oslo")

fun XMLGregorianCalendar.toOffsetDateTime(): OffsetDateTime = toGregorianCalendar(TimeZone.getTimeZone(zoneOslo), null, null).toZonedDateTime().toOffsetDateTime()