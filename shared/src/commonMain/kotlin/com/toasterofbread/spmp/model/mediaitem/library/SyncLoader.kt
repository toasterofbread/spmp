package com.toasterofbread.spmp.model.mediaitem.library

import androidx.compose.runtime.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.CoroutineStart
import com.toasterofbread.spmp.platform.AppContext

internal abstract class SyncLoader<T> {
    var synced: Map<String, T>? by mutableStateOf(null)
        private set
    var sync_in_progress: Boolean by mutableStateOf(false)
        private set

    private val coroutine_scope: CoroutineScope = CoroutineScope(Job())
    private val lock: Mutex = Mutex()
    private var sync_job: Deferred<Map<String, T>>? = null

    protected abstract suspend fun internalPerformSync(context: AppContext): Map<String, T>

    suspend fun put(key: String, value: T): Boolean {
        lock.withLock {
            sync_job?.also { job ->
                if (job.isActive) {
                    return false
                }
            }

            synced = synced?.toMutableMap()?.apply {
                put(key, value)
            }
        }

        return true
    }

    suspend fun remove(key: String) {
        lock.withLock {
            sync_job?.also { job ->
                if (job.isActive) {
                    return
                }
            }

            synced = synced?.toMutableMap()?.apply {
                remove(key)
            }
        }
    }

    suspend fun performSync(context: AppContext, skip_if_synced: Boolean = false): Map<String, T> =
        coroutine_scope.async {
            return@async coroutineScope {
                val loader: Deferred<Map<String, T>>
                lock.withLock {
                    if (skip_if_synced) {
                        synced?.also {
                            return@coroutineScope it
                        }
                    }

                    sync_job?.also { job ->
                        if (job.isActive) {
                            loader = job
                            return@withLock
                        }
                    }

                    loader = async(start = CoroutineStart.LAZY) {
                        try {
                            sync_in_progress = true

                            val sync_result: Map<String, T> = internalPerformSync(context)
                            synced = sync_result

                            return@async sync_result
                        }
                        finally {
                            sync_in_progress = false
                        }
                    }
                    sync_job = loader
                }

                loader.start()
                return@coroutineScope loader.await()
            }
        }.await()
}
