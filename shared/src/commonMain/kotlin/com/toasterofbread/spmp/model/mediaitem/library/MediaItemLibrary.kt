package com.toasterofbread.spmp.model.mediaitem.library

import SpMp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.toastbits.composekit.platform.PlatformFile
import dev.toastbits.composekit.utils.common.addUnique
import com.toasterofbread.spmp.model.lyrics.LyricsFileConverter
import com.toasterofbread.spmp.model.mediaitem.playlist.LocalPlaylist
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.model.mediaitem.playlist.PlaylistData
import com.toasterofbread.spmp.model.mediaitem.playlist.PlaylistFileConverter
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongRef
import com.toasterofbread.spmp.model.settings.category.SystemSettings
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.download.DownloadStatus
import com.toasterofbread.spmp.platform.download.LocalSongMetadataProcessor
import com.toasterofbread.spmp.platform.download.SongDownloader
import com.toasterofbread.spmp.platform.playerservice.ClientServerPlayerService
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Suppress("DeferredResultUnused")
object MediaItemLibrary {
    fun getLibraryDir(
        context: AppContext,
        custom_location_uri: String = SystemSettings.Key.LIBRARY_PATH.get(context),
    ): PlatformFile {
        if (custom_location_uri.isNotBlank()) {
            val custom_dir: PlatformFile? = context.getUserDirectoryFile(custom_location_uri)
            if (custom_dir != null) {
                return custom_dir
            }
        }

        return getDefaultLibraryDir(context)
    }

    fun getDefaultLibraryDir(context: AppContext): PlatformFile =
        PlatformFile.fromFile(context.getFilesDir(), context).resolve("library")

    fun getSongDownloadsDir(context: AppContext): PlatformFile =
        PlatformFile.fromFile(context.getFilesDir(), context).resolve("downloads")

    fun getLocalSongsDir(context: AppContext): PlatformFile =
        getLibraryDir(context).resolve("songs")

    fun getLocalLyricsDir(context: AppContext): PlatformFile =
        getLibraryDir(context).resolve("lyrics")

    fun getLocalLyricsFile(song: Song, context: AppContext): PlatformFile =
        getLocalLyricsDir(context).resolve(LyricsFileConverter.getSongLyricsFileName(song))

    fun getLocalPlaylistsDir(context: AppContext): PlatformFile =
        getLibraryDir(context).resolve("playlists")

    fun getLocalPlaylistFile(playlist: LocalPlaylist, context: AppContext): PlatformFile =
        getLocalPlaylistsDir(context).resolve(PlaylistFileConverter.getPlaylistFileName(playlist))

    interface PlaylistsListener {
        fun onPlaylistAdded(playlist: PlaylistData) {}
        fun onPlaylistRemoved(playlist: Playlist) {}
    }
    private val playlists_listeners: MutableList<PlaylistsListener> = mutableListOf()

    fun addPlaylistsListener(listener: PlaylistsListener) {
        synchronized(playlists_listeners) {
            playlists_listeners.addUnique(listener)
        }
    }
    fun removePlaylistsListener(listener: PlaylistsListener) {
        synchronized(playlists_listeners) {
            playlists_listeners.remove(listener)
        }
    }

    fun onPlaylistCreated(playlist: PlaylistData) {
        synchronized(playlists_listeners) {
            for (listener in playlists_listeners) {
                listener.onPlaylistAdded(playlist)
            }
        }
    }

    fun onPlaylistDeleted(playlist: Playlist) {
        synchronized(playlists_listeners) {
            for (listener in playlists_listeners) {
                listener.onPlaylistRemoved(playlist)
            }
        }
    }

    suspend fun getLocalLyrics(context: AppContext, song: Song, allow_partial: Boolean = false): PlatformFile? =
        lyrics_sync_loader.performSync(context, skip_if_synced = true)[song.id]

    private val song_sync_loader: LocalSongSyncLoader = LocalSongSyncLoader()
    private val lyrics_sync_loader: LocalLyricsSyncLoader = LocalLyricsSyncLoader()

    val synced_songs: Map<String, DownloadStatus>?
        get() = song_sync_loader.synced
    val song_sync_in_progress: Boolean
        get() = song_sync_loader.sync_in_progress

    suspend fun syncLocalSongs(context: AppContext, skip_if_synced: Boolean = false): Map<String, DownloadStatus> =
        song_sync_loader.performSync(context, skip_if_synced)

    suspend fun getLocalSong(song: Song, context: AppContext): DownloadStatus? =
        syncLocalSongs(context, skip_if_synced = true)[song.id]

    suspend fun getLocalSongs(context: AppContext): List<DownloadStatus> =
        syncLocalSongs(context, skip_if_synced = true).values.toList()

