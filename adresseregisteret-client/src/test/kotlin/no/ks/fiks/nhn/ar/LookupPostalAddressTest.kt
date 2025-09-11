package no.ks.fiks.nhn.ar

import io.kotest.assertions.asClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.random.Random.Default.nextInt

class LookupPostalAddressTest : FreeSpec(){
  init {

      "Verify that missing communicationparty throws AddressNotFoundException" {
          assertThrows<AddressNotFoundException> {
              buildClient(setupServiceMock(null))
                  .lookupPostalAddress(nextInt(1000, 100000))
          }.asClue {
              it.message shouldBe "Did not find any communication party related to herId"
          }
      }

      "Verify that communicationparty with no physicalAddress throws AddressNotFoundException" {
          val organizationPerson = buildOrganizationPerson(physicalAddresses = emptyList())
          assertThrows<AddressNotFoundException> {
              buildClient(setupServiceMock(organizationPerson))
                  .lookupPostalAddress(nextInt(1000, 100000))
          }.asClue {
              it.message shouldBe "Could not find any physicalAdresses related to herId"
          }
      }

      "Verify that postadresse is primarily chosen if many physicalAddresses are present" {
          val postalAddress = buildPhysicalAddress(type = PostalAddressType.POSTADRESSE.code)
          val name = UUID.randomUUID().toString()
          val organizationPerson =
              buildOrganizationPerson(
                  name = name,
                  physicalAddresses =
                      listOf(
                          buildPhysicalAddress(type = PostalAddressType.BESOKSADRESSE.code),
                          postalAddress,
                          buildPhysicalAddress(type = PostalAddressType.BESOKSADRESSE.code),
                      )
              )
          buildClient(setupServiceMock(organizationPerson))
              .lookupPostalAddress(nextInt(1000, 100000)).asClue {
                  it.name shouldBe name
                  it.streetAddress shouldBe postalAddress.streetAddress.value
                  it.postalCode shouldBe postalAddress.postalCode.toString().padStart(4, '0')
                  it.postbox shouldBe postalAddress.postbox.value
                  it.city shouldBe postalAddress.city.value
                  it.country shouldBe postalAddress.country.value?.codeText?.value

              }
      }

      "Verify that first instance of besoksadresse is chosen if postadresse is not present" {
          val besoksadresse = buildPhysicalAddress(type = PostalAddressType.BESOKSADRESSE.code)
          val name = UUID.randomUUID().toString()
          val organizationPerson =
              buildOrganizationPerson(
                  name = name,
                  physicalAddresses =
                      listOf(
                          besoksadresse,
                          buildPhysicalAddress(type = PostalAddressType.BESOKSADRESSE.code),
                      )
              )
          buildClient(setupServiceMock(organizationPerson))
              .lookupPostalAddress(nextInt(1000, 100000)).asClue {
                  it.name shouldBe name
                  it.streetAddress shouldBe besoksadresse.streetAddress.value
                  it.postalCode shouldBe besoksadresse.postalCode.toString().padStart(4, '0')
                  it.postbox shouldBe besoksadresse.postbox.value
                  it.city shouldBe besoksadresse.city.value
                  it.country shouldBe besoksadresse.country.value?.codeText?.value
              }
      }
  }
}