package com.toasterofbread.spmp.model.mediaitem.library

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.toasterofbread.composekit.platform.PlatformFile
import com.toasterofbread.composekit.utils.common.addUnique
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
        if (custom_location_uri.isBlank()) {
            return getDefaultLibraryDir(context)
        }
        return context.getUserDirectoryFile(custom_location_uri)
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

    fun getLocalLyrics(context: AppContext, song: Song, allow_partial: Boolean = false): PlatformFile? {
        val file: PlatformFile = getLocalLyricsFile(song, context)
        if (!file.is_file) {
            return null
        }
        return file
    }

    private val song_sync_coroutine_scope: CoroutineScope = CoroutineScope(Job())
    private val song_sync_lock: Mutex = Mutex()
    private var song_sync_job: Deferred<Map<String, DownloadStatus>>? = null
    var synced_songs: Map<String, DownloadStatus>? by mutableStateOf(null)
        private set
    var song_sync_in_progress: Boolean by mutableStateOf(false)
        private set

    suspend fun syncLocalSongs(context: AppContext, skip_if_synced: Boolean = false): Map<String, DownloadStatus> = song_sync_coroutine_scope.async {
        return@async coroutineScope {
            val loader: Deferred<Map<String, DownloadStatus>>
            song_sync_lock.withLock {
                if (skip_if_synced) {
                    synced_songs?.also {
                        return@coroutineScope it
                    }
                }

                song_sync_job?.also { job ->
                    if (job.isActive) {
                        loader = job
                        return@withLock
                    }
                }

                loader = async(start = CoroutineStart.LAZY) {
                    try {
                        song_sync_in_progress = true
                        val songs: Map<String, DownloadStatus> =
                            getAllLocalSongFiles(context, true)
                                .associateBy { it.song.id }
                        synced_songs = songs

                        SpMp._player_state?.interactService { service: Any ->
                            if (service is ClientServerPlayerService) {
                                service.onLocalSongsSynced(songs)
                            }
                        }

                        return@async songs
                    }
                    finally {
                        song_sync_in_progress = false
                    }
                }
                song_sync_job = loader
            }

            loader.start()
            return@coroutineScope loader.await()
        }
    }.await()

    suspend fun getLocalSong(song: Song, context: AppContext): DownloadStatus? =
        syncLocalSongs(context, true)[song.id]

    suspend fun getLocalSongs(context: AppContext): List<DownloadStatus> =
        syncLocalSongs(context, true).values.toList()

    suspend fun onSongFileAdded(download_status: DownloadStatus) {
        song_sync_lock.withLock {
            song_sync_job?.also { job ->
                if (job.isActive) {
                    return
                }
            }

            synced_songs = synced_songs?.toMutableMap()?.apply {
                put(download_status.song.id, download_status)
            }
        }

        SpMp._player_state?.interactService { service: Any ->
            if (service is ClientServerPlayerService) {
                service.onSongFileAdded(download_status)
            }
        }
    }

    suspend fun onSongFileDeleted(song: Song) {
        song_sync_lock.withLock {
            song_sync_job?.also { job ->
                if (job.isActive) {
                    return
                }
            }

            synced_songs = synced_songs?.toMutableMap()?.apply {
                remove(song.id)
            }
        }

        SpMp._player_state?.interactService { service: Any ->
            if (service is ClientServerPlayerService) {
                service.onSongFileDeleted(song)
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

            val song: Song =
                file_info.id?.let { SongRef(it) }
                        ?: LocalSongMetadataProcessor.readLocalSongMetadata(file, context, load_data = true)?.apply { saveToDatabase(context.database) }
                        ?: return@launch

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

//private suspend fun Song.getLocalSongFile(context: AppContext, files: List<PlatformFile>?): SongDownloader.Companion.DownloadFileInfo? {
//    if (files == null) {
//        return null
//    }
//
//    for (file in files) {
//        val file_info: SongDownloader.Companion.DownloadFileInfo = SongDownloader.getFileDownloadInfo(file)
//        if (!file_info.is_partial) {
//            if (LocalSongMetadataProcessor.readLocalSongMetadata(file, context, id, load_data = false) != null) {
//                return file_info
//            }
//        }
//        else if (file_info.id == id) {
//            return file_info
//        }
//    }
//
//    return null
//}
