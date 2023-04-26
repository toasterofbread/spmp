package com.spectre7.spmp.platform

import com.spectre7.spmp.PlayerDownloadService
import com.spectre7.spmp.model.Song
import java.io.File

actual class PlayerDownloadManager actual constructor(val context: PlatformContext) {
    private var service: PlayerDownloadService? = null
    private var service_connecting = false
    private var service_connection: Any? = null

    data class PlayerDownloadMessage(
        val action: PlayerDownloadService.IntentAction,
        val data: Map<String, Any?>,
        val instance: Int? = null
    )

    private var result_callback_id: Int = 0
    private val result_callbacks: MutableMap<PlayerDownloadService.IntentAction, MutableMap<String, MutableMap<Int, (Map<String, Any?>) -> Unit>>> = mutableMapOf()
    private val result_receiver = { data: Any? ->
        require(data is PlayerDownloadMessage)
        onResultIntentReceived(data)
    }

    actual class DownloadStatus(
        actual val song: Song,
        actual val status: Status,
        actual val quality: Song.AudioQuality,
        actual val progress: Float,
        val file: File?
    ) {
        actual enum class Status { IDLE, PAUSED, DOWNLOADING, CANCELLED, ALREADY_FINISHED, FINISHED }
    }

    actual interface DownloadStatusListener {
        actual fun onSongDownloadStatusChanged(song_id: String, status: DownloadStatus.Status)
    }
    private val download_status_listeners: MutableList<DownloadStatusListener> = mutableListOf()

    actual fun addDownloadStatusListener(listener: DownloadStatusListener) {
        download_status_listeners.add(listener)
    }
    actual fun removeDownloadStatusListener(listener: DownloadStatusListener) {
        download_status_listeners.remove(listener)
    }

    private fun onResultIntentReceived(result: PlayerDownloadMessage) {
        val data = result.data
        val status: DownloadStatus = data["status"] as DownloadStatus

        val instance: Int? = data["instance"] as Int?
        if (instance != null) {
            result_callbacks[result.action]?.get(status.song.id)?.remove(instance)?.invoke(data)
        }

        when (result.action) {
            PlayerDownloadService.IntentAction.START_DOWNLOAD -> {
                val result = data["result"] as Result<File?>
                if (result.isFailure) {
                    context.sendNotification(result.exceptionOrNull()!!)
                }
//                onStateChanged()
            }
            PlayerDownloadService.IntentAction.STATUS_CHANGED -> {
                download_status_listeners.forEach { it.onSongDownloadStatusChanged(status.song.id, data["status"] as DownloadStatus.Status) }
//                onStateChanged()
            }
            else -> {}
        }
    }

    private fun addResultCallback(action: PlayerDownloadService.IntentAction, song_id: String, instance: Int, callback: (data: Map<String, Any?>) -> Unit) {
        val callbacks = result_callbacks.getOrPut(action) { mutableMapOf() }.getOrPut(song_id) { mutableMapOf() }
        callbacks[instance] = callback
    }

    actual fun getDownloadedSongs(): List<DownloadStatus> {
        val files = getDownloadDir(context).listFiles() ?: return emptyList()
        return files.map { file ->
            val data = PlayerDownloadService.getFilenameData(file.name)
            DownloadStatus(
                Song.fromId(data.id),
                if (data.downloading) DownloadStatus.Status.IDLE else DownloadStatus.Status.FINISHED,
                data.quality,
                if (data.downloading) 0f else 1f,
                file
            )
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
    actual fun startDownload(song_id: String, silent: Boolean, onCompleted: ((DownloadStatus) -> Unit)?) {
        if (service == null) {
            startService({ startDownload(song_id, silent, onCompleted) })
            return
        }

        val instance = result_callback_id++
        if (onCompleted != null) {
            addResultCallback(PlayerDownloadService.IntentAction.START_DOWNLOAD, song_id, instance) { data ->
                onCompleted(data["status"] as DownloadStatus)
            }
        }

        service!!.onMessage(PlayerDownloadMessage(
            PlayerDownloadService.IntentAction.START_DOWNLOAD,
            mapOf(
                "song_id" to song_id,
                "silent" to silent
            ),
            instance
        ))


//        onStateChanged()
    }

    actual fun getSongDownloadStatus(song_id: String, callback: (DownloadStatus) -> Unit) {
        if (service == null) {
            startService({ getSongDownloadStatus(song_id, callback) })
            return
        }

        context.networkThread {
            callback(service!!.getDownloadStatus(song_id))
        }
    }

//    fun getSongDownloadProgress(song_id: String, callback: (Float) -> Unit) {
//        if (service == null) {
//            startService({ getSongDownloadProgress(song_id, callback) })
//        }
//
//        context.networkThread {
//            callback(service!!.getDownloadProgress(song_id))
//        }
//    }

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
                service!!.addMessageReceiver(result_receiver)
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

    actual fun release() {
        if (service_connection != null) {
            PlatformService.unbindService(context, service_connection!!)
            service_connection = null
        }
    }

    companion object {
        fun getDownloadDir(context: PlatformContext): File {
            return File(context.getFilesDir(), "download")
        }
    }
}
