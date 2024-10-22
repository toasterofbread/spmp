package com.toasterofbread.spmp.platform.playerservice

import ProgramArguments
import android.app.ActivityManager
import android.app.ServiceStartNotAllowedException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.Build
import android.os.IBinder
import com.toasterofbread.spmp.platform.AppContext
import kotlin.reflect.KClass

internal class PlayerBinder(val service: ForegroundPlayerService): Binder()

abstract class InternalPlayerServiceCompanion(
    private val service_class: KClass<*>
): PlayerServiceCompanion {
    private fun AppContext.getAndroidContext(): Context =
        ctx.applicationContext

    override suspend fun connect(
        context: AppContext,
        launch_arguments: ProgramArguments,
        instance: PlayerService?,
        onConnected: (PlayerService) -> Unit,
        onDisconnected: () -> Unit
    ): Any {
        val ctx: Context = context.getAndroidContext()

        val service_connection: ServiceConnection =
            object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    onConnected((service as PlayerBinder).service)
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    onDisconnected()
                }
            }

        ctx.startService(Intent(ctx, service_class.java))
        ctx.bindService(Intent(ctx, service_class.java), service_connection, Context.BIND_AUTO_CREATE)

        return service_connection
    }

    override fun disconnect(context: AppContext, connection: Any) {
        context.getAndroidContext().unbindService(connection as ServiceConnection)
    }

    override fun isServiceRunning(context: AppContext): Boolean {
        val manager: ActivityManager = context.ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (service.service.className == service_class.java.name) {
                return true
            }
        }
        return false
    }

    override fun isServiceAttached(context: AppContext): Boolean = isServiceRunning(context)
}
