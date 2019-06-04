package net.slash_omega.juktaway.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

fun CoroutineScope.regenerableLaunch(
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit
) = RegenerableCoroutine(this, context, start, block)

class RegenerableCoroutine(
        private val parent: CoroutineScope,
        private val context: CoroutineContext = EmptyCoroutineContext,
        private val start: CoroutineStart = CoroutineStart.DEFAULT,
        private val block: suspend CoroutineScope.() -> Unit
) {
    private var job = parent.launch(context, start, block)

    fun start() = job.start()

    fun cancel() = job.cancel()

    fun restart() {
        if (!job.isCancelled) job.cancel()
        job = parent.launch(context, start, block)
    }
}