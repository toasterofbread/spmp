package com.spectre7.spmp.platform

expect open class PlatformService() {
    abstract class PlatformBinder()
    abstract class BroadcastReceiver() {
        abstract fun onReceive(data: Map<String, Any?>)
    }

    val context: PlatformContext

    open fun onCreate()
    open fun onDestroy()

    open fun onBind(): PlatformBinder? // Binder

    fun broadcast(data: Map<String, Any?>)
    fun addBroadcastReceiver(receiver: BroadcastReceiver)
    fun removeBroadcastReceiver(receiver: BroadcastReceiver)

    companion object {
        fun startService(
            context: PlatformContext,
            cls: Class<out PlatformService>,
            onConnected: ((binder: PlatformBinder?) -> Unit)? = null,
            onDisconnected: (() -> Unit)? = null
        ): Any // Service connection

        fun unbindService(context: PlatformContext, connection: Any)
    }
}
