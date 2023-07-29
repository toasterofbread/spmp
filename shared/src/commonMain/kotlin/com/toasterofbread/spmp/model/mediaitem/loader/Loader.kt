package com.toasterofbread.spmp.model.mediaitem.loader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.toasterofbread.spmp.api.getOrThrowHere
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal interface ListenerLoader<K, V> {
    val listeners: MutableList<Listener<K, V>>

    fun addListener(listener: Listener<K, V>) { listeners.add(listener) }
    fun removeListener(listener: Listener<K, V>) { listeners.remove(listener) }

    interface Listener<K, V> {
        fun onLoadStarted(key: K)
        fun onLoadFinished(key: K, value: V)
        fun onLoadFailed(key: K, error: Throwable)
    }
}

internal abstract class ItemStateLoader<K, V>: BasicLoader<K, V>(), ListenerLoader<K, V> {
    override val listeners: MutableList<ListenerLoader.Listener<K, V>> = mutableListOf()

    interface ItemState<V> {
        val loading: Boolean
        val value: V?
    }

    @Composable
    fun rememberItemState(state_key: K): ItemState<V> {
        val state = remember(state_key) {
            object : ItemState<V> {
                override var loading: Boolean by mutableStateOf(false)
                override var value: V? by mutableStateOf(null)
            }
        }

        DisposableEffect(state) {
            val listener = object : ListenerLoader.Listener<K, V> {
                override fun onLoadStarted(key: K) {
                    if (key != state_key) {
                        return
                    }

                    synchronized(state) {
                        state.loading = true
                    }
                }

                override fun onLoadFinished(key: K, value: V) {
                    if (key != state_key) {
                        return
                    }

                    synchronized(state) {
                        state.value = value
                        state.loading = false
                    }
                }

                override fun onLoadFailed(key: K, error: Throwable) {
                    if (key != state_key) {
                        return
                    }

                    synchronized(state) {
                        state.loading = false
                    }
                }
            }

            addListener(listener)
            onDispose {
                removeListener(listener)
            }
        }

        return state
    }
}

internal abstract class BasicLoader<K, V> {
    private val lock = ReentrantLock()
    inline fun <T> withLock(action: () -> T): T = lock.withLock(action)

    private val loading_items: MutableMap<K, Deferred<V>> = mutableMapOf()

    protected suspend inline fun performLoad(key: K, noinline performLoad: suspend () -> V): Result<V> =
        performSafeLoad(key, lock, loading_items, performLoad = performLoad)

    protected suspend inline fun performResultLoad(key: K, noinline performLoad: suspend () -> Result<V>): Result<V> =
        performResultSafeLoad(key, lock, loading_items, performLoad = performLoad)
}

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
