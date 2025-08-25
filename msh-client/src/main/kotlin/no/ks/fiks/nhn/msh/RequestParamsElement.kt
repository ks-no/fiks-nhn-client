package no.ks.fiks.nhn.msh

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.withContext

class RequestParamsElement(
    private val params: RequestParameters?
) : ThreadContextElement<RequestParameters?>, AbstractCoroutineContextElement(Key) {

    companion object Key : CoroutineContext.Key<RequestParamsElement>

    override fun updateThreadContext(ctx: CoroutineContext): RequestParameters? {
        val prev = RequestContextHolder.get()
        RequestContextHolder.set(params)
        return prev
    }

    override fun restoreThreadContext(ctx: CoroutineContext, oldState: RequestParameters?) {
        if (oldState == null) RequestContextHolder.clear() else RequestContextHolder.set(oldState)
    }
}

// 2) Helpers for both worlds
suspend inline fun <T> withRequestParams(
    params: RequestParameters?,
    crossinline block: suspend () -> T
): T = withContext(RequestParamsElement(params)) { block() }

inline fun <T> withRequestParamsBlocking(
    params: RequestParameters?,
    block: () -> T
): T =
    withBlocking(params) {
        block()
    }

