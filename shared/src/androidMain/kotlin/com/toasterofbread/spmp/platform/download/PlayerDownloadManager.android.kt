package com.toasterofbread.spmp.platform.download

import android.os.Build
import dev.toastbits.composekit.context.PlatformFile
import com.toasterofbread.spmp.model.mediaitem.library.MediaItemLibrary
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.startPlatformService
import com.toasterofbread.spmp.platform.unbindPlatformService
import com.toasterofbread.spmp.service.playercontroller.DownloadRequestCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

actual class PlayerDownloadManager actual constructor(val context: AppContext) {
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

    actual open class DownloadStatusListener {
        actual open fun onDownloadAdded(status: DownloadStatus) {}
        actual open fun onDownloadRemoved(id: String) {}
        actual open fun onDownloadChanged(status: DownloadStatus) {}
    }
    private val download_status_listeners: MutableList<DownloadStatusListener> = mutableListOf()

    actual fun addDownloadStatusListener(listener: DownloadStatusListener) {
        synchronized(download_status_listeners) {
            download_status_listeners.add(listener)
        }
    }
    actual fun removeDownloadStatusListener(listener: DownloadStatusListener) {
        synchronized(download_status_listeners) {
            download_status_listeners.remove(listener)
        }
    }

    private fun forEachDownloadStatusListener(action: (DownloadStatusListener) -> Unit) {
        synchronized(download_status_listeners) {
            for (listener in download_status_listeners.toList()) {
                action(listener)
            }
        }
    }

    private fun onResultIntentReceived(result: PlayerDownloadMessage) {
        val data: Map<String, Any?> = result.data
        val status: DownloadStatus = data["status"] as DownloadStatus

        val instance: Int? = result.instance
        if (instance != null) {
            synchronized(result_callbacks) {
                result_callbacks[result.action]?.get(status.song.id)?.remove(instance)?.invoke(data)
            }
        }

        when (result.action) {
            PlayerDownloadService.IntentAction.START_DOWNLOAD -> {
                (data["result"] as Result<PlatformFile?>).fold(
                    {
                        forEachDownloadStatusListener { it.onDownloadChanged(status) }
                    },
                    { error ->
                        context.sendNotification(error)
                        forEachDownloadStatusListener { it.onDownloadRemoved(status.id) }
                    }
                )
            }
            PlayerDownloadService.IntentAction.STATUS_CHANGED -> {
                if (data["started"] as Boolean) {
                    forEachDownloadStatusListener { it.onDownloadAdded(status) }
                }
                else {
                    forEachDownloadStatusListener { it.onDownloadChanged(status) }
                }
            }
            else -> {}
        }
    }

    private fun addResultCallback(action: PlayerDownloadService.IntentAction, song_id: String, instance: Int, callback: (data: Map<String, Any?>) -> Unit) {
        synchronized(result_callbacks) {
            val callbacks: MutableMap<Int, (Map<String, Any?>) -> Unit> = result_callbacks
                .getOrPut(action) { mutableMapOf() }
                .getOrPut(song_id) { mutableMapOf() }
            callbacks[instance] = callback
        }
    }

    private fun onService(action: PlayerDownloadService.() -> Unit) {
        service?.also {
            action(it)
            return
        }
        startService({ action(it) })
    }

    actual suspend fun getDownload(song: Song): DownloadStatus? = withContext(Dispatchers.IO) {
        service?.apply {
            val service_status: DownloadStatus? = downloader?.getDownloadStatus(song)
            if (service_status != null) {
                return@withContext service_status
            }
        }

        return@withContext MediaItemLibrary.getLocalSong(song, context)
    }

    actual suspend fun getDownloads(): List<DownloadStatus> =
        service?.downloader?.getAllDownloadsStatus() ?: emptyList()

    actual fun canStartDownload(): Boolean = true

    @Synchronized
    actual fun startDownload(
        song: Song,
        silent: Boolean,
        custom_uri: String?,
        download_lyrics: Boolean,
        direct: Boolean,
        callback: DownloadRequestCallback?
    ) {
        if (!silent && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.applicationContext?.requestNotificationPermission { granted ->
                if (granted) {
                    performDownload(song, silent, custom_uri, download_lyrics, direct, callback)
                }
            }
        }
        else {
            performDownload(song, silent, custom_uri, download_lyrics, direct, callback)
        }
    }

    private fun performDownload(
        song: Song,
        silent: Boolean,
        custom_uri: String?,
        download_lyrics: Boolean,
        direct: Boolean,
        onCompleted: DownloadRequestCallback?
    ) {
        onService {
            val instance: Int = result_callback_id++
            addResultCallback(PlayerDownloadService.IntentAction.START_DOWNLOAD, song.id, instance) { data ->
                val status: DownloadStatus = data["status"] as DownloadStatus
                context.coroutineScope.launch {
                    if (custom_uri == null) {
                        MediaItemLibrary.onSongFileAdded(status)
                    }
                    onCompleted?.invoke(status)
                }
            }

            onMessage(
                PlayerDownloadMessage(
                    PlayerDownloadService.IntentAction.START_DOWNLOAD,
                    mapOf(
                        "song_id" to song.id,
                        "silent" to silent,
                        "custom_uri" to custom_uri,
                        "download_lyrics" to download_lyrics,
                        "direct" to direct
                    ),
                    instance
                )
            )
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
            { PlayerDownloadService() },
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

    actual suspend fun deleteSongLocalAudioFile(song: Song) {
        val download: DownloadStatus = getDownload(song) ?: return
        download.file?.delete()
        MediaItemLibrary.onSongFileDeleted(song)
        forEachDownloadStatusListener { it.onDownloadRemoved(download.id) }
    }

    actual fun release() {
        if (service_connection != null) {
            unbindPlatformService(context, service_connection!!)
            service_connection = null
        }
    }
}
