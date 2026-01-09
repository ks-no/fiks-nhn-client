package no.ks.fiks.nhn.ar

import io.kotest.core.spec.style.StringSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import no.nhn.register.communicationparty.CommunicationParty
import java.time.Duration
import java.time.Instant
import kotlin.random.Random.Default.nextInt

class LookupCacheTest : StringSpec({

    "Lookup should not be cached if cache config is not provided" {
        val queryHerId = nextInt(1000, 100000)
        val expected = buildOrganization()

        val service = setupServiceMock(expected)
        buildClient(service).also {
            it.lookupHerId(queryHerId)
            it.lookupHerId(queryHerId)
        }

        verifySequence {
            service.getCommunicationPartyDetails(queryHerId)
            service.getCommunicationPartyDetails(queryHerId)
        }
    }

    "Lookup should be cached if cache config is provided" {
        val queryHerId = nextInt(1000, 100000)
        val expected = buildOrganization()

        val service = setupServiceMock(expected)
        AdresseregisteretClient(service, CacheConfig()).also {
            it.lookupHerId(queryHerId)
            it.lookupHerId(queryHerId)
        }

        verifySequence {
            service.getCommunicationPartyDetails(queryHerId)
        }
    }

    "Lookup should be cached for the configured amount of time" {
        val queryHerId = nextInt(1000, 100000)
        val expected = buildOrganization()

        val service = setupServiceMock(expected)
        val client = AdresseregisteretClient(service, CacheConfig(cacheTtl = Duration.ofMillis(500)))

        val start = Instant.now()
        while (start.plusMillis(400).isAfter(Instant.now())) {
            client.lookupHerId(queryHerId)
        }

        verify(exactly = 1) { service.getCommunicationPartyDetails(queryHerId) }

        while (start.plusMillis(800).isAfter(Instant.now())) {
            client.lookupHerId(queryHerId)
        }

        verify(exactly = 2) { service.getCommunicationPartyDetails(queryHerId) }
    }

    "Lookup should be cached separately for each HER-id" {
        val expected = buildOrganization()

        val service = setupServiceMock(expected)
        val client = AdresseregisteretClient(service, CacheConfig(cacheTtl = Duration.ofMillis(500)))

        val herIds = (1..nextInt(10, 20)).map { nextInt(1000, 100000) }

        herIds.shuffled().forEach { herId ->
            repeat(nextInt(1, 10)) {
                client.lookupHerId(herId)
            }
        }

        herIds.forEach { herId ->
            verify(exactly = 1) { service.getCommunicationPartyDetails(herId) }
        }
    }

    "Entries should be evicted if cache reaches max size" {
        val expected = buildOrganization()

        val service = setupServiceMock(expected)
        val client = AdresseregisteretClient(service, CacheConfig(maxSize = 1))

        val herId1 = nextInt(1000, 100000)
        val herId2 = nextInt(1000, 100000)

        client.lookupHerId(herId1)
        client.lookupHerId(herId2)
        client.lookupHerId(herId2)
        Thread.sleep(100) // Wait for async cache eviction
        client.lookupHerId(herId1)

        verifySequence {
            service.getCommunicationPartyDetails(herId1)
            service.getCommunicationPartyDetails(herId2)
            service.getCommunicationPartyDetails(herId1)
        }
    }

})
