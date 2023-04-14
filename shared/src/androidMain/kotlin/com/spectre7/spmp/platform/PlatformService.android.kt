package com.spectre7.spmp.platform

import android.app.Service
import android.content.*
import android.os.Binder
import android.os.IBinder
import android.os.Parcelable
import androidx.localbroadcastmanager.content.LocalBroadcastManager

actual open class PlatformService: Service() {

    actual abstract class PlatformBinder: Binder()
    actual abstract class BroadcastReceiver: android.content.BroadcastReceiver() {
        actual abstract fun onReceive(data: Map<String, Any?>)

        override fun onReceive(context: Context, intent: Intent) {
            val data = mutableMapOf<String, Any>()
            intent.extras?.also { extras ->
                for (extra in extras.keySet()) {
                    data[extra] = extras[extra] as Any
                }
            }
            onReceive(data)
        }

    }

    actual val context: PlatformContext get() = PlatformContext(this)

    actual override fun onCreate() {
        super.onCreate()
    }
    actual override fun onDestroy() {
        super.onDestroy()
    }

    actual open fun onBind(): PlatformBinder? = null
    override fun onBind(p0: Intent?): IBinder? = onBind()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    actual fun broadcast(action: String, data: Map<String, Any?>) {
        val intent = Intent(action)
        for (entry in data.entries) {
            when (val value = entry.value) {
                is Boolean -> intent.putExtra(entry.key, value)
                is Byte -> intent.putExtra(entry.key, value)
                is Char -> intent.putExtra(entry.key, value)
                is Short -> intent.putExtra(entry.key, value)
                is Int -> intent.putExtra(entry.key, value)
                is Long -> intent.putExtra(entry.key, value)
                is Float -> intent.putExtra(entry.key, value)
                is Double -> intent.putExtra(entry.key, value)
                is String -> intent.putExtra(entry.key, value)
                is CharSequence -> intent.putExtra(entry.key, value)
                is Parcelable -> intent.putExtra(entry.key, value)
                null -> intent.removeExtra(entry.key)
                else -> throw NotImplementedError(value::class.java.name)
            }
        }
        LocalBroadcastManager.getInstance(context.ctx).sendBroadcast(intent)
    }

    actual fun addBroadcastReceiver(receiver: BroadcastReceiver, action: String) {
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, IntentFilter(action))
    }
    actual fun removeBroadcastReceiver(receiver: BroadcastReceiver) {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
    }

    actual companion object {
        actual fun startService(
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

        actual fun unbindService(context: PlatformContext, connection: Any) {
            context.ctx.unbindService(connection as ServiceConnection)
        }
    }
}