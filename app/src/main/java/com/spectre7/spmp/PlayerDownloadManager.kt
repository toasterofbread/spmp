package com.spectre7.spmp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.*
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.spectre7.spmp.model.Settings
import com.spectre7.spmp.model.Song
import com.spectre7.utils.createNotification
import com.spectre7.utils.getString
import com.spectre7.utils.networkThread
import java.io.File

private const val ERROR_NOTIFICATION_CHANNEL_ID = "download_error_channel"

fun <T> Intent.getExtra(key: String): T {
    return extras!!.get(key) as T
}

class PlayerDownloadManager(private val context: Context) {

    var service: PlayerDownloadService? = null
    var service_connecting = false
    private var service_intent: Intent? = null
    private var service_connection: ServiceConnection? = null

    private var result_callback_id: Int = 0
    private val result_callbacks: MutableMap<PlayerDownloadService.IntentAction, MutableMap<String, MutableMap<Int, (Intent) -> Unit>>> = mutableMapOf()
    private val result_broadcast_receiver = object : BroadcastReceiver() {
        override fun onReceive(_context: Context, intent: Intent) {
            val action = intent.extras?.get("action") as PlayerDownloadService.IntentAction
            onResultIntentReceived(action, intent)
        }
    }

    abstract class DownloadStatusListener {
        abstract fun onSongDownloadStatusChanged(song_id: String, status: PlayerDownloadService.DownloadStatus)
    }
    private val download_status_listeners: MutableList<DownloadStatusListener> = mutableListOf()

    private val downloaded_songs: MutableList<Song> = mutableStateListOf()

    init {
        val files = getDownloadDir(context).listFiles()
        if (files != null) {
            for (file in files) {
                onSongDownloaded(PlayerDownloadService.getFilenameSong(file.name))
            }
        }
    }

    private fun onSongDownloaded(song: Song) {
        if (!downloaded_songs.contains(song)) {
            downloaded_songs.add(song)
        }
    }

    private fun onSongDeleted(song_id: String) {
        downloaded_songs.removeIf { it.id == song_id }
    }

    companion object {
        fun getDownloadDir(context: Context): File {
            return File(context.filesDir, "download")
        }
    }

    fun addDownloadStatusListener(listener: DownloadStatusListener) {
        download_status_listeners.add(listener)
    }
    fun removeDownloadStatusListener(listener: DownloadStatusListener) {
        download_status_listeners.remove(listener)
    }

    private fun onResultIntentReceived(action: PlayerDownloadService.IntentAction, intent: Intent) {
        val song_id: String = intent.getExtra("song_id")

        val instance: Int? = intent.getExtra("instance")
        if (instance != null) {
            result_callbacks[action]?.get(song_id)?.remove(instance)?.invoke(intent)
        }

        when (action) {
            PlayerDownloadService.IntentAction.START_DOWNLOAD -> {
                val result = intent.extras!!.get("result") as Result<File?>
                if (result.isFailure) {
                    NotificationManagerCompat.from(context).notify(
                        System.currentTimeMillis().toInt(),
                        result.exceptionOrNull()!!.createNotification(context, getErrorNotificationChannel())
                    )
                }

                if (result.getOrNull() == null) {
                    onSongDeleted(song_id)
                }
            }
            PlayerDownloadService.IntentAction.STATUS_CHANGED -> {
                download_status_listeners.forEach { it.onSongDownloadStatusChanged(song_id, intent.getExtra("status")) }
            }
            else -> {}
        }
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

    private fun addResultCallback(action: PlayerDownloadService.IntentAction, song_id: String, instance: Int, callback: (Intent) -> Unit) {
        val callbacks = result_callbacks.getOrPut(action) { mutableMapOf() }.getOrPut(song_id) { mutableMapOf() }
        callbacks[instance] = callback
    }

    fun getDownloadedSong(song: Song): File? {
        val files = getDownloadDir(context).listFiles() ?: return null
        for (file in files) {
            if (PlayerDownloadService.fileMatchesDownload(file.name, song.id, song.getTargetDownloadQuality()) == true) {
                return file
            }
        }
        return null
    }

    @Synchronized
    fun startDownload(song_id: String, onCompleted: ((File?, PlayerDownloadService.DownloadStatus) -> Unit)? = null) {
        if (service == null) {
            startService({ startDownload(song_id, onCompleted) })
            return
        }

        val instance = result_callback_id++
        if (onCompleted != null) {
            addResultCallback(PlayerDownloadService.IntentAction.START_DOWNLOAD, song_id, instance) { intent ->
                val result = intent.extras!!.get("result") as Result<File?>
                onCompleted(result.getOrNull(), intent.extras!!.get("status") as PlayerDownloadService.DownloadStatus)
            }
        }

        val intent = Intent(PlayerDownloadService::class.java.canonicalName)
        intent.putExtra("action", PlayerDownloadService.IntentAction.START_DOWNLOAD)
        intent.putExtra("song_id", song_id)
        intent.putExtra("instance", instance)

        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)

        onSongDownloaded(Song.fromId(song_id))
    }

    fun getSongDownloadStatus(song_id: String, callback: (PlayerDownloadService.DownloadStatus) -> Unit) {
        if (service == null) {
            startService({ getSongDownloadStatus(song_id, callback) })
            return
        }

        networkThread {
            callback(service!!.getDownloadStatus(song_id))
        }
    }

    fun getSongDownloadProgress(song_id: String, callback: (Float) -> Unit) {
        if (service == null) {
            startService({ getSongDownloadProgress(song_id, callback) })
        }

        networkThread {
            callback(service!!.getDownloadProgress(song_id))
        }
    }

    @Synchronized
    private fun startService(onConnected: (() -> Unit)? = null, onDisconnected: (() -> Unit)? = null) {
        if (service_connecting || service != null) {
            return
        }
        service_connecting = true

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

        context.startService(service_intent)
        context.bindService(service_intent, service_connection!!, 0)
    }

    fun release() {
        if (service_connection != null) {
            context.unbindService(service_connection!!)
            service_connection = null
            service_intent = null
        }
    }
}
