package com.toasterofbread.spmp.model.mediaitem.loader

import com.toasterofbread.spmp.api.getOrThrowHere
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.locks.ReentrantLock


internal suspend inline fun <K, V> performResultSafeLoad(
    instance_key: K,
    lock: ReentrantLock,
    running: MutableMap<K, Deferred<V>>,
    keep_results: Boolean = false,
    crossinline performLoad: suspend () -> Result<V>
): Result<V> =
    performSafeLoad(
        instance_key,
        lock,
        running,
        keep_results,
        performLoad = {
            performLoad().getOrThrowHere()
        }
    )

internal suspend inline fun <K, V> performSafeLoad(
    instance_key: K,
    lock: ReentrantLock,
    running: MutableMap<K, Deferred<V>>,
    keep_results: Boolean = false,
    crossinline performLoad: suspend () -> V
): Result<V> {
    val end_with_lock = lock.isHeldByCurrentThread
    lock.lock()

    val loading = running[instance_key]
    if (loading != null) {
        lock.unlock()

        return runCatching {
            loading.await()
        }
    }

    val load_job: Deferred<V> = coroutineScope {
        async {
            performLoad()
        }
    }

    running[instance_key] = load_job

    lock.unlock()

    val result = runCatching {
        load_job.await()
    }

    if (!keep_results || result.isFailure) {
        lock.lock()

        @Suppress("DeferredResultUnused")
        running.remove(instance_key)

        if (!end_with_lock) {
            lock.unlock()
        }
    }

    return result
}
