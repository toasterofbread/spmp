package com.toasterofbread.spmp.model.mediaitem.loader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal abstract class ListenerLoader<K, V>: BasicLoader<K, V>() {
    protected abstract val listeners: MutableList<Listener<K, V>>

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

internal abstract class Loader<V> {
    protected val lock: ReentrantLock = ReentrantLock()

    abstract class LoadJob<V> {
        private var awaiting_count: Int = 0
        private val coroutine_scope: CoroutineScope = CoroutineScope(Job())
        private var job: Deferred<V>? = null

        abstract suspend fun performLoad(): V
        abstract fun onCancelled(cause: CancellationException)

        fun start() {
            check(job == null)

            // Prevent job being cancelled by parent cancellation
            job = coroutine_scope.async(NonCancellable) {
                performLoad()
            }
        }

        suspend fun await(): V {
            val current_job = job
            check(current_job != null)

            synchronized(coroutine_scope) {
                awaiting_count++
            }

            val result: V
            try {
                result = current_job.await()
            }
            catch (e: CancellationException) {
                val cancelled: Boolean
                synchronized(coroutine_scope) {
                    cancelled = --awaiting_count == 0
                    if (cancelled) {
                        current_job.cancel(e)
                    }
                }

                if (cancelled) {
                    onCancelled(e)
                }

                throw e
            }

            synchronized(coroutine_scope) {
                awaiting_count--
            }

            return result
        }
    }

    internal suspend inline fun <K> performSafeLoad(
        instance_key: K,
        running: MutableMap<K, LoadJob<Result<V>>>,
        listeners: List<ListenerLoader.Listener<K, in V>>? = null,
        crossinline performLoad: suspend () -> Result<V>
    ): Result<V> {
        return performSafeLoad(instance_key, lock, running, listeners, performLoad)
    }
}

internal abstract class BasicLoader<K, V>: Loader<V>() {
    protected val loading_items: MutableMap<K, LoadJob<Result<V>>> = mutableMapOf()

    protected open suspend fun performLoad(key: K, performLoad: suspend () -> Result<V>): Result<V> =
        performSafeLoad(key, lock, loading_items, performLoad = performLoad)
}

internal suspend inline fun <K, V> performSafeLoad(
    instance_key: K,
    lock: ReentrantLock,
    running: MutableMap<K, Loader.LoadJob<Result<V>>>,
    listeners: List<ListenerLoader.Listener<K, in V>>? = null,
    crossinline performLoad: suspend () -> Result<V>
): Result<V> {
    val loading: Loader.LoadJob<Result<V>>? = lock.withLock {
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

    val result = coroutineScope {
        val load_job = object : Loader.LoadJob<Result<V>>() {
            override suspend fun performLoad(): Result<V> {
                return performLoad()
            }

            override fun onCancelled(cause: CancellationException) {
                lock.withLock {
                    running.remove(instance_key)
                }

                if (listeners != null) {
                    for (listener in listeners) {
                        listener.onLoadFailed(instance_key, cause)
                    }
                }
            }
        }

        lock.withLock {
            running[instance_key] = load_job
            load_job.start()
        }

        load_job.await()
    }

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

    lock.withLock {
        running.remove(instance_key)
    }

    return result
}
