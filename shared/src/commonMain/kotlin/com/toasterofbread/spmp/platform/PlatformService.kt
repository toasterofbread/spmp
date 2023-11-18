package com.toasterofbread.spmp.platform

expect abstract class PlatformBinder()

interface PlatformService {
    val context: AppContext

    fun onCreate()
    fun onDestroy()

    fun onBind(): PlatformBinder?

    fun sendMessageOut(data: Any?)
    fun onMessage(data: Any?)

    fun addMessageReceiver(receiver: (Any?) -> Unit)
    fun removeMessageReceiver(receiver: (Any?) -> Unit)
}

expect class PlatformServiceImpl: PlatformService {
    override val context: AppContext

    override fun onCreate()
    override fun onDestroy()

    override fun onBind(): PlatformBinder?

    override fun sendMessageOut(data: Any?)
    override fun onMessage(data: Any?)

    override fun addMessageReceiver(receiver: (Any?) -> Unit)
    override fun removeMessageReceiver(receiver: (Any?) -> Unit)
}

expect fun startPlatformService(
    context: AppContext,
    cls: Class<out PlatformServiceImpl>,
    onConnected: ((binder: PlatformBinder?) -> Unit)? = null,
    onDisconnected: (() -> Unit)? = null
): Any // Service connection

expect fun unbindPlatformService(context: AppContext, connection: Any)
