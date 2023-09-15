package com.toasterofbread.spmp.platform

import LocalPlayerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongAudioQuality

expect class PlayerDownloadManager(context: PlatformContext) {
    class DownloadStatus {
        val song: Song
        val status: Status
        val quality: SongAudioQuality?
        val progress: Float
        val id: String
        enum class Status { IDLE, PAUSED, DOWNLOADING, CANCELLED, ALREADY_FINISHED, FINISHED }
    }

    open class DownloadStatusListener() {
        open fun onDownloadAdded(status: DownloadStatus)
        open fun onDownloadRemoved(id: String)
        open fun onDownloadChanged(status: DownloadStatus)
    }

    fun addDownloadStatusListener(listener: DownloadStatusListener)
    fun removeDownloadStatusListener(listener: DownloadStatusListener)
    
    fun getDownload(song: Song, callback: (DownloadStatus?) -> Unit)
    fun getDownloads(callback: (List<DownloadStatus>) -> Unit)

    @Synchronized
    fun startDownload(song_id: String, silent: Boolean = false, onCompleted: ((DownloadStatus) -> Unit)? = null)

    fun release()
}

@Composable
fun rememberSongDownloads(): List<PlayerDownloadManager.DownloadStatus> {
    val download_manager = LocalPlayerState.current.context.download_manager
    var downloads: List<PlayerDownloadManager.DownloadStatus> by remember { mutableStateOf(emptyList()) }

    DisposableEffect(Unit) {
        download_manager.getDownloads {
            downloads = it
        }

        val listener = object : PlayerDownloadManager.DownloadStatusListener() {
            override fun onDownloadAdded(status: PlayerDownloadManager.DownloadStatus) {
                downloads += status
            }
            override fun onDownloadRemoved(id: String) {
                downloads = downloads.toMutableList().apply {
                    removeIf { it.id == id }
                }
            }
            override fun onDownloadChanged(status: PlayerDownloadManager.DownloadStatus) {
                val temp = downloads.toMutableList()
                for (i in downloads.indices) {
                    if (downloads[i].id == status.id) {
                        temp[i] = status
                    }
                }
                downloads = temp
            }
        }
        download_manager.addDownloadStatusListener(listener)

        onDispose {
            download_manager.removeDownloadStatusListener(listener)
        }
    }

    return downloads
}

expect fun Song.getLocalAudioFile(context: PlatformContext): PlatformFile?
