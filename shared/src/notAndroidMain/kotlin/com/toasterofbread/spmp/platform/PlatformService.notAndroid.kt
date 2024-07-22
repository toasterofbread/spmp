package com.toasterofbread.spmp.platform

import kotlin.reflect.KClass

actual abstract class PlatformBinder

actual open class PlatformServiceImpl: PlatformService {
    private class ServiceConnection(val service: PlatformServiceImpl)
    private val connections: MutableList<ServiceConnection> = mutableListOf()
    fun getConnection(): Any {
        val connection: ServiceConnection = ServiceConnection(this)
        connections.add(connection)
        return connection
    }
    private fun removeConnection(conn: ServiceConnection): Boolean {
        require(conn.service == this)
        check(connections.remove(conn))
        return connections.isEmpty()
    }

    fun _init(context: AppContext) {
        _context = context
    }

    private lateinit var _context: AppContext
    actual override val context: AppContext get() = _context

    actual override fun onCreate() {}
    actual override fun onDestroy() {}
    actual override fun onBind(): PlatformBinder? = null

    companion object {
        val service_instances: MutableMap<KClass<out PlatformServiceImpl>, PlatformServiceImpl> = mutableMapOf()

        inline fun <reified T: PlatformServiceImpl> startService(
            context: AppContext,
            createInstance: () -> T,
            crossinline onConnected: (binder: PlatformBinder?) -> Unit,
            crossinline onDisconnected: () -> Unit
        ): Any {
            val service: PlatformServiceImpl = service_instances.getOrPut(T::class) {
                createInstance().also {
                    it._init(context)
                    it.onCreate()
                }
            }

            val binder = service.onBind()
            onConnected(binder)

            return service.getConnection()
        }

        internal fun unbindService(context: AppContext, connection: Any) {
            require(connection is ServiceConnection)
            if (connection.service.removeConnection(connection)) {
                connection.service.onDestroy()
            }
        }
    }

    actual override fun sendMessageOut(data: Any?) {
        TODO()
    }

    actual override fun onMessage(data: Any?) {
        TODO()
    }

    actual override fun addMessageReceiver(receiver: (Any?) -> Unit) {
        TODO()
    }

    actual override fun removeMessageReceiver(receiver: (Any?) -> Unit) {
        TODO()
    }
}

actual inline fun <reified T: PlatformServiceImpl> startPlatformService(
    context: AppContext,
    createInstance: () -> T,
    crossinline onConnected: (binder: PlatformBinder?) -> Unit,
    crossinline onDisconnected: () -> Unit,
): Any {
    return PlatformServiceImpl.startService(context, createInstance, onConnected, onDisconnected)
}

actual fun unbindPlatformService(context: AppContext, connection: Any) {
    return PlatformServiceImpl.unbindService(context, connection)
}
