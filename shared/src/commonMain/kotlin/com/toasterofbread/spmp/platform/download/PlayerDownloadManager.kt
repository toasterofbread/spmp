package com.toasterofbread.spmp.platform.download

import LocalPlayerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.toastbits.composekit.util.platform.Platform
import dev.toastbits.composekit.context.PlatformFile
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.library.MediaItemLibrary
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongAudioQuality
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.service.playercontroller.DownloadRequestCallback
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.download_method_title_library
import spmp.shared.generated.resources.download_method_title_custom
import spmp.shared.generated.resources.download_method_desc_library
import spmp.shared.generated.resources.download_method_desc_custom

enum class DownloadMethod {
    LIBRARY, CUSTOM;

    fun isAvailable(): Boolean = true

    @Composable
    fun getTitle(): String =
        when (this) {
            LIBRARY -> stringResource(Res.string.download_method_title_library)
            CUSTOM -> stringResource(Res.string.download_method_title_custom)
        }

    @Composable
    fun getDescription(): String =
        when (this) {
            LIBRARY -> stringResource(Res.string.download_method_desc_library)
            CUSTOM -> stringResource(Res.string.download_method_desc_custom)
        }

    suspend fun execute(context: AppContext, songs: List<Song>, callback: DownloadRequestCallback?) {
        when (this) {
            LIBRARY -> {
                for (song in songs) {
                    context.download_manager.startDownload(song, callback = callback)
                }
            }
            CUSTOM -> {
                if (songs.size == 1) {
                    val file: PlatformFile? =
                        context.promptUserForFileCreation(
                            // TODO | Remove hardcoded MIME type
                            "audio/mp4",
                            songs.single().getActiveTitle(context.database),
                            false
                        )

                    if (file == null) {
                        callback?.invoke(null)
                        return
                    }

                    context.download_manager.startDownload(
                        songs.single(),
                        custom_uri = file.uri,
                        download_lyrics = false,
                        direct = true,
                        callback = callback
                    )
                }
                else {
                    val directory: PlatformFile? = context.promptUserForDirectory(persist = true)
                    if (directory == null) {
                        callback?.invoke(null)
                        return
                    }

                    Platform.ANDROID.only {
                        directory.mkdirs()
                    }

                    context.coroutineScope.launch {
                        for (song in songs) {
                            var file: PlatformFile
                            val name: String = song.getActiveTitle(context.database) ?: getString(MediaItemType.SONG.getReadable(false))

                            var i: Int = 0
                            do {
                                // TODO | Remove hardcoded file type
                                var file_name = name + ".m4a"
                                if (i++ >= 1) {
                                    file_name += " (${i + 1})"
                                }
                                file = directory.resolve(file_name)
                            }
                            while (file.exists)

                            Platform.ANDROID.only {
                                // File must be created at this stage on Android, it will fail if done later
                                file.createFile()
                            }

                            context.download_manager.startDownload(song, custom_uri = file.uri, download_lyrics = false, callback = callback)
                        }
                    }
                }
            }
        }
    }

    companion object {
        val DEFAULT: DownloadMethod = LIBRARY
        val available: List<DownloadMethod> get() = entries.filter { it.isAvailable() }
    }
}

data class DownloadStatus(
    val song: Song,
    val status: Status,
    val quality: SongAudioQuality?,
    val progress: Float,
    val id: String,
    val file: PlatformFile?
) {
    enum class Status { IDLE, PAUSED, DOWNLOADING, CANCELLED, ALREADY_FINISHED, FINISHED }
    fun isCompleted(): Boolean = progress >= 1f
}

expect class PlayerDownloadManager(context: AppContext) {
    open class DownloadStatusListener() {
        open fun onDownloadAdded(status: DownloadStatus)
        open fun onDownloadRemoved(id: String)
        open fun onDownloadChanged(status: DownloadStatus)
    }

    fun addDownloadStatusListener(listener: DownloadStatusListener)
    fun removeDownloadStatusListener(listener: DownloadStatusListener)

    suspend fun getDownload(song: Song): DownloadStatus?
    suspend fun getDownloads(): List<DownloadStatus>

    fun canStartDownload(): Boolean
    fun startDownload(
        song: Song,
        silent: Boolean = false,
        custom_uri: String? = null,
        download_lyrics: Boolean = true,
        direct: Boolean = false,
        callback: DownloadRequestCallback? = null
    )

    suspend fun deleteSongLocalAudioFile(song: Song)

    fun release()
}

@Composable
fun Song.rememberDownloadStatus(): State<DownloadStatus?> {
    val download_manager: PlayerDownloadManager = LocalPlayerState.current.context.download_manager
    val download_state: MutableState<DownloadStatus?> = remember { mutableStateOf(null) }

    LaunchedEffect(id) {
        download_state.value = null
        download_state.value = download_manager.getDownload(this@rememberDownloadStatus)
    }

    DisposableEffect(id) {
        val listener = object : PlayerDownloadManager.DownloadStatusListener() {
            override fun onDownloadAdded(status: DownloadStatus) {
                if (status.song.id == id) {
                    download_state.value = status
                }
            }
            override fun onDownloadRemoved(id: String) {
                if (id == download_state.value?.id) {
                    download_state.value = null
                }
            }
            override fun onDownloadChanged(status: DownloadStatus) {
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
fun rememberSongDownloads(): State<List<DownloadStatus>> {
    val download_manager: PlayerDownloadManager = LocalPlayerState.current.context.download_manager
    var downloads: List<DownloadStatus> by remember { mutableStateOf(emptyList()) }

    val synced: Iterable<DownloadStatus> = MediaItemLibrary.synced_songs?.values ?: emptyList()

    LaunchedEffect(Unit) {
        downloads = download_manager.getDownloads()
    }

    DisposableEffect(Unit) {
        val listener = object : PlayerDownloadManager.DownloadStatusListener() {
            override fun onDownloadAdded(status: DownloadStatus) {
                synchronized(downloads) {
                    downloads += status
                }
            }
            override fun onDownloadRemoved(id: String) {
                synchronized(downloads) {
                    downloads = downloads.toMutableList().apply {
                        removeAll { it.id == id }
                    }
                }
            }
            override fun onDownloadChanged(status: DownloadStatus) {
                synchronized(downloads) {
                    val temp = downloads.toMutableList()
                    for (i in 0 until downloads.size) {
                        if (downloads[i].id == status.id) {
                            temp[i] = status
                        }
                    }
                    downloads = temp
                }
            }
        }
        download_manager.addDownloadStatusListener(listener)

        onDispose {
            download_manager.removeDownloadStatusListener(listener)
        }
    }

    return remember(synced) { derivedStateOf {
        downloads + synced.filter { local ->
            downloads.none { current ->
                if (current.isCompleted() != local.isCompleted()) {
                    return@none false
                }
                return@none local.file?.let { current.file?.matches(it) } == true
            }
        }
    } }
}
