package com.spectre7.spmp.platform

expect abstract class PlatformBinder()

interface PlatformService {
    val context: PlatformContext

    fun onCreate()
    fun onDestroy()

    fun onBind(): PlatformBinder?

    fun sendMessageOut(data: Any?)
    fun onMessage(data: Any?)

    fun addMessageReceiver(receiver: (Any?) -> Unit)
    fun removeMessageReceiver(receiver: (Any?) -> Unit)
}

expect class PlatformServiceImpl: PlatformService {
    override val context: PlatformContext

    override fun onCreate()
    override fun onDestroy()

    override fun onBind(): PlatformBinder?

    override fun sendMessageOut(data: Any?)
    override fun onMessage(data: Any?)

    override fun addMessageReceiver(receiver: (Any?) -> Unit)
    override fun removeMessageReceiver(receiver: (Any?) -> Unit)
}


expect fun startPlatformService(
    context: PlatformContext,
    cls: Class<out PlatformService>,
    onConnected: ((binder: PlatformBinder?) -> Unit)? = null,
    onDisconnected: (() -> Unit)? = null
): Any // Service connection

expect fun unbindPlatformService(context: PlatformContext, connection: Any)
