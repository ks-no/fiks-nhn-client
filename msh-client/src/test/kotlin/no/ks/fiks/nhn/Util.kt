package no.ks.fiks.nhn

import no.ks.fiks.hdir.Adressetype
import no.ks.fiks.hdir.OrganizationIdType
import no.ks.fiks.hdir.PersonIdType
import no.ks.fiks.nhn.msh.Address
import no.ks.fiks.nhn.msh.Country
import no.ks.fiks.nhn.msh.County
import no.ks.fiks.nhn.msh.OrganizationId
import no.ks.fiks.nhn.msh.PersonId
import java.util.Locale
import java.util.UUID
import kotlin.random.Random.Default.nextInt


fun readResourceContentAsString(path: String) = readResourceContent(path).decodeToString()

fun readResourceContent(path: String) = ClassLoader.getSystemResource(path).readBytes()

fun randomHerId(): Int = nextInt(0, 1000000)
fun randomString(): String = UUID.randomUUID().toString()
fun randomOrganizationHerId(): OrganizationId = OrganizationId(randomString(), OrganizationIdType.HER_ID)
fun randomPersonHerId(): PersonId = PersonId(randomString(), PersonIdType.HER_ID)

fun randomAddress(): Address = Address(
    type = Adressetype.entries.random(),
    streetAdr = UUID.randomUUID().toString(),
    postalCode = UUID.randomUUID().toString(),
    city = UUID.randomUUID().toString(),
    postbox = UUID.randomUUID().toString(),
    county = County(code = nextInt(1000, 10000).toString(), name = UUID.randomUUID().toString()),
    country = Country(code = Locale.getISOCountries().random(), name = UUID.randomUUID().toString()),
)