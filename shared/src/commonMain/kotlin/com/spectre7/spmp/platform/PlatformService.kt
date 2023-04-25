package com.spectre7.spmp.platform

expect open class PlatformService() {
    abstract class PlatformBinder()

    val context: PlatformContext

    open fun onCreate()
    open fun onDestroy()

    open fun onBind(): PlatformBinder?

    protected fun sendMessageOut(data: Any?)
    open fun onMessage(data: Any?)

    fun addMessageReceiver(receiver: (Any?) -> Unit)
    fun removeMessageReceiver(receiver: (Any?) -> Unit)

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
