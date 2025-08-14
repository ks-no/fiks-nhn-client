package no.ks.fiks.nhn.msh

internal object RequestContextHolder {

    private val context = ThreadLocal<RequestParameters?>()

    fun get(): RequestParameters? = context.get()

    fun with(params: RequestParameters?) =
        ContextScope()
            .also { context.set(params) }

    fun clear() {
        context.remove()
    }

    class ContextScope : AutoCloseable {
        override fun close() {
            clear()
        }
    }

}