    suspend fun onSongFileAdded(download_status: DownloadStatus) {
        song_sync_loader.put(download_status.song.id, download_status)

        SpMp._player_state?.interactService { service: Any ->
            if (service is ClientServerPlayerService) {
                service.onSongFilesAdded(listOf(download_status))
            }
        }
    }

    suspend fun onSongFileDeleted(song: Song) {
        song_sync_loader.remove(song.id)

        SpMp._player_state?.interactService { service: Any ->
            if (service is ClientServerPlayerService) {
                service.onSongFilesDeleted(listOf(song))
            }
        }
    }
}

private suspend fun getAllLocalSongFiles(context: AppContext, allow_partial: Boolean = false): List<DownloadStatus> = withContext(Dispatchers.IO) {
    val files: List<PlatformFile> = (
        MediaItemLibrary.getLocalSongsDir(context).listFiles().orEmpty()
        + if (allow_partial) MediaItemLibrary.getSongDownloadsDir(context).listFiles().orEmpty() else emptyList()
    )

    val results: Array<DownloadStatus?> = arrayOfNulls(files.size)

    files.mapIndexed { index, file ->
        launch {
            val file_info: SongDownloader.Companion.DownloadFileInfo = SongDownloader.getFileDownloadInfo(file)
            if (!allow_partial && file_info.is_partial) {
                return@launch
            }

            var song: Song? =
                file_info.id?.let { SongRef(it) }
                    ?: LocalSongMetadataProcessor.readLocalSongMetadata(file, context, load_data = true)?.apply { saveToDatabase(context.database) }

            if (song == null) {
//                song = SongRef('!' + file.absolute_path.hashCode().toString())
//                song.Title.set(file.name.split('.', limit = 2).first(), context.database)
                return@launch
            }

            val result: DownloadStatus =
                DownloadStatus(
                    song = song,
                    status = if (file_info.is_partial) DownloadStatus.Status.IDLE else DownloadStatus.Status.FINISHED,
                    quality = null,
                    progress = if (file_info.is_partial) -1f else 1f,
                    id = file_info.file.name,
                    file = file_info.file
                )

            synchronized(results) {
                results[index] = result
            }
        }
    }.joinAll()

    return@withContext results.filterNotNull()
}

private class LocalSongSyncLoader: SyncLoader<DownloadStatus>() {
    override suspend fun internalPerformSync(context: AppContext): Map<String, DownloadStatus> {
        val downloads: List<DownloadStatus> =
            getAllLocalSongFiles(context, true)

        SpMp._player_state?.interactService { service: Any ->
            if (service is ClientServerPlayerService) {
                service.onLocalSongsSynced(downloads)
            }
        }

        return downloads.associateBy { it.song.id }
    }
}

private class LocalLyricsSyncLoader: SyncLoader<PlatformFile>() {
    override suspend fun internalPerformSync(context: AppContext): Map<String, PlatformFile> {
        return MediaItemLibrary.getLocalLyricsDir(context).listFiles().orEmpty()
            .associateBy { file ->
                file.name.split('.', limit = 2).first()
            }
    }
}

private abstract class SyncLoader<T> {
    var synced: Map<String, T>? by mutableStateOf(null)
        private set
    var sync_in_progress: Boolean by mutableStateOf(false)
        private set

    private val coroutine_scope: CoroutineScope = CoroutineScope(Job())
    private val lock: Mutex = Mutex()
    private var sync_job: Deferred<Map<String, T>>? = null

    protected abstract suspend fun internalPerformSync(context: AppContext): Map<String, T>

    suspend fun put(key: String, value: T): Boolean {
        lock.withLock {
            sync_job?.also { job ->
                if (job.isActive) {
                    return false
                }
            }

            synced = synced?.toMutableMap()?.apply {
                put(key, value)
            }
        }

        return true
    }

    suspend fun remove(key: String) {
        lock.withLock {
            sync_job?.also { job ->
                if (job.isActive) {
                    return
                }
            }

            synced = synced?.toMutableMap()?.apply {
                remove(key)
            }
        }
    }

    suspend fun performSync(context: AppContext, skip_if_synced: Boolean = false): Map<String, T> =
        coroutine_scope.async {
            return@async coroutineScope {
                val loader: Deferred<Map<String, T>>
                lock.withLock {
                    if (skip_if_synced) {
                        synced?.also {
                            return@coroutineScope it
                        }
                    }

                    sync_job?.also { job ->
                        if (job.isActive) {
                            loader = job
                            return@withLock
                        }
                    }

                    loader = async(start = CoroutineStart.LAZY) {
                        try {
                            sync_in_progress = true

                            val sync_result: Map<String, T> = internalPerformSync(context)
                            synced = sync_result

                            return@async sync_result
                        }
                        finally {
                            sync_in_progress = false
                        }
                    }
                    sync_job = loader
                }

                loader.start()
                return@coroutineScope loader.await()
            }
        }.await()
}