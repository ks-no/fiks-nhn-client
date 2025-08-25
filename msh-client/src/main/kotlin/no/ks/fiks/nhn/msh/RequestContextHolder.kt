package no.ks.fiks.nhn.msh

import kotlin.use
import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.withContext
import no.ks.fiks.nhn.msh.RequestContextHolder.asContextElement

object RequestContextHolder {

    private val context = ThreadLocal<RequestParameters?>()

    fun get(): RequestParameters? = context.get()

    fun set(params: RequestParameters?) {
        if (params == null) context.remove() else context.set(params)
    }


    fun with(params: RequestParameters?) =
        ContextScope()
            .also { context.set(params) }

    fun clear() {
        context.remove()
    }

    fun asContextElement(params: RequestParameters?): ThreadContextElement<RequestParameters?> =
        context.asContextElement(params)

    class ContextScope : AutoCloseable {
        override fun close() {
            clear()
        }
    }

}

inline fun <T> withBlocking(params: RequestParameters?, block: () -> T): T =
    RequestContextHolder.with(params).use { block() }

suspend inline fun <T> withCoroutine(params: RequestParameters?, crossinline block: suspend () -> T): T =
    withContext(asContextElement(params)) { block() }