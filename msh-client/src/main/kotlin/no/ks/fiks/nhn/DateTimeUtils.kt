package no.ks.fiks.nhn

import mu.KotlinLogging
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeParseException

private val log = KotlinLogging.logger { }

val DEFAULT_ZONE: ZoneId = ZoneId.of("Europe/Oslo")

fun String.parseOffsetDateTimeOrNull(): OffsetDateTime? =
    try {
        OffsetDateTime.parse(this)
    } catch (_: DateTimeParseException) {
        try {
            LocalDateTime.parse(this).atZone(DEFAULT_ZONE).toOffsetDateTime()
        } catch (_: DateTimeParseException) {
            log.warn { "Unable to parse date: $this" }
            null
        }
    }


