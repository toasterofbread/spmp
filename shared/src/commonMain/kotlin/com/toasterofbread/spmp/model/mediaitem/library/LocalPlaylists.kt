package com.toasterofbread.spmp.model.mediaitem.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.toasterofbread.spmp.model.mediaitem.playlist.LocalPlaylistData
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.model.mediaitem.playlist.PlaylistData
import com.toasterofbread.spmp.model.mediaitem.playlist.PlaylistFileConverter
import com.toasterofbread.spmp.model.mediaitem.playlist.PlaylistFileConverter.saveToFile
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.resources.getString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

@Composable
fun MediaItemLibrary.rememberLocalPlaylists(context: AppContext): List<LocalPlaylistData>? {
    var playlists: List<LocalPlaylistData>? by remember { mutableStateOf(null) }
    LaunchedEffect(Unit) {
        playlists = loadLocalPlaylists(context)
    }

    DisposableEffect(Unit) {
        val listener = object : MediaItemLibrary.PlaylistsListener {
            override fun onPlaylistAdded(playlist: PlaylistData) {
                if (playlist is LocalPlaylistData) {
                    playlists = (playlists ?: emptyList()).plus(playlist)
                }
            }

            override fun onPlaylistRemoved(playlist: Playlist) {
                playlists = playlists?.let { list ->
                    val mutable = list.toMutableList()
                    mutable.removeIf { it.id == playlist.id }
                    mutable
                }
            }
        }

        addPlaylistsListener(listener)

        onDispose {
            removePlaylistsListener(listener)
        }
    }

    return playlists
}

suspend fun MediaItemLibrary.loadLocalPlaylists(context: AppContext): List<LocalPlaylistData>? = withContext(Dispatchers.IO) {
    val playlists_dir = getLocalPlaylistsDir(context)
    if (!playlists_dir.is_directory) {
        return@withContext null
    }

    val playlists = (playlists_dir.listFiles() ?: emptyList()).mapNotNull { file ->
        PlaylistFileConverter.loadFromFile(file, context)
    }
    return@withContext playlists
}

suspend fun MediaItemLibrary.createLocalPlaylist(context: AppContext, base_data: PlaylistData? = null): Result<LocalPlaylistData> = withContext(Dispatchers.IO) {
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

    if (playlist.name == null) {
        playlist.name = getString("new_playlist_title")
    }
    playlist.loaded = true

    val file = getLocalPlaylistFile(playlist, context)
    playlist.saveToFile(file, context).onFailure {
        return@withContext Result.failure(it)
    }

    onPlaylistCreated(playlist)

    return@withContext Result.success(playlist)
}
