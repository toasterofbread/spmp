package com.toasterofbread.spmp.model.mediaitem.library

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    suspend fun getLocalSongDownload(song: Song, context: AppContext): DownloadStatus? = withContext(Dispatchers.IO) {
        for (file in getLocalSongsDir(context).listFiles() ?: emptyList()) {
            val in_progress: Boolean
            if (SongDownloader.isFileDownloadInProgress(file)) {
                if (!SongDownloader.isFileDownloadInProgressForSong(file, song)) {
                    continue
                }

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

    suspend fun getLocalSongDownloads(context: AppContext): List<DownloadStatus> = withContext(Dispatchers.IO) {
        val files: List<PlatformFile> = getLocalSongsDir(context).listFiles() ?: emptyList()
        return@withContext files.mapNotNull { file ->
            val song: Song
            val in_progress: Boolean

            val song_id: String? = SongDownloader.getSongIdOfInProgressDownload(file)
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
}
