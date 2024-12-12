package com.toasterofbread.spmp.platform.download

import dev.toastbits.composekit.context.PlatformFile
import com.toasterofbread.spmp.model.mediaitem.library.MediaItemLibrary
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.service.playercontroller.DownloadRequestCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

actual class PlayerDownloadManager actual constructor(private val context: AppContext) {
    private var _downloader: SongDownloader? = null
    private val listeners: MutableList<DownloadStatusListener> = mutableListOf()

    private fun getDownloader(): SongDownloader {
        if (_downloader == null) {
            _downloader =
                object : SongDownloader(
                    context,
                    Executors.newFixedThreadPool(3)
                ) {
                    override fun getAudioFileDurationMs(file: PlatformFile): Long? {
                        // TODO
                        return null
                    }

                    override fun onDownloadStatusChanged(download: Download, started: Boolean) {
                        synchronized(listeners) {
                            val status: DownloadStatus = download.getStatusObject()
                            for (listener in listeners) {
                                if (started) {
                                    listener.onDownloadAdded(status)
                                }
                                else {
                                    listener.onDownloadChanged(status)
                                }
                            }
                        }
                    }
                }
        }

        return _downloader!!
    }

    actual open class DownloadStatusListener actual constructor() {
        actual open fun onDownloadAdded(status: DownloadStatus) {}
        actual open fun onDownloadRemoved(id: String) {}
        actual open fun onDownloadChanged(status: DownloadStatus) {}
    }

    actual fun addDownloadStatusListener(listener: DownloadStatusListener) {
        synchronized(listeners) {
            listeners.add(listener)
        }
    }

    actual fun removeDownloadStatusListener(listener: DownloadStatusListener) {
        synchronized(listeners) {
            listeners.remove(listener)
        }
    }

    actual fun canStartDownload(): Boolean = true

    @Synchronized
    actual fun startDownload(
        song: Song,
        silent: Boolean,
        custom_uri: String?,
        download_lyrics: Boolean,
        direct: Boolean,
        callback: DownloadRequestCallback?,
    ) {
        context.coroutineScope.launch {
            getDownloader().startDownload(song, silent, custom_uri, download_lyrics, direct) { download, result ->
                val status: DownloadStatus = download.getStatusObject()
                context.coroutineScope.launch {
                    if (custom_uri == null) {
                        MediaItemLibrary.onSongFileAdded(status)
                    }
                    callback?.invoke(status)
                }
            }
        }
    }

    actual fun release() {
        synchronized(listeners) {
            listeners.clear()
        }
    }

    actual suspend fun getDownload(song: Song): DownloadStatus? = withContext(Dispatchers.IO) {
        val service_status: DownloadStatus? = getDownloader().getDownloadStatus(song)
        if (service_status != null) {
            return@withContext service_status
        }

        return@withContext MediaItemLibrary.getLocalSong(song, context)
    }

    actual suspend fun getDownloads(): List<DownloadStatus> =
        _downloader?.getAllDownloadsStatus().orEmpty()

    actual suspend fun deleteSongLocalAudioFile(song: Song) {
        val download: DownloadStatus = getDownload(song) ?: return
        download.file?.delete()
        MediaItemLibrary.onSongFileDeleted(song)
        synchronized(listeners) {
            for (listener in listeners) {
                listener.onDownloadRemoved(download.id)
            }
        }
    }
}
