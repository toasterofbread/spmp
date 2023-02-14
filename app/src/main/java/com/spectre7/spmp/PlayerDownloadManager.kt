package com.spectre7.spmp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.*
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.spectre7.spmp.model.Song
import com.spectre7.utils.createNotification
import com.spectre7.utils.getString
import java.io.File

private const val ERROR_NOTIFICATION_CHANNEL_ID = "download_error_channel"

class PlayerDownloadManager(private val context: Context) {

    var service: PlayerDownloadService? = null
    var service_connecting = false
    private var service_intent: Intent? = null
    private var service_connection: ServiceConnection? = null

    private var result_callback_id: Int = 0
    private val download_start_callbacks: MutableMap<String, MutableMap<Int, (File?, PlayerDownloadService.Download.Status) -> Unit>> = mutableMapOf()
    private val result_broadcast_receiver = object : BroadcastReceiver() {
        override fun onReceive(_context: Context, intent: Intent) {
            val action = intent.extras?.get("action") as PlayerDownloadService.ResultIntentAction
            onResultIntentReceived(action, intent)
        }
    }

    private fun onResultIntentReceived(action: PlayerDownloadService.ResultIntentAction, intent: Intent) {
        when (action) {
            PlayerDownloadService.ResultIntentAction.DOWNLOAD_RESULT -> {
                onDownloadCompleted(
                    intent.extras!!.get("status") as PlayerDownloadService.Download.Status,
                    intent.extras!!.get("result") as Result<File?>,
                    intent.getStringExtra("song_id")!!,
                    intent.extras!!.get("instance") as Int
                )
            }
        }
    }

    private fun onDownloadCompleted(status: PlayerDownloadService.Download.Status, result: Result<File?>, song_id: String, instance: Int) {
        if (result.isFailure) {
            NotificationManagerCompat.from(context).notify(
                System.currentTimeMillis().toInt(),
                result.exceptionOrNull()!!.createNotification(context, getErrorNotificationChannel())
            )
            return
        }

        download_start_callbacks[song_id]?.remove(instance)?.invoke(result.getOrNull(), status)
    }

    private fun getErrorNotificationChannel(): String {
        val channel = NotificationChannel(
            ERROR_NOTIFICATION_CHANNEL_ID,
            getString(R.string.download_service_error_name),
            NotificationManager.IMPORTANCE_HIGH
        )

        NotificationManagerCompat.from(context).createNotificationChannel(channel)
        return ERROR_NOTIFICATION_CHANNEL_ID
    }

    companion object {
        fun getDownloadDir(context: Context): File {
            return File(context.filesDir, "download")
        }
    }

    fun getDownloadedSong(song: Song): File? {
        val files = getDownloadDir(context).listFiles() ?: return null
        return files.firstOrNull { it.nameWithoutExtension == song.id }
    }

//    @Synchronized
    fun startDownload(song: Song, onCompleted: ((File?, PlayerDownloadService.Download.Status) -> Unit)? = null) {
        if (service == null) {
            startService({ startDownload(song, onCompleted) })
            return
        }

        val instance = result_callback_id++
        if (onCompleted != null) {
            val callbacks = download_start_callbacks.getOrPut(song.id) { mutableMapOf() }
            callbacks[instance] = onCompleted
        }

        val intent = Intent(PlayerDownloadService::class.java.canonicalName)
        intent.putExtra("action", PlayerDownloadService.IntentAction.START_DOWNLOAD)
        intent.putExtra("song_id", song.id)
        intent.putExtra("instance", instance)

        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    fun release() {
        if (service_connection != null) {
            context.unbindService(service_connection!!)
            service_connection = null
            service_intent = null
        }
    }

    @Synchronized
    private fun startService(onConnected: (() -> Unit)? = null, onDisconnected: (() -> Unit)? = null) {
        if (service_connecting || service != null) {
            return
        }
        service_connecting = true

        if (service_intent == null) {
            service_intent = Intent(context, PlayerDownloadService::class.java)
            service_connection = object : ServiceConnection {
                override fun onServiceConnected(className: ComponentName, binder: IBinder) {
                    service = (binder as PlayerDownloadService.ServiceBinder).getService()
                    service_connecting = false
                    LocalBroadcastManager.getInstance(context).registerReceiver(result_broadcast_receiver, IntentFilter(PlayerDownloadService.RESULT_INTENT_ACTION))
                    onConnected?.invoke()
                }

                override fun onServiceDisconnected(arg0: ComponentName) {
                    service = null
                    service_connecting = false
                    LocalBroadcastManager.getInstance(context).unregisterReceiver(result_broadcast_receiver)
                    onDisconnected?.invoke()
                }
            }
        }

        context.startService(service_intent)
        context.bindService(service_intent, service_connection!!, 0)
    }
}
