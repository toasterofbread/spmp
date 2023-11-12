package com.toasterofbread.spmp.model.mediaitem.playlist

import LocalPlayerState
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.model.mediaitem.MediaItemSortType
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.library.MediaItemLibrary
import com.toasterofbread.spmp.model.mediaitem.library.createLocalPlaylist
import com.toasterofbread.spmp.model.mediaitem.playlist.PlaylistFileConverter.saveToFile
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.composekit.platform.PlatformFile
import com.toasterofbread.composekit.utils.modifier.background
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface LocalPlaylist: Playlist {
    fun getLocalPlaylistFile(context: AppContext): PlatformFile =
        MediaItemLibrary.getLocalPlaylistFile(this, context)

    override fun getType(): MediaItemType = MediaItemType.PLAYLIST_LOC
    override fun getURL(context: AppContext): String =
        "file://" + getLocalPlaylistFile(context).absolute_path
    override fun getEmptyData(): LocalPlaylistData = LocalPlaylistData(id)

    override suspend fun loadData(context: AppContext, populate_data: Boolean, force: Boolean): Result<LocalPlaylistData>

    override suspend fun setActiveTitle(value: String?, context: AppContext) {
        val data: LocalPlaylistData = loadData(context).getOrNull() ?: return
        data.title = value

        val file = MediaItemLibrary.getLocalPlaylistFile(this, context)
        data.saveToFile(file, context)
    }

    override suspend fun setSortType(sort_type: MediaItemSortType?, context: AppContext): Result<Unit> {
        val data: LocalPlaylistData = loadData(context).fold(
            { it },
            { return Result.failure(it) }
        )
        data.sort_type = sort_type

        val file = MediaItemLibrary.getLocalPlaylistFile(this, context)
        return data.saveToFile(file, context)
    }
}

suspend fun Playlist.downloadAsLocalPlaylist(context: AppContext, replace: Boolean = false): Result<LocalPlaylistData> = withContext(Dispatchers.IO) {
    val playlist_data: PlaylistData = loadData(context).fold(
        { it },
        { return@withContext Result.failure(it) }
    )

    val local_playlist = MediaItemLibrary.createLocalPlaylist(context, base_data = playlist_data).fold(
        { it },
        { return@withContext Result.failure(it) }
    )

    if (replace) {
        PlaylistHolder.onPlaylistReplaced(this@downloadAsLocalPlaylist, local_playlist)
    }

    return@withContext Result.success(local_playlist)
}

@Composable
fun LocalPlaylist.LocalPlaylistDefaultThumbnail(modifier: Modifier = Modifier) {
    val player = LocalPlayerState.current
    Box(modifier.background(player.theme.accent_provider), contentAlignment = Alignment.Center) {
        Icon(Icons.Default.PlaylistPlay, null, tint = player.theme.on_accent)
    }
}
