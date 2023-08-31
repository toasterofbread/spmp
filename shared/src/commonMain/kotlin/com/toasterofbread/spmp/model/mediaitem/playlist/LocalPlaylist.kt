package com.toasterofbread.spmp.model.mediaitem.playlist

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.model.mediaitem.library.MediaItemLibrary
import com.toasterofbread.spmp.model.mediaitem.playlist.PlaylistFileConverter.saveToFile
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.utils.modifier.background
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

@Composable
fun rememberLocalPlaylists(context: PlatformContext): List<LocalPlaylistData>? {
    var playlists: List<LocalPlaylistData>? by remember { mutableStateOf(null) }
    LaunchedEffect(Unit) {
        playlists = loadLocalPlaylists(context)
    }
    return playlists
}

suspend fun loadLocalPlaylists(context: PlatformContext): List<LocalPlaylistData>? = withContext(Dispatchers.IO) {
    val playlists_dir = MediaItemLibrary.getLocalPlaylistsDir(context)
    if (!playlists_dir.is_directory) {
        return@withContext null
    }

    val playlists = (playlists_dir.listFiles() ?: emptyList()).map { file ->
        PlaylistFileConverter.loadFromFile(file, context)
    }
    return@withContext playlists
}

suspend fun createLocalPlaylist(context: PlatformContext): Result<LocalPlaylistData> = withContext(Dispatchers.IO) {
    val playlists_dir = MediaItemLibrary.getLocalPlaylistsDir(context)
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
    playlist.loaded = true
    playlist.title = getString("new_playlist_title")

    val extension = PlaylistFileConverter.getFileExtension()

    val file = playlists_dir.resolve("${playlist.id}.$extension")
    playlist.saveToFile(file, context).onFailure {
        return@withContext Result.failure(it)
    }

    return@withContext Result.success(playlist)
}

@Composable
fun LocalPlaylist.LocalPlaylistDefaultThumbnail(modifier: Modifier = Modifier) {
    Box(modifier.background(Theme.accent_provider), contentAlignment = Alignment.Center) {
        Icon(Icons.Default.PlaylistPlay, null, tint = Theme.on_accent)
    }
}
