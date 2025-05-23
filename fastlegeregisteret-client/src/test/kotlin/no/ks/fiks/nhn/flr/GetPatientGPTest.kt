package no.ks.fiks.nhn.flr

import io.kotest.assertions.asClue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.beNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import io.mockk.every
import io.mockk.mockk
import jakarta.xml.bind.JAXBElement
import no.nhn.common.flr.GenericFault
import no.nhn.schemas.reg.flr.IFlrReadOperations
import no.nhn.schemas.reg.flr.IFlrReadOperationsGetPatientGPDetailsGenericFaultFaultFaultMessage
import no.nhn.schemas.reg.flr.PatientToGPContractAssociation
import java.util.*
import javax.xml.namespace.QName
import kotlin.random.Random.Default.nextInt

class GetPatientGPTest : StringSpec({

    "Verify that the API response is mapped correctly" {
        val expected = buildPatientGP()
        buildClient(setupServiceMock(expected))
            .getPatientGP(UUID.randomUUID().toString())
            .asClue {
                it shouldNot beNull()
                it!!.patientId shouldBe expected.patientNIN.value
                it.gpHerId shouldBe expected.gpHerId.value
            }
    }

    "Should handle null from API" {
        buildClient(setupServiceMock(null))
            .getPatientGP(UUID.randomUUID().toString())
            .asClue {
                it should beNull()
            }
    }

    "Should handle null values from API" {
        val expected = buildPatientGP(null, null)
        buildClient(setupServiceMock(expected))
            .getPatientGP(UUID.randomUUID().toString())
            .asClue {
                it shouldNot beNull()
                it!!.patientId should beNull()
                it.gpHerId should beNull()
            }
    }

    "An exception thrown by the API should be mapped to a client exception" {
        val exceptionMessage = UUID.randomUUID().toString()
        val faultErrorCode = UUID.randomUUID().toString()
        val faultMessage = UUID.randomUUID().toString()

        shouldThrow<FastlegeregisteretException> {
            buildClient(
                mockk {
                    every { getPatientGPDetails(any()) } throws IFlrReadOperationsGetPatientGPDetailsGenericFaultFaultFaultMessage(
                        exceptionMessage,
                        GenericFault().apply {
                            errorCode = buildJAXBElement(faultErrorCode)
                            message = buildJAXBElement(faultMessage)
                        })
                })
                .getPatientGP(UUID.randomUUID().toString())
        }.asClue {
            it.errorCode shouldBe faultErrorCode
            it.faultMessage shouldBe faultMessage
            it.message shouldBe exceptionMessage
        }
    }


})

private fun buildClient(service: IFlrReadOperations) = FastlegeregisteretClient(Environment(""), Credentials("", ""), service)

private fun setupServiceMock(expected: PatientToGPContractAssociation?) = mockk<IFlrReadOperations> {
    every { getPatientGPDetails(any()) } returns expected
}

private fun buildPatientGP(
    patientNIN: String? = UUID.randomUUID().toString(),
    gpHerId: Int? = nextInt(1, 100000),
) = PatientToGPContractAssociation().apply {
    this.patientNIN = buildJAXBElement(patientNIN)
    this.gpHerId = buildJAXBElement(gpHerId)
}

private inline fun <reified T> buildJAXBElement(value: T) = JAXBElement(QName.valueOf("field"), T::class.java, value)
