package no.ks.fiks.nhn

import no.ks.fiks.hdir.OrganizationIdType
import no.ks.fiks.hdir.PersonIdType
import no.ks.fiks.nhn.msh.OrganizationId
import no.ks.fiks.nhn.msh.PersonId
import java.util.UUID
import kotlin.random.Random.Default.nextInt


fun readResourceContentAsString(path: String) = readResourceContent(path).decodeToString()

fun readResourceContent(path: String) = ClassLoader.getSystemResource(path).readBytes()

fun randomHerId(): Int = nextInt(0, 1000000)
fun randomString(): String = UUID.randomUUID().toString()
fun randomOrganizationHerId(): OrganizationId = OrganizationId(randomString(), OrganizationIdType.HER_ID)
fun randomPersonHerId(): PersonId = PersonId(randomString(), PersonIdType.HER_ID)
