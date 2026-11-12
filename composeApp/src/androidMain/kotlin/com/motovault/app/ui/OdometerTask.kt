package com.motovault.app.ui

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Native processing still owns its input after the caller cancels. Await its terminal signal before releasing it. */
internal suspend fun <T> awaitOdometerTask(task: Task<T>): T {
    val outcome = withContext(NonCancellable) {
        runCatching {
            suspendCancellableCoroutine<T> { continuation ->
                task.addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
                    .addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
                    .addOnCanceledListener {
                        if (continuation.isActive) continuation.resumeWithException(IllegalStateException())
                    }
            }
        }
    }
    currentCoroutineContext().ensureActive()
    return outcome.getOrThrow()
}
