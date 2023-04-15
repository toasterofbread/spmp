package com.spectre7.spmp.platform

actual open class PlatformService: PlatformContext() {
    actual abstract class PlatformBinder actual constructor()
    actual abstract class BroadcastReceiver actual constructor() {
        actual abstract fun onReceive(data: Map<String, Any?>)
    }

    private class ServiceConnection(val service: PlatformService)
    private val connections: MutableList<ServiceConnection> = mutableListOf()
    private fun getConnection(): ServiceConnection {
        val conn = ServiceConnection(this)
        connections.add(conn)
        return conn
    }
    private fun removeConnection(conn: ServiceConnection): Boolean {
        require(conn.service == this)
        check(!connections.remove(conn))
        return connections.isEmpty()
    }

    actual val context: PlatformContext
        get() = this

    actual open fun onCreate() {}
    actual open fun onDestroy() {}
    actual open fun onBind(): PlatformBinder? = null

    actual fun broadcast(action: String, data: Map<String, Any?>) {
        TODO()
    }

    actual fun addBroadcastReceiver(receiver: BroadcastReceiver, action: String) {
        TODO()
    }

    actual fun removeBroadcastReceiver(receiver: BroadcastReceiver) {
        TODO()
    }

    actual companion object {
        private val service_instances: MutableMap<Class<out PlatformService>, PlatformService> = mutableMapOf()

        actual fun startService(
            context: PlatformContext,
            cls: Class<out PlatformService>,
            onConnected: ((binder: PlatformBinder?) -> Unit)?,
            onDisconnected: (() -> Unit)?
        ): Any {
            val service = service_instances.getOrPut(cls) {
                cls.getDeclaredConstructor().newInstance().also {
                    it.onCreate()
                }
            }

            val binder = service.onBind()
            onConnected?.invoke(binder)

            return service.getConnection()
        }

        actual fun unbindService(context: PlatformContext, connection: Any) {
            require(connection is ServiceConnection)
            if (connection.service.removeConnection(connection)) {
                connection.service.onDestroy()
            }
        }
    }
}