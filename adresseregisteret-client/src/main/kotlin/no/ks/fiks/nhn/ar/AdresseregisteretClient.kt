package no.ks.fiks.nhn.ar

import jakarta.xml.bind.JAXBElement
import jakarta.xml.ws.soap.SOAPBinding
import mu.KotlinLogging
import no.nhn.register.communicationparty.*
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.ws.addressing.WSAddressingFeature
import no.nhn.register.communicationparty.CommunicationParty as NhnCommunicationParty

private val log = KotlinLogging.logger { }

class AdresseregisteretClient(
    environment: Environment,
    credentials: Credentials,
    private val service: ICommunicationPartyService = buildService(environment, credentials),
) {

    fun lookupHerId(herId: Int): CommunicationParty? =
        try {
            service.getCommunicationPartyDetails(herId)
                ?.let {
                    when (it) {
                        is Organization -> it.convert()
                        is OrganizationPerson -> it.convert()
                        is Service -> it.convert()
                        else -> throw RuntimeException("Unsupported communication party type: ${it::class}")
                    }
                }
        } catch (e: ICommunicationPartyServiceGetCommunicationPartyDetailsGenericFaultFaultFaultMessage) {
            log.debug(e) { "Exception was thrown by service" }
            throw AdresseregisteretApiException(e.faultInfo?.errorCode?.value, e.faultInfo?.message?.value, e.message)
        }

    fun lookupPostalAddress(herId: Int): PostalAddress? =
        lookupHerId(herId)?.let { communicationParty ->
            if (communicationParty.physicalAddresses.isEmpty()) {
                throw AddressNotFoundException("Could not find any physicalAdresses related to herId")
            }
            log.debug("Found ${communicationParty.physicalAddresses.size} addresses for $herId")

            val addressPriority = listOf(AddressType.POSTADRESSE, AddressType.BESOKSADRESSE)

            addressPriority.firstNotNullOfOrNull { type -> communicationParty.physicalAddresses.firstOrNull { it.type == type } }
                ?.toPostalAddress(communicationParty.name)
                ?: throw AddressNotFoundException("Could not find any relevant physicalAdresses related to herId")
        } ?: throw AddressNotFoundException("Did not find any communication party related to herId")


    private fun Organization.convert() = OrganizationCommunicationParty(
        herId = herId,
        name = name.value,
        parent = convertParent(),
        physicalAddresses = convertPhysicalAddresses(),
        organizationNumber = organizationNumber?.toString()?.padStart(9, '0'),
    )

    private fun OrganizationPerson.convert() = PersonCommunicationParty(
        herId = herId,
        name = name.value,
        parent = convertParent(),
        physicalAddresses = convertPhysicalAddresses(),
        firstName = person.value.firstName.value,
        middleName = person.value?.middleName?.value,
        lastName = person.value.lastName.value,
    )

    private fun Service.convert() = ServiceCommunicationParty(
        herId = herId,
        name = name.value,
        parent = convertParent(),
        physicalAddresses = convertPhysicalAddresses(),
    )

    private fun NhnCommunicationParty.convertParent() =
        takeIf { parentHerId != null && parentHerId != -1 }
            ?.run {
                CommunicationPartyParent(
                    herId = parentHerId,
                    name = parentName.value ?: "",
                )
            }

    private fun NhnCommunicationParty.convertPhysicalAddresses() = physicalAddresses.value?.physicalAddress
        ?.map { address ->
            PhysicalAddress(
                type = AddressType.fromCode(address.type.value?.codeValue?.value),
                streetAddress = address.streetAddress.valueNotBlank(),
                postbox = address.postbox.valueNotBlank(),
                postalCode = address.postalCode?.toString()?.padStart(4, '0'),
                city = address.city.valueNotBlank(),
                country = address.country.value?.codeText?.valueNotBlank(),
            )
        }
        ?: emptyList()

}

private fun JAXBElement<String>.valueNotBlank() = value?.takeIf { it.isNotBlank() }

class AdresseregisteretClientBuilder {

    private var environment: Environment? = null
    private var credentials: Credentials? = null
    private var service: ICommunicationPartyService? = null

    fun url(url: String) = apply { this.environment = Environment(url) }
    fun environment(environment: Environment) = apply { this.environment = environment }
    fun credentials(credentials: Credentials) = apply { this.credentials = credentials }
    fun service(service: ICommunicationPartyService) = apply { this.service = service }

    fun build(): AdresseregisteretClient {
        val environment = environment ?: throw IllegalStateException("environment is required")
        val credentials = credentials ?: throw IllegalStateException("credentials is required")
        return AdresseregisteretClient(
            environment = environment,
            credentials = credentials,
            service = service ?: buildService(environment, credentials),
        )
    }

}

private fun buildService(environment: Environment, credentials: Credentials): ICommunicationPartyService =
    JaxWsProxyFactoryBean().apply {
        address = environment.url

        username = credentials.username
        password = credentials.password

        features.add(WSAddressingFeature())
        bindingId = SOAPBinding.SOAP12HTTP_BINDING
    }.create(ICommunicationPartyService::class.java)
