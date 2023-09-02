package com.toasterofbread.spmp.platform

import android.os.Build
import com.toasterofbread.spmp.PlayerDownloadService
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongRef
import com.toasterofbread.spmp.model.mediaitem.library.MediaItemLibrary
import com.toasterofbread.spmp.model.mediaitem.song.SongAudioQuality

actual class PlayerDownloadManager actual constructor(val context: PlatformContext) {
    private var service: PlayerDownloadService? = null
    private var service_connecting = false
    private var service_connection: Any? = null
    private val service_connect_callbacks: MutableList<(PlayerDownloadService) -> Unit> = mutableListOf()

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

    actual data class DownloadStatus(
        actual val song: Song,
        actual val status: Status,
        actual val quality: SongAudioQuality?,
        actual val progress: Float,
        actual val id: String,
        val file: PlatformFile?
    ) {
        actual enum class Status { IDLE, PAUSED, DOWNLOADING, CANCELLED, ALREADY_FINISHED, FINISHED }
    }

    actual open class DownloadStatusListener {
        actual open fun onDownloadAdded(status: DownloadStatus) {}
        actual open fun onDownloadRemoved(id: String) {}
        actual open fun onDownloadChanged(status: DownloadStatus) {}
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
                val result = data["result"] as Result<PlatformFile?>
                result.fold(
                    {
                        download_status_listeners.forEach { it.onDownloadChanged(status) }
                    },
                    { error ->
                        context.sendNotification(error)
                        download_status_listeners.forEach { it.onDownloadRemoved(status.id) }
                    }
                )
            }
            PlayerDownloadService.IntentAction.STATUS_CHANGED -> {
                if (data["started"] as Boolean) {
                    download_status_listeners.forEach { it.onDownloadAdded(status) }
                }
                else {
                    download_status_listeners.forEach { it.onDownloadChanged(status) }
                }
            }
            else -> {}
        }
    }

    private fun addResultCallback(action: PlayerDownloadService.IntentAction, song_id: String, instance: Int, callback: (data: Map<String, Any?>) -> Unit) {
        val callbacks = result_callbacks.getOrPut(action) { mutableMapOf() }.getOrPut(song_id) { mutableMapOf() }
        callbacks[instance] = callback
    }

    private fun onService(action: PlayerDownloadService.() -> Unit) {
        service?.also {
            action(it)
            return
        }
        startService({ action(it) })
    }

    actual fun getDownload(song: Song, callback: (DownloadStatus?) -> Unit) {
        service?.apply {
            getDownloadStatus(song.id)?.also {
                callback(it)
                return
            }
        }

        for (file in getDownloadDir(context).listFiles() ?: emptyList()) {
            val data = PlayerDownloadService.getFilenameData(file.name)
            if (data.id == song.id) {
                callback(DownloadStatus(
                    SongRef(data.id),
                    if (data.downloading) DownloadStatus.Status.IDLE else DownloadStatus.Status.FINISHED,
                    null,
                    if (data.downloading) -1f else 1f,
                    file.name,
                    file
                ))
                return
            }
        }

        callback(null)
    }

    actual fun getDownloads(callback: (List<DownloadStatus>) -> Unit) {
        val current_downloads: List<DownloadStatus> = service?.run {
            getAllDownloadsStatus()
        } ?: emptyList()

        val files = getDownloadDir(context).listFiles() ?: emptyList()
        callback(
            current_downloads + files.mapNotNull { file ->
                if (current_downloads.any { it.file == file }) {
                    return@mapNotNull null
                }

                val data = PlayerDownloadService.getFilenameData(file.name)
                DownloadStatus(
                    SongRef(data.id),
                    if (data.downloading) DownloadStatus.Status.IDLE else DownloadStatus.Status.FINISHED,
                    null,
                    if (data.downloading) -1f else 1f,
                    file.name,
                    file
                )
            }
        )
    }

    @Synchronized
    actual fun startDownload(song_id: String, silent: Boolean, onCompleted: ((DownloadStatus) -> Unit)?) {
        // If needed, get notification permission on A13 and above
        if (!silent && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.application_context?.requestNotficationPermission() { granted ->
                if (granted) {
                    performDownload(song_id, silent, onCompleted)
                }
            }
        }
        else {
            performDownload(song_id, silent, onCompleted)
        }
    }

    private fun performDownload(song_id: String, silent: Boolean, onCompleted: ((DownloadStatus) -> Unit)?) {
        onService {
            val instance = result_callback_id++
            if (onCompleted != null) {
                addResultCallback(PlayerDownloadService.IntentAction.STATUS_CHANGED, song_id, instance) { data ->
                    onCompleted(data["status"] as DownloadStatus)
                }
            }

            onMessage(PlayerDownloadMessage(
                PlayerDownloadService.IntentAction.START_DOWNLOAD,
                mapOf(
                    "song_id" to song_id,
                    "silent" to silent
                ),
                instance
            ))
        }
    }

    @Synchronized
    private fun startService(onConnected: ((PlayerDownloadService) -> Unit)? = null, onDisconnected: (() -> Unit)? = null) {
        synchronized(service_connect_callbacks) {
            if (service != null) {
                onConnected?.invoke(service!!)
                return
            }

            if (service_connecting) {
                if (onConnected != null) {
                    service_connect_callbacks.add(onConnected)
                }
                return
            }

            service_connecting = true
        }

        service_connection = startPlatformService(
            context,
            PlayerDownloadService::class.java,
            onConnected = { binder ->
                synchronized(service_connect_callbacks) {
                    service = (binder as PlayerDownloadService.ServiceBinder).getService()
                    service!!.addMessageReceiver(result_receiver)
                    service_connecting = false

                    onConnected?.invoke(service!!)
                    for (callback in service_connect_callbacks) {
                        callback(service!!)
                    }
                    service_connect_callbacks.clear()
                }
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
            unbindPlatformService(context, service_connection!!)
            service_connection = null
        }
    }

    companion object {
        fun getDownloadDir(context: PlatformContext): PlatformFile {
            return MediaItemLibrary.getLocalSongsDir(context)
        }
    }
}

actual fun Song.getLocalAudioFile(context: PlatformContext): PlatformFile? {
    val files = PlayerDownloadManager.getDownloadDir(context).listFiles() ?: return null
    for (file in files) {
        if (PlayerDownloadService.fileMatchesDownload(file.name, id) == true) {
            return file
        }
    }
    return null
}
