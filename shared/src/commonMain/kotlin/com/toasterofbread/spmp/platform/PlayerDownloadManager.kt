package com.toasterofbread.spmp.platform

import LocalPlayerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongAudioQuality
import com.toasterofbread.composekit.platform.PlatformFile

expect class PlayerDownloadManager(context: AppContext) {
    class DownloadStatus {
        val song: Song
        val status: Status
        val quality: SongAudioQuality?
        val progress: Float
        val id: String
        enum class Status { IDLE, PAUSED, DOWNLOADING, CANCELLED, ALREADY_FINISHED, FINISHED }

        fun isCompleted(): Boolean
    }

    open class DownloadStatusListener() {
        open fun onDownloadAdded(status: DownloadStatus)
        open fun onDownloadRemoved(id: String)
        open fun onDownloadChanged(status: DownloadStatus)
    }

    fun addDownloadStatusListener(listener: DownloadStatusListener)
    fun removeDownloadStatusListener(listener: DownloadStatusListener)
    
    suspend fun getDownload(song: Song): DownloadStatus?
    suspend fun getDownloads(): List<DownloadStatus>

    @Synchronized
    fun startDownload(song_id: String, silent: Boolean = false, onCompleted: ((DownloadStatus) -> Unit)? = null)

    suspend fun deleteSongLocalAudioFile(song: Song)

    fun release()
}

@Composable
fun Song.rememberDownloadStatus(): State<PlayerDownloadManager.DownloadStatus?> {
    val download_manager: PlayerDownloadManager = LocalPlayerState.current.context.download_manager
    val download_state: MutableState<PlayerDownloadManager.DownloadStatus?> = remember { mutableStateOf(null) }

    LaunchedEffect(id) {
        download_state.value = null
        download_state.value = download_manager.getDownload(this@rememberDownloadStatus)
    }

    DisposableEffect(id) {
        val listener = object : PlayerDownloadManager.DownloadStatusListener() {
            override fun onDownloadAdded(status: PlayerDownloadManager.DownloadStatus) {
                if (status.song.id == id) {
                    download_state.value = status
                }
            }
            override fun onDownloadRemoved(id: String) {
                if (id == download_state.value?.id) {
                    download_state.value = null
                }
            }
            override fun onDownloadChanged(status: PlayerDownloadManager.DownloadStatus) {
                if (status.song.id == id) {
                    download_state.value = status
                }
            }
        }
        download_manager.addDownloadStatusListener(listener)

        onDispose {
            download_manager.removeDownloadStatusListener(listener)
        }
    }

    return download_state
}

@Composable
fun rememberSongDownloads(): State<List<PlayerDownloadManager.DownloadStatus>> {
    val download_manager = LocalPlayerState.current.context.download_manager
    val download_state: MutableState<List<PlayerDownloadManager.DownloadStatus>> = remember { mutableStateOf(emptyList()) }

    LaunchedEffect(Unit) {
        download_state.value = download_manager.getDownloads()
    }

    DisposableEffect(Unit) {
        val listener = object : PlayerDownloadManager.DownloadStatusListener() {
            override fun onDownloadAdded(status: PlayerDownloadManager.DownloadStatus) {
                synchronized(download_state) {
                    download_state.value += status
                }
            }
            override fun onDownloadRemoved(id: String) {
                synchronized(download_state) {
                    download_state.value = download_state.value.toMutableList().apply {
                        removeAll { it.id == id }
                    }
                }
            }
            override fun onDownloadChanged(status: PlayerDownloadManager.DownloadStatus) {
                synchronized(download_state) {
                    val temp = download_state.value.toMutableList()
                    for (i in 0 until download_state.value.size) {
                        if (download_state.value[i].id == status.id) {
                            temp[i] = status
                        }
                    }
                    download_state.value = temp
                }
            }
        }
        download_manager.addDownloadStatusListener(listener)

        onDispose {
            download_manager.removeDownloadStatusListener(listener)
        }
    }

    return download_state
}

expect fun Song.getLocalSongFile(context: AppContext, allow_partial: Boolean = false): PlatformFile?
expect fun Song.getLocalLyricsFile(context: AppContext, allow_partial: Boolean = false): PlatformFile?
