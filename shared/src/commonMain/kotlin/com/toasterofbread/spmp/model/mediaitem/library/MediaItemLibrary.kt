package com.toasterofbread.spmp.model.mediaitem.library

import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.mediaitem.playlist.LocalPlaylist
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.model.mediaitem.playlist.PlaylistData
import com.toasterofbread.spmp.model.mediaitem.playlist.PlaylistFileConverter
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.platform.PlatformFile
import com.toasterofbread.utils.common.addUnique

object MediaItemLibrary {
    fun getLibraryDir(
        context: PlatformContext,
        custom_location_uri: String = Settings.KEY_LIBRARY_PATH.get(context),
    ): PlatformFile {
        if (custom_location_uri.isBlank()) {
            return getDefaultLibraryDir(context)
        }
        return context.getUserDirectoryFile(custom_location_uri)
    }

    fun getDefaultLibraryDir(context: PlatformContext): PlatformFile =
        PlatformFile.fromFile(context.getFilesDir(), context).resolve("library")

    fun getLocalSongsDir(context: PlatformContext): PlatformFile =
        getLibraryDir(context).resolve("songs")

    fun getLocalLyricsDir(context: PlatformContext): PlatformFile =
        getLibraryDir(context).resolve("lyrics")

    fun getLocalPlaylistsDir(context: PlatformContext): PlatformFile =
        getLibraryDir(context).resolve("playlists")

    fun getLocalPlaylistFile(playlist: LocalPlaylist, context: PlatformContext): PlatformFile =
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
}
