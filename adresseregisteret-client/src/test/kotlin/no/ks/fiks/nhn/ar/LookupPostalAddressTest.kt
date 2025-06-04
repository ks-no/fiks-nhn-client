package no.ks.fiks.nhn.ar

import io.kotest.assertions.asClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.assertThrows
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
          val organizationPerson = buildOrganizationPerson(addresses = emptyList())
          assertThrows<AddressNotFoundException> {
              buildClient(setupServiceMock(organizationPerson))
                  .lookupPostalAddress(nextInt(1000, 100000))
          }.asClue {
              it.message shouldBe "Could not find any physicalAdresses related to herId"
          }
      }

      "Verify that communicationparty with no relevant physicalAddress throws AddressNotFoundException" {
          val organizationPerson =
              buildOrganizationPerson(
                  addresses =
                      listOf(
                          buildPhysicalAddress(type = AddressType.UBRUKELIG_ADRESSE.code),
                          buildPhysicalAddress(type = AddressType.ARBEIDSADRESSE.code)
                      )
              )
          assertThrows<AddressNotFoundException> {
              buildClient(setupServiceMock(organizationPerson))
                  .lookupPostalAddress(nextInt(1000, 100000))
          }.asClue {
              it.message shouldBe "Could not find any relevant physicalAdresses related to herId"
          }
      }

      "Verify that postadresse is primarily chosen if many physicalAddresses are present" {
          val organizationPerson =
              buildOrganizationPerson(
                  addresses =
                      listOf(
                          buildPhysicalAddress(type = AddressType.BOSTEDSADRESSE.code),
                          buildPhysicalAddress(type = AddressType.MIDLERTIDIG_ADRESSE.code),
                          buildPhysicalAddress(type = AddressType.POSTADRESSE.code),
                          buildPhysicalAddress(type = AddressType.BESOKSADRESSE.code),
                      )
              )
          buildClient(setupServiceMock(organizationPerson))
              .lookupPostalAddress(nextInt(1000, 100000)).asClue {
                  it!!.type shouldBe AddressType.POSTADRESSE
              }
      }

      "Verify that bostedsadresse is chosen if postadresse is not present" {
          val organizationPerson =
              buildOrganizationPerson(
                  addresses =
                      listOf(
                          buildPhysicalAddress(type = AddressType.MIDLERTIDIG_ADRESSE.code),
                          buildPhysicalAddress(type = AddressType.BOSTEDSADRESSE.code),
                          buildPhysicalAddress(type = AddressType.BESOKSADRESSE.code),
                      )
              )
          buildClient(setupServiceMock(organizationPerson))
              .lookupPostalAddress(nextInt(1000, 100000)).asClue {
                  it!!.type shouldBe AddressType.BOSTEDSADRESSE
              }
      }
  }
}