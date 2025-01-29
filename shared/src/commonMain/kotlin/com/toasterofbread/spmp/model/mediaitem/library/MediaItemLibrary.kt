package com.toasterofbread.spmp.model.mediaitem.library

import SpMp
import com.toasterofbread.spmp.model.lyrics.LyricsFileConverter
import com.toasterofbread.spmp.model.mediaitem.playlist.LocalPlaylist
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.model.mediaitem.playlist.PlaylistData
import com.toasterofbread.spmp.model.mediaitem.playlist.PlaylistFileConverter
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.download.DownloadStatus
import com.toasterofbread.spmp.platform.playerservice.ClientServerPlayerService
import dev.toastbits.composekit.context.PlatformFile
import dev.toastbits.composekit.util.addUnique
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Suppress("DeferredResultUnused")
object MediaItemLibrary {
    suspend fun getLibraryDir(
        context: AppContext,
        custom_location_uri: String? = null
    ): PlatformFile? {
        val location_url: String = custom_location_uri ?: context.settings.Misc.LIBRARY_PATH.get()
        if (location_url.isNotBlank()) {
            val custom_dir: PlatformFile? = context.getUserDirectoryFile(location_url)
            if (custom_dir != null) {
                return custom_dir
            }
        }

        return getDefaultLibraryDir(context)
    }

    fun getDefaultLibraryDir(context: AppContext): PlatformFile? =
        context.getFilesDir()?.resolve("library")

    fun getSongDownloadsDir(context: AppContext): PlatformFile? =
        context.getFilesDir()?.resolve("downloads")

    suspend fun getLocalSongsDir(context: AppContext): PlatformFile? =
        getLibraryDir(context)?.resolve("songs")

    suspend fun getLocalLyricsDir(context: AppContext): PlatformFile? =
        getLibraryDir(context)?.resolve("lyrics")

    suspend fun getLocalLyricsFile(song: Song, context: AppContext): PlatformFile? =
        getLocalLyricsDir(context)?.resolve(LyricsFileConverter.getSongLyricsFileName(song))

    suspend fun getLocalPlaylistsDir(context: AppContext): PlatformFile? =
        getLibraryDir(context)?.resolve("playlists")

    suspend fun getLocalPlaylistFile(playlist: LocalPlaylist, context: AppContext): PlatformFile? =
        getLocalPlaylistsDir(context)?.resolve(PlaylistFileConverter.getPlaylistFileName(playlist))

    interface PlaylistsListener {
        fun onPlaylistAdded(playlist: PlaylistData) {}
        fun onPlaylistRemoved(playlist: Playlist) {}
    }

    private val playlists_listeners: MutableList<PlaylistsListener> = mutableListOf()
    private val playlists_listeners_lock: ReentrantLock = ReentrantLock()

    fun addPlaylistsListener(listener: PlaylistsListener) {
        playlists_listeners_lock.withLock {
            playlists_listeners.addUnique(listener)
        }
    }
    fun removePlaylistsListener(listener: PlaylistsListener) {
        playlists_listeners_lock.withLock {
            playlists_listeners.remove(listener)
        }
    }

    fun onPlaylistCreated(playlist: PlaylistData) {
        playlists_listeners_lock.withLock {
            for (listener in playlists_listeners) {
                listener.onPlaylistAdded(playlist)
            }
        }
    }

    fun onPlaylistDeleted(playlist: Playlist) {
        playlists_listeners_lock.withLock {
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

private class LocalLyricsSyncLoader: SyncLoader<PlatformFile>() {
    override suspend fun internalPerformSync(context: AppContext): Map<String, PlatformFile> {
        return MediaItemLibrary.getLocalLyricsDir(context)?.listFiles().orEmpty()
            .associateBy { file ->
                file.name.split('.', limit = 2).first()
            }
    }
}
