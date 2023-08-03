package com.toasterofbread.spmp.platform

import LocalPlayerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongAudioQuality

expect class PlayerDownloadManager(context: PlatformContext) {
    class DownloadStatus {
        val song: Song
        val status: Status
        val quality: SongAudioQuality
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
    val player = LocalPlayerState.current
    val downloads: MutableList<PlayerDownloadManager.DownloadStatus> = remember { mutableStateListOf() }

    DisposableEffect(Unit) {
        player.download_manager.getDownloads {
            downloads.addAll(it)
        }

        val listener = object : PlayerDownloadManager.DownloadStatusListener() {
            override fun onDownloadAdded(status: PlayerDownloadManager.DownloadStatus) {
                downloads.add(status)
            }
            override fun onDownloadRemoved(id: String) {
                downloads.removeIf { it.id == id }
            }
            override fun onDownloadChanged(status: PlayerDownloadManager.DownloadStatus) {
                for (i in downloads.indices) {
                    if (downloads[i].id == status.id) {
                        downloads[i] = status
                    }
                }
            }
        }
        player.download_manager.addDownloadStatusListener(listener)

        onDispose {
            player.download_manager.removeDownloadStatusListener(listener)
        }
    }

    return downloads
}
