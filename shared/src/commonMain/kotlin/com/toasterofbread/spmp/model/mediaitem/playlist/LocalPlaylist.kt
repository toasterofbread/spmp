package com.toasterofbread.spmp.model.mediaitem.playlist

import androidx.compose.runtime.Composable
import com.toasterofbread.Database
import com.toasterofbread.spmp.model.mediaitem.LocalPlaylistRef
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.model.mediaitem.observeAsState

@Composable
fun rememberLocalPlaylists(db: Database): List<LocalPlaylistRef> {
    return db.playlistQueries.byType(PlaylistType.LOCAL.ordinal.toLong())
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

fun createLocalPlaylist(db: Database): LocalPlaylistRef {
    val local_type = PlaylistType.LOCAL.ordinal.toLong()

    val largest_local_id: Long = db.playlistQueries
        .getLargestIdByType(local_type)
        .executeAsOne().MAX?.toLongOrNull() ?: -1

    val playlist = LocalPlaylistRef((largest_local_id + 1).toString())
    db.playlistQueries.insertById(playlist.id, local_type)

    return playlist
}
