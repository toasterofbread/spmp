package com.spectre7.spmp.platform

import android.app.Service
import android.content.*
import android.os.Binder
import android.os.IBinder
import android.os.Parcelable
import androidx.localbroadcastmanager.content.LocalBroadcastManager

actual abstract class PlatformBinder: Binder()

actual open class PlatformServiceImpl: Service(), PlatformService {
    actual override val context: PlatformContext get() = PlatformContext(this)
    actual override fun onCreate() {
        super.onCreate()
    }
    actual override fun onDestroy() {
        super.onDestroy()
    }
    actual override fun onBind(): PlatformBinder? = null
    override fun onBind(p0: Intent?): IBinder? = onBind()
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }
    private val message_receivers: MutableList<(Any?) -> Unit> = mutableListOf()
    actual override fun sendMessageOut(data: Any?) {
        message_receivers.forEach { it(data) }
    }
    actual override fun onMessage(data: Any?) {}
    actual override fun addMessageReceiver(receiver: (Any?) -> Unit) {
        message_receivers.add(receiver)
    }
    actual override fun removeMessageReceiver(receiver: (Any?) -> Unit) {
        message_receivers.remove(receiver)
    }
}

actual fun startPlatformService(
    context: PlatformContext,
    cls: Class<out PlatformService>,
    onConnected: ((binder: PlatformBinder?) -> Unit)?,
    onDisconnected: (() -> Unit)?
): Any {
    val ctx = context.ctx

    val service_intent = Intent(ctx, cls)
    val service_connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder?) {
            onConnected?.invoke(binder as PlatformBinder?)
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            onDisconnected?.invoke()
        }
    }

    ctx.startService(service_intent)
    ctx.bindService(service_intent, service_connection, 0)

    return service_connection
}

actual fun unbindPlatformService(context: PlatformContext, connection: Any) {
    context.ctx.unbindService(connection as ServiceConnection)
}