package no.ks.fiks.nhn.msh

/**
 * Handler that can be injected into [Client] to observe incoming messages.
 *
 * Override only the methods you are interested in — all methods have empty default implementations.
 */
interface MessageHandler {

    /**
     * Called when the Base64-encoded payload of a business document could not be decoded.
     *
     * @param id the MSH message ID that was fetched
     * @param rawBusinessDocument the raw Base64-encoded payload that could not be decoded
     * @param exception the exception thrown during Base64 decoding
     */
    fun onIncomingBusinessDocumentDecodingFailed(id: String, rawBusinessDocument: String, exception: Throwable) {}

    /**
     * Called after a business document has been retrieved but deserialization failed.
     *
     * @param id the MSH message ID that was fetched
     * @param rawXml the raw XML that was received
     * @param exception the exception thrown during deserialization
     */
    fun onIncomingBusinessDocumentDeserializationFailed(id: String, rawXml: String, exception: Throwable) {}

    /**
     * Called after a business document has been retrieved and successfully deserialized.
     *
     * @param id the MSH message ID that was fetched
     * @param rawXml the raw XML that was received
     * @param document the successfully parsed [IncomingBusinessDocument]
     */
    fun onIncomingBusinessDocumentReceived(id: String, rawXml: String, document: IncomingBusinessDocument) {}

    /**
     * Called when the Base64-encoded payload of an application receipt could not be decoded.
     *
     * @param id the MSH message ID that was fetched
     * @param rawBusinessDocument the raw Base64-encoded payload that could not be decoded
     * @param exception the exception thrown during Base64 decoding
     */
    fun onIncomingApplicationReceiptDecodingFailed(id: String, rawBusinessDocument: String, exception: Throwable) {}

    /**
     * Called after an application receipt has been retrieved but deserialization failed.
     *
     * @param id the MSH message ID that was fetched
     * @param rawXml the raw XML that was received
     * @param exception the exception thrown during deserialization
     */
    fun onIncomingApplicationReceiptDeserializationFailed(id: String, rawXml: String, exception: Throwable) {}

    /**
     * Called after an application receipt has been retrieved and successfully deserialized.
     *
     * @param id the MSH message ID that was fetched
     * @param rawXml the raw XML that was received
     * @param receipt the successfully parsed [IncomingApplicationReceipt]
     */
    fun onIncomingApplicationReceiptReceived(id: String, rawXml: String, receipt: IncomingApplicationReceipt) {}
}
