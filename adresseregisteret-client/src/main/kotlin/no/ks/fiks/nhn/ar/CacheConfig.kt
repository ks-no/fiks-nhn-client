package no.ks.fiks.nhn.ar

import java.time.Duration

data class CacheConfig(
    val maxSize: Long = 1000,
    val cacheTtl: Duration = Duration.ofHours(24),
)
