package no.ks.fiks.nhn.msh

import kotlin.collections.map
import no.nhn.msh.v2.model.Message as NhnMessage

open class AsyncClient( private val apiService: ApiService,) {

    suspend fun getMessages( receiverHerId: Int,
                                  requestParameters: RequestParameters? = null,): List<Message> =
        withRequestParams(requestParameters) {
            apiService.getMessages(receiverHerId).map {
                it.toMessageInfo() }
        }
}
