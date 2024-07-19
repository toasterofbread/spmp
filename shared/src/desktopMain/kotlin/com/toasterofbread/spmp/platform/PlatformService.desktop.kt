package com.toasterofbread.spmp.platform

import kotlin.reflect.KClass

actual open class PlatformServiceImpl: PlatformService {
    private class ServiceConnection(val service: PlatformServiceImpl)
    private val connections: MutableList<ServiceConnection> = mutableListOf()
    private fun getConnection(): ServiceConnection {
        val conn = ServiceConnection(this)
        connections.add(conn)
        return conn
    }
    private fun removeConnection(conn: ServiceConnection): Boolean {
        require(conn.service == this)
        check(connections.remove(conn))
        return connections.isEmpty()
    }

    private fun init(context: AppContext) {
        _context = context
    }

    private lateinit var _context: AppContext
    actual override val context: AppContext get() = _context

    actual override fun onCreate() {}
    actual override fun onDestroy() {}
    actual override fun onBind(): PlatformBinder? = null

    companion object {
        private val service_instances: MutableMap<KClass<out PlatformServiceImpl>, PlatformServiceImpl> = mutableMapOf()

        internal fun startService(
            context: AppContext,
            cls: KClass<out PlatformServiceImpl>,
            onConnected: ((binder: PlatformBinder?) -> Unit)?,
            onDisconnected: (() -> Unit)?
        ): Any {
            val service: PlatformServiceImpl = service_instances.getOrPut(cls) {
                cls.getDeclaredConstructor().newInstance().also {
                    it.init(context)
                    it.onCreate()
                }
            }

            val binder = service.onBind()
            onConnected?.invoke(binder)

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

actual fun startPlatformService(
    context: AppContext,
    cls: KClass<out PlatformServiceImpl>,
    onConnected: ((binder: PlatformBinder?) -> Unit)?,
    onDisconnected: (() -> Unit)?,
): Any {
    return PlatformServiceImpl.startService(context, cls, onConnected, onDisconnected)
}

actual fun unbindPlatformService(context: AppContext, connection: Any) {
    return PlatformServiceImpl.unbindService(context, connection)
}
