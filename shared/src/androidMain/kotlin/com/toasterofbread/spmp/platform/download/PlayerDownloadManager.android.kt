package com.toasterofbread.spmp.platform.download

import android.os.Build
import com.toasterofbread.composekit.platform.PlatformFile
import com.toasterofbread.spmp.PlayerDownloadService
import com.toasterofbread.spmp.model.lyrics.LyricsFileConverter
import com.toasterofbread.spmp.model.mediaitem.library.MediaItemLibrary
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongAudioQuality
import com.toasterofbread.spmp.model.mediaitem.song.SongData
import com.toasterofbread.spmp.model.mediaitem.song.SongRef
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.startPlatformService
import com.toasterofbread.spmp.platform.unbindPlatformService
import kotlinx.coroutines.Dispatchers
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

    actual data class DownloadStatus(
        actual val song: Song,
        actual val status: Status,
        actual val quality: SongAudioQuality?,
        actual val progress: Float,
        actual val id: String,
        val file: PlatformFile?
    ) {
        actual enum class Status { IDLE, PAUSED, DOWNLOADING, CANCELLED, ALREADY_FINISHED, FINISHED }

        actual fun isCompleted(): Boolean = progress >= 1f
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
        val data = result.data
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
            val callbacks = result_callbacks
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
            val service_status: DownloadStatus? = getDownloadStatus(song)
            if (service_status != null) {
                return@withContext service_status
            }
        }

        for (file in getSongDownloadDir(context).listFiles() ?: emptyList()) {
            val in_progress: Boolean
            if (PlayerDownloadService.isFileDownloadInProgressForSong(file, song)) {
                in_progress = true
            }
            else if (LocalSongMetadataProcessor.readLocalSongMetadata(file, match_id = song.id, load_data = false) != null) {
                in_progress = false
            }
            else {
                continue
            }

            return@withContext DownloadStatus(
                song = song,
                status = if (in_progress) DownloadStatus.Status.IDLE else DownloadStatus.Status.FINISHED,
                quality = null,
                progress = if (in_progress) -1f else 1f,
                id = file.name,
                file = file
            )
        }

        return@withContext null
    }

    actual suspend fun getDownloads(): List<DownloadStatus> = withContext(Dispatchers.IO) {
        val current_downloads: List<DownloadStatus> = service?.run {
            getAllDownloadsStatus()
        } ?: emptyList()

        val files: List<PlatformFile> = getSongDownloadDir(context).listFiles() ?: emptyList()
        return@withContext current_downloads + files.mapNotNull { file ->
            if (current_downloads.any { it.file?.matches(file) == true }) {
                return@mapNotNull null
            }

            val song: Song
            val in_progress: Boolean

            val song_id: String? = PlayerDownloadService.getSongIdOfInProgressDownload(file)
            if (song_id != null) {
                song = SongRef(song_id)
                in_progress = true
            }
            else {
                song = LocalSongMetadataProcessor.readLocalSongMetadata(file, load_data = false) ?: return@mapNotNull null
                in_progress = false
            }

            DownloadStatus(
                song = song,
                status = if (in_progress) DownloadStatus.Status.IDLE else DownloadStatus.Status.FINISHED,
                quality = null,
                progress = if (in_progress) -1f else 1f,
                id = file.name,
                file = file
            )
        }
    }

    @Synchronized
    actual fun startDownload(song: Song, silent: Boolean, onCompleted: ((DownloadStatus) -> Unit)?) {
        // If needed, get notification permission on A13 and above
        if (!silent && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.application_context?.requestNotficationPermission() { granted ->
                if (granted) {
                    performDownload(song, silent, onCompleted)
                }
            }
        }
        else {
            performDownload(song, silent, onCompleted)
        }
    }

    private fun performDownload(song: Song, silent: Boolean, onCompleted: ((DownloadStatus) -> Unit)?) {
        onService {
            val instance: Int = result_callback_id++
            if (onCompleted != null) {
                addResultCallback(PlayerDownloadService.IntentAction.START_DOWNLOAD, song.id, instance) { data ->
                    onCompleted(data["status"] as DownloadStatus)
                }
            }

            onMessage(
                PlayerDownloadMessage(
                    PlayerDownloadService.IntentAction.START_DOWNLOAD,
                    mapOf(
                        "song_id" to song.id,
                        "silent" to silent
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

    actual suspend fun deleteSongLocalAudioFile(song: Song) = withContext(Dispatchers.IO) {
        val download: DownloadStatus = getDownload(song) ?: return@withContext
        download.file?.delete()
        forEachDownloadStatusListener { it.onDownloadRemoved(download.id) }
    }

    actual fun release() {
        if (service_connection != null) {
            unbindPlatformService(context, service_connection!!)
            service_connection = null
        }
    }

    companion object {
        fun getSongDownloadDir(context: AppContext): PlatformFile =
            MediaItemLibrary.getLocalSongsDir(context)
        fun getLyricsDownloadDir(context: AppContext): PlatformFile =
            MediaItemLibrary.getLocalLyricsDir(context)
    }
}

actual suspend fun Song.getLocalSongFile(context: AppContext, allow_partial: Boolean): PlatformFile? {
    val files: List<PlatformFile> = PlayerDownloadManager.getSongDownloadDir(context).listFiles() ?: return null
    for (file in files) {
        if (PlayerDownloadService.isFileDownloadInProgressForSong(file, this)) {
            if (allow_partial) {
                return file
            }
            return null
        }

        val metadata: SongData? = LocalSongMetadataProcessor.readLocalSongMetadata(file, id, load_data = false)
        if (metadata != null) {
            return file
        }
    }
    return null
}

actual fun Song.getLocalLyricsFile(context: AppContext, allow_partial: Boolean): PlatformFile? {
    val filename: String = LyricsFileConverter.getSongLyricsFileName(this)

    val files: List<PlatformFile> = PlayerDownloadManager.getLyricsDownloadDir(context).listFiles() ?: return null
    return files.firstOrNull { it.name == filename }
}
