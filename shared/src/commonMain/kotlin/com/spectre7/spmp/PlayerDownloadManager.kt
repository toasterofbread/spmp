package com.spectre7.spmp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.platform.PlatformContext
import com.spectre7.spmp.platform.PlatformService
import java.io.File

class PlayerDownloadManager(private val context: PlatformContext) {

    var service: PlayerDownloadService? = null
    private var service_connecting = false
    private var service_connection: Any? = null

    private var result_callback_id: Int = 0
    private val result_callbacks: MutableMap<PlayerDownloadService.IntentAction, MutableMap<String, MutableMap<Int, (Map<String, Any?>) -> Unit>>> = mutableMapOf()
    private val result_broadcast_receiver = object : PlatformService.BroadcastReceiver() {
        override fun onReceive(data: Map<String, Any?>) {
            val action = data["action"] as PlayerDownloadService.IntentAction
            onResultIntentReceived(action, data)
        }
    }

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

    private fun onResultIntentReceived(action: PlayerDownloadService.IntentAction, data: Map<String, Any?>) {
        val song_id: String = data["song_id"] as String

        val instance: Int? = data["instance"] as Int?
        if (instance != null) {
            result_callbacks[action]?.get(song_id)?.remove(instance)?.invoke(data)
        }

        when (action) {
            PlayerDownloadService.IntentAction.START_DOWNLOAD -> {
                val result = data["result"] as Result<File?>
                if (result.isFailure) {
                    context.sendNotification(result.exceptionOrNull()!!)
                }

                onStateChanged()
            }
            PlayerDownloadService.IntentAction.STATUS_CHANGED -> {
                download_status_listeners.forEach { it.onSongDownloadStatusChanged(song_id, data["status"] as PlayerDownloadService.DownloadStatus) }
                onStateChanged()
            }
            else -> {}
        }
    }

    private fun addResultCallback(action: PlayerDownloadService.IntentAction, song_id: String, instance: Int, callback: (data: Map<String, Any?>) -> Unit) {
        val callbacks = result_callbacks.getOrPut(action) { mutableMapOf() }.getOrPut(song_id) { mutableMapOf() }
        callbacks[instance] = callback
    }

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
        if (service == null) {
            startService({ startDownload(song_id, silent, onCompleted) })
            return
        }

        val instance = result_callback_id++
        if (onCompleted != null) {
            addResultCallback(PlayerDownloadService.IntentAction.START_DOWNLOAD, song_id, instance) { data ->
                val result = data["result"] as Result<File?>
                onCompleted(result.getOrNull(), data["status"] as PlayerDownloadService.DownloadStatus)
            }
        }

        service!!.broadcast(mapOf(
            "action" to PlayerDownloadService.IntentAction.START_DOWNLOAD,
            "song_id" to song_id,
            "silent" to silent,
            "instance" to instance
        ))

        onStateChanged()
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
        if (service_connecting || service != null) {
            return
        }

        service_connecting = true
        service_connection = PlatformService.startService(
            context,
            PlayerDownloadService::class.java,
            onConnected = { binder ->
                service = (binder as PlayerDownloadService.ServiceBinder).getService()
                service!!.addBroadcastReceiver(result_broadcast_receiver)
                service_connecting = false
                onConnected?.invoke()
            },
            onDisconnected = {
                service = null
                service_connecting = false
                onDisconnected?.invoke()
            }
        )
    }

    fun release() {
        if (service_connection != null) {
            PlatformService.unbindService(context, service_connection!!)
            service_connection = null
        }
    }
}
