package com.toasterofbread.spmp.model.mediaitem.db

import LocalPlayerState
import androidx.compose.runtime.*
import app.cash.sqldelight.Query
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongRef
import com.toasterofbread.spmp.model.mediaitem.song.toLong
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import dev.toastbits.ytmkt.model.external.SongLikedStatus

@Composable
fun rememberLocalLikedSongs(liked_status: SongLikedStatus = SongLikedStatus.LIKED): State<List<Song>?> {
    val player: PlayerState = LocalPlayerState.current
    
    val query: Query<String> = remember(liked_status) {
        player.database.songQueries.byLiked(liked_status.toLong())
    }
    val liked_songs: MutableState<List<Song>?> = remember { mutableStateOf(null) }
    
    DisposableEffect(query) {
        val listener: Query.Listener = Query.Listener {
            liked_songs.value = query.executeAsList().map { SongRef(it) }
        }
        
        listener.queryResultsChanged()

        query.addListener(listener)

        onDispose {
            query.removeListener(listener)
        }
    }
    
    return liked_songs
}
