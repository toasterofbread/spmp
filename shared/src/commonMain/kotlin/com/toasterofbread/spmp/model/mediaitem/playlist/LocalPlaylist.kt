package com.toasterofbread.spmp.model.mediaitem.playlist

import androidx.compose.runtime.Composable
import com.toasterofbread.spmp.model.mediaitem.LocalPlaylistRef
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.model.mediaitem.observeAsState
import com.toasterofbread.spmp.platform.PlatformContext

@Composable
fun rememberLocalPlaylists(context: PlatformContext): List<LocalPlaylistRef> {
    return context.database.playlistQueries.byType(PlaylistType.LOCAL.ordinal.toLong())
        .observeAsState(
            {
                it.executeAsList().map { playlist_id ->
                    LocalPlaylistRef(playlist_id)
                }
            },
            null
        )
        .value
}

fun createLocalPlaylist(context: PlatformContext): LocalPlaylistRef {
    val local_type: Long = PlaylistType.LOCAL.ordinal.toLong()

    val largest_local_id: Long = context.database.playlistQueries
        .getLargestIdByType(local_type)
        .executeAsOne().MAX?.toLongOrNull() ?: -1

    val playlist = LocalPlaylistRef((largest_local_id + 1).toString())
    context.database.playlistQueries.insertById(playlist.id, local_type)

    return playlist
}
