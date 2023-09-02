package com.toasterofbread.spmp.model.mediaitem.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.toasterofbread.spmp.model.mediaitem.playlist.LocalPlaylist
import com.toasterofbread.spmp.model.mediaitem.playlist.LocalPlaylistData
import com.toasterofbread.spmp.model.mediaitem.playlist.PlaylistData
import com.toasterofbread.spmp.model.mediaitem.playlist.PlaylistFileConverter
import com.toasterofbread.spmp.model.mediaitem.playlist.PlaylistFileConverter.saveToFile
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.resources.getString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

private interface LocalPlaylistsListener {
    fun onPlaylistAdded(playlist: LocalPlaylistData) {}
    fun onPlaylistRemoved(playlist: LocalPlaylist) {}
}
private val local_playlists_listeners: MutableList<LocalPlaylistsListener> = mutableListOf()

fun MediaItemLibrary.onLocalPlaylistDeleted(playlist: LocalPlaylist) {
    synchronized(local_playlists_listeners) {
        for (listener in local_playlists_listeners) {
            listener.onPlaylistRemoved(playlist)
        }
    }
}

@Composable
fun MediaItemLibrary.rememberLocalPlaylists(context: PlatformContext): List<LocalPlaylistData>? {
    var playlists: List<LocalPlaylistData>? by remember { mutableStateOf(null) }
    LaunchedEffect(Unit) {
        playlists = loadLocalPlaylists(context)
    }

    DisposableEffect(Unit) {
        val listener = object : LocalPlaylistsListener {
            override fun onPlaylistAdded(playlist: LocalPlaylistData) {
                playlists = (playlists ?: emptyList()).plus(playlist)
            }

            override fun onPlaylistRemoved(playlist: LocalPlaylist) {
                playlists = playlists?.let { list ->
                    val mutable = list.toMutableList()
                    mutable.removeIf { it.id == playlist.id }
                    mutable
                }
            }
        }

        synchronized(local_playlists_listeners) {
            local_playlists_listeners.add(listener)
        }

        onDispose {
            synchronized(local_playlists_listeners) {
                local_playlists_listeners.remove(listener)
            }
        }
    }

    return playlists
}

suspend fun MediaItemLibrary.loadLocalPlaylists(context: PlatformContext): List<LocalPlaylistData>? = withContext(Dispatchers.IO) {
    val playlists_dir = getLocalPlaylistsDir(context)
    if (!playlists_dir.is_directory) {
        return@withContext null
    }

    val playlists = (playlists_dir.listFiles() ?: emptyList()).map { file ->
        PlaylistFileConverter.loadFromFile(file, context)
    }
    return@withContext playlists
}

suspend fun MediaItemLibrary.createLocalPlaylist(context: PlatformContext, base_data: PlaylistData? = null): Result<LocalPlaylistData> = withContext(Dispatchers.IO) {
    val playlists_dir = getLocalPlaylistsDir(context)
    var largest_existing_id: Int = -1

    if (playlists_dir.is_directory) {
        for (file in playlists_dir.listFiles() ?: emptyList()) {
            if (!file.is_file) {
                continue
            }

            val int_id: Int? = file.name.split('.', limit = 2).first().toIntOrNull()
            if (int_id != null && int_id > largest_existing_id) {
                largest_existing_id = int_id
            }
        }
    }
    else if (playlists_dir.is_file) {
        return@withContext Result.failure(IOException("Playlists directory path is occupied by a file ${playlists_dir.absolute_path}"))
    }
    else {
        playlists_dir.mkdirs()
    }

    val playlist = LocalPlaylistData(
        (largest_existing_id + 1).toString()
    )

    if (base_data != null) {
        base_data.populateData(playlist, context.database)
    }

    if (playlist.title == null) {
        playlist.title = getString("new_playlist_title")
    }
    playlist.loaded = true

    val file = getLocalPlaylistFile(playlist, context)
    playlist.saveToFile(file, context).onFailure {
        return@withContext Result.failure(it)
    }

    synchronized(local_playlists_listeners) {
        for (listener in local_playlists_listeners) {
            listener.onPlaylistAdded(playlist)
        }
    }

    return@withContext Result.success(playlist)
}
