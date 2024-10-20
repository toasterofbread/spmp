package com.toasterofbread.spmp.platform

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking

actual abstract class PlatformBinder: Binder()

actual open class PlatformServiceImpl: Service(), PlatformService {
    private val coroutine_scope = CoroutineScope(Job())

    actual override val context: AppContext by lazy { runBlocking {
        AppContext.create(this@PlatformServiceImpl, coroutine_scope) }
    }
    actual override fun onCreate() {
        super.onCreate()
    }
    actual override fun onDestroy() {
        coroutine_scope.cancel()
        super.onDestroy()
    }
    actual override fun onBind(): PlatformBinder? = null
    override fun onBind(p0: Intent?): IBinder? = onBind()
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
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

actual inline fun <reified T: PlatformServiceImpl> startPlatformService(
    context: AppContext,
    createInstance: () -> T,
    crossinline onConnected: (binder: PlatformBinder?) -> Unit,
    crossinline onDisconnected: () -> Unit
): Any {
    val ctx: Context = context.ctx

    val service_intent: Intent = Intent(ctx, T::class.java)
    val service_connection: ServiceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder?) {
                onConnected(binder as PlatformBinder?)
            }

            override fun onServiceDisconnected(arg0: ComponentName) {
                onDisconnected()
            }
        }

    ctx.startService(service_intent)
    ctx.bindService(service_intent, service_connection, 0)

    return service_connection
}

actual fun unbindPlatformService(context: AppContext, connection: Any) {
    context.ctx.unbindService(connection as ServiceConnection)
}
