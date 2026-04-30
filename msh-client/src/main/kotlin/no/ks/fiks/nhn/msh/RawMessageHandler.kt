package no.ks.fiks.nhn.msh

import java.util.*

/**
 * Handler that can be injected into [Client] to observe incoming messages.
 *
 * Each method receives the raw XML and a [Result] indicating whether deserialization succeeded.
 * This makes it possible to store the raw XML for messages that cannot be deserialized,
 * while still having access to the parsed object when deserialization succeeds.
 *
 * Override only the methods you are interested in — all methods have empty default implementations.
 */
interface MessageHandler {

    /**
     * Called after a business document has been retrieved and deserialization has been attempted.
     *
     * @param id the MSH message ID that was fetched
     * @param rawXml the raw XML that was received
     * @param result the deserialization result — either a successfully parsed [IncomingBusinessDocument] or the exception that was thrown
     */
    suspend fun onIncomingBusinessDocument(id: UUID, rawXml: String, result: Result<IncomingBusinessDocument>) {}

    /**
     * Called after an application receipt has been retrieved and deserialization has been attempted.
     *
     * @param id the MSH message ID that was fetched
     * @param rawXml the raw XML that was received
     * @param result the deserialization result — either a successfully parsed [IncomingApplicationReceipt] or the exception that was thrown
     */
    suspend fun onIncomingApplicationReceipt(id: UUID, rawXml: String, result: Result<IncomingApplicationReceipt>) {}
}
