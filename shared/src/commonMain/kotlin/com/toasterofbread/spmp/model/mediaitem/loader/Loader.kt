package com.toasterofbread.spmp.model.mediaitem.loader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal abstract class ListenerLoader<K, V>: BasicLoader<K, V>() {
    abstract val listeners: MutableList<Listener<K, V>>

    fun addListener(listener: Listener<K, V>) { listeners.add(listener) }
    fun removeListener(listener: Listener<K, V>) { listeners.remove(listener) }

    override suspend fun performLoad(key: K, performLoad: suspend () -> Result<V>): Result<V> =
        performSafeLoad(key, lock, loading_items, listeners = listeners, performLoad = performLoad)

    interface Listener<K, V> {
        fun onLoadStarted(key: K)
        fun onLoadFinished(key: K, value: V)
        fun onLoadFailed(key: K, error: Throwable)
    }
}

internal abstract class ItemStateLoader<K, V>: ListenerLoader<K, V>() {
    override val listeners: MutableList<Listener<K, V>> = mutableListOf()

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
            val listener = object : Listener<K, V> {
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
    protected val lock = ReentrantLock()

    protected val loading_items: MutableMap<K, Deferred<Result<V>>> = mutableMapOf()

    protected open suspend fun performLoad(key: K, performLoad: suspend () -> Result<V>): Result<V> =
        performSafeLoad(key, lock, loading_items, performLoad = performLoad)
}

internal suspend inline fun <K, V> performSafeLoad(
    instance_key: K,
    lock: ReentrantLock,
    running: MutableMap<K, Deferred<Result<V>>>,
    keep_results: Boolean = false,
    listeners: List<ListenerLoader.Listener<K, V>>? = null,
    crossinline performLoad: suspend () -> Result<V>
): Result<V> {
    val loading = lock.withLock {
        running[instance_key]
    }

    if (loading != null) {
        return loading.await()
    }

    if (listeners != null) {
        for (listener in listeners) {
            listener.onLoadStarted(instance_key)
        }
    }

    val load_job: Deferred<Result<V>> = coroutineScope {
        async {
            performLoad()
        }
    }

    lock.withLock {
        running[instance_key] = load_job
    }

    val result = load_job.await()

    if (listeners != null) {
        result.fold(
            { value ->
                for (listener in listeners) {
                    listener.onLoadFinished(instance_key, value)
                }
            },
            { error ->
                for (listener in listeners) {
                    listener.onLoadFailed(instance_key, error)
                }
            }
        )
    }

    if (!keep_results || result.isFailure) {
        @Suppress("DeferredResultUnused")
        lock.withLock {
            running.remove(instance_key)
        }
    }

    return result
}
