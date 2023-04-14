package com.spectre7.spmp.platform

actual open class PlatformService: PlatformContext() {
    actual abstract class PlatformBinder actual constructor()
    actual abstract class BroadcastReceiver actual constructor() {
        actual abstract fun onReceive(data: Map<String, Any?>)
    }

    actual val context: PlatformContext
        get() = TODO("Not yet implemented")

    actual open fun onCreate() {
    }

    actual open fun onDestroy() {
    }

    actual open fun onBind(): PlatformBinder? {
        TODO("Not yet implemented")
    }

    actual fun broadcast(action: String, data: Map<String, Any?>) {
    }

    actual fun addBroadcastReceiver(receiver: BroadcastReceiver, action: String) {
    }

    actual fun removeBroadcastReceiver(receiver: BroadcastReceiver) {
    }

    actual companion object {
        actual fun startService(
            context: PlatformContext,
            cls: Class<out PlatformService>,
            onConnected: ((binder: PlatformBinder?) -> Unit)?,
            onDisconnected: (() -> Unit)?
        ): Any {
            TODO("Not yet implemented")
        }

        actual fun unbindService(context: PlatformContext, connection: Any) {
        }

    }
}