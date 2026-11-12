package com.motovault.app.ui

import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

/** Controlled Google Tasks test resource ownership, not fabricated OCR recognition. */
internal suspend fun checkOdometerTaskLifetime() {
    cancelledOwner { it.setResult("synthetic SDK completion") }
    cancelledOwner { it.setException(IllegalStateException()) }
    val failed = runCatching { awaitOdometerTask(Tasks.forException<String>(IllegalStateException())) }
    check(failed.exceptionOrNull() is IllegalStateException)
    val token = CancellationTokenSource()
    val cancelled = TaskCompletionSource<String>(token.token)
    token.cancel()
    val sdkCancelled = runCatching { awaitOdometerTask(cancelled.task) }
    check(sdkCancelled.exceptionOrNull() is IllegalStateException)
}

private suspend fun cancelledOwner(complete: (TaskCompletionSource<String>) -> Unit) = coroutineScope {
    val completion = TaskCompletionSource<String>()
    var released = false
    var delivered = false
    val owner = launch(start = CoroutineStart.UNDISPATCHED) {
        try {
            awaitOdometerTask(completion.task)
            delivered = true
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { delivered = true }
        finally { released = true }
    }
    owner.cancel()
    yield()
    check(!released) { "Input released while the SDK task was still pending" }
    complete(completion)
    owner.join()
    check(released && !delivered && owner.isCancelled)
}
