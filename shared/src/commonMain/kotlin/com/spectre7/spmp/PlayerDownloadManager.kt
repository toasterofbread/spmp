package com.spectre7.spmp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.platform.PlatformContext
import com.spectre7.spmp.platform.PlatformService
import java.io.File

//fun <T> Intent.getExtra(key: String): T {
//    return extras!!.get(key) as T
//}

class PlayerDownloadManager(private val context: PlatformContext) {

    var service: PlayerDownloadService? = null
    private var service_connecting = false
    private var service_connection: Any? = null

    private var result_callback_id: Int = 0
//    private val result_callbacks: MutableMap<PlayerDownloadService.IntentAction, MutableMap<String, MutableMap<Int, (Intent) -> Unit>>> = mutableMapOf()
//    private val result_broadcast_receiver = object : BroadcastReceiver() {
//        override fun onReceive(context: Context?, intent: Intent) {
//            val action = intent.extras?.get("action") as PlayerDownloadService.IntentAction
//            onResultIntentReceived(action, intent)
//        }
//    }

    abstract class DownloadStatusListener {
        abstract fun onSongDownloadStatusChanged(song_id: String, status: PlayerDownloadService.DownloadStatus)
    }
    private val download_status_listeners: MutableList<DownloadStatusListener> = mutableListOf()

    var download_state: Int by mutableStateOf(0)
        private set
    
    private fun onStateChanged() {
        download_state++
    }

    companion object {
        fun getDownloadDir(context: PlatformContext): File {
            return File(context.getFilesDir(), "download")
        }
    }

    fun addDownloadStatusListener(listener: DownloadStatusListener) {
        download_status_listeners.add(listener)
    }
    fun removeDownloadStatusListener(listener: DownloadStatusListener) {
        download_status_listeners.remove(listener)
    }

//    private fun onResultIntentReceived(action: PlayerDownloadService.IntentAction, intent: Intent) {
//        val song_id: String = intent.getExtra("song_id")
//
//        val instance: Int? = intent.getExtra("instance")
//        if (instance != null) {
//            result_callbacks[action]?.get(song_id)?.remove(instance)?.invoke(intent)
//        }
//
//        when (action) {
//            PlayerDownloadService.IntentAction.START_DOWNLOAD -> {
//                val result = intent.extras!!.get("result") as Result<File?>
//                if (result.isFailure) {
//                    context.sendNotification(result.exceptionOrNull()!!)
//                }
//
//                onStateChanged()
//            }
//            PlayerDownloadService.IntentAction.STATUS_CHANGED -> {
//                download_status_listeners.forEach { it.onSongDownloadStatusChanged(song_id, intent.getExtra("status")) }
//                onStateChanged()
//            }
//            else -> {}
//        }
//    }

//    private fun addResultCallback(action: PlayerDownloadService.IntentAction, song_id: String, instance: Int, callback: (Intent) -> Unit) {
//        val callbacks = result_callbacks.getOrPut(action) { mutableMapOf() }.getOrPut(song_id) { mutableMapOf() }
//        callbacks[instance] = callback
//    }

    fun iterateDownloadedFiles(action: (file: File?, data: PlayerDownloadService.FilenameData) -> Unit) {
        val files = getDownloadDir(context).listFiles() ?: return
        for (file in files) {
            action(file, PlayerDownloadService.getFilenameData(file.name))
        }
    }

    fun getSongLocalFile(song: Song): File? {
        val files = getDownloadDir(context).listFiles() ?: return null
        for (file in files) {
            if (PlayerDownloadService.fileMatchesDownload(file.name, song.id, Song.getTargetDownloadQuality()) == true) {
                return file
            }
        }
        return null
    }

    @Synchronized
    fun startDownload(song_id: String, silent: Boolean = false, onCompleted: ((File?, PlayerDownloadService.DownloadStatus) -> Unit)? = null) {
//        if (service == null) {
//            startService({ startDownload(song_id, silent, onCompleted) })
//            return
//        }
//
//        val instance = result_callback_id++
//        if (onCompleted != null) {
//            addResultCallback(PlayerDownloadService.IntentAction.START_DOWNLOAD, song_id, instance) { intent ->
//                val result = intent.extras!!.get("result") as Result<File?>
//                onCompleted(result.getOrNull(), intent.extras!!.get("status") as PlayerDownloadService.DownloadStatus)
//            }
//        }
//
//        val intent = Intent(PlayerDownloadService::class.java.canonicalName)
//        intent.putExtra("action", PlayerDownloadService.IntentAction.START_DOWNLOAD)
//        intent.putExtra("song_id", song_id)
//        intent.putExtra("silent", silent)
//        intent.putExtra("instance", instance)
//
//        LocalBroadcastManager.getInstance(context.ctx).sendBroadcast(intent)
//
//        onStateChanged()
    }

    fun getSongDownloadStatus(song_id: String, callback: (PlayerDownloadService.DownloadStatus) -> Unit) {
        if (service == null) {
            startService({ getSongDownloadStatus(song_id, callback) })
            return
        }

        context.networkThread {
            callback(service!!.getDownloadStatus(song_id))
        }
    }

    fun getSongDownloadProgress(song_id: String, callback: (Float) -> Unit) {
        if (service == null) {
            startService({ getSongDownloadProgress(song_id, callback) })
        }

        context.networkThread {
            callback(service!!.getDownloadProgress(song_id))
        }
    }

    @Synchronized
    private fun startService(onConnected: (() -> Unit)? = null, onDisconnected: (() -> Unit)? = null) {
//        if (service_connecting || service != null) {
//            return
//        }
//        service_connecting = true
//
//        service_connection = PlatformService.startService(
//            context,
//            PlayerDownloadService::class.java,
//            onConnected = { binder ->
//                service = (binder as PlayerDownloadService.ServiceBinder).getService()
//                service_connecting = false
//                LocalBroadcastManager.getInstance(context.ctx).registerReceiver(result_broadcast_receiver, IntentFilter(PlayerDownloadService.RESULT_INTENT_ACTION))
//                onConnected?.invoke()
//            },
//            onDisconnected = {
//                service = null
//                service_connecting = false
//                LocalBroadcastManager.getInstance(context.ctx).unregisterReceiver(result_broadcast_receiver)
//                onDisconnected?.invoke()
//            }
//        )
    }

    fun release() {
        if (service_connection != null) {
            PlatformService.unbindService(context, service_connection!!)
            service_connection = null
        }
    }
}
