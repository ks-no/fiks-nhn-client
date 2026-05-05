package no.ks.fiks.nhn.msh

import java.util.*

/**
 * Java-friendly base class for handling incoming messages.
 *
 * Extend this class and override only the methods you are interested in.
 * All methods have empty default implementations.
 *
 * This class implements [MessageHandler] and can be passed directly to [Client] or [ClientFactory].
 *
 * Example (Java):
 * ```java
 * ClientFactory.createClient(config, List.of(new JavaMessageHandler() {
 *     @Override
 *     public void onIncomingBusinessDocumentReceived(UUID id, String rawXml, IncomingBusinessDocument document) {
 *         logger.info("Received document " + id);
 *     }
 *     @Override
 *     public void onIncomingBusinessDocumentDeserializationFailed(UUID id, String rawXml, Throwable exception) {
 *         db.saveRaw(id, rawXml);
 *     }
 * }));
 * ```
 */
abstract class JavaMessageHandler : MessageHandler {

    open fun onIncomingBusinessDocumentReceived(id: UUID, rawXml: String, document: IncomingBusinessDocument) {}
    open fun onIncomingBusinessDocumentDeserializationFailed(id: UUID, rawXml: String, exception: Throwable) {}

    open fun onIncomingApplicationReceiptReceived(id: UUID, rawXml: String, receipt: IncomingApplicationReceipt) {}
    open fun onIncomingApplicationReceiptDeserializationFailed(id: UUID, rawXml: String, exception: Throwable) {}

    final override suspend fun onIncomingBusinessDocument(id: UUID, rawXml: String, result: Result<IncomingBusinessDocument>) {
        result
            .onSuccess { onIncomingBusinessDocumentReceived(id, rawXml, it) }
            .onFailure { onIncomingBusinessDocumentDeserializationFailed(id, rawXml, it) }
    }

    final override suspend fun onIncomingApplicationReceipt(id: UUID, rawXml: String, result: Result<IncomingApplicationReceipt>) {
        result
            .onSuccess { onIncomingApplicationReceiptReceived(id, rawXml, it) }
            .onFailure { onIncomingApplicationReceiptDeserializationFailed(id, rawXml, it) }
    }
}

