package com.toasterofbread.spmp.model.mediaitem.db

import LocalPlayerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.cash.sqldelight.Query
import com.toasterofbread.Database
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistRef
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.model.mediaitem.playlist.LocalPlaylistRef
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistRef
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongRef
import mediaitem.ArtistQueries
import mediaitem.PlaylistQueries
import mediaitem.SongQueries

fun isMediaItemHidden(item: MediaItem, db: Database, hidden_items: List<MediaItem>? = null): Boolean {
    if (hidden_items?.any { it.id == item.id } ?: item.Hidden.get(db)) {
        return true
    }

    if (!Settings.KEY_FILTER_ENABLE.get<Boolean>()) {
        return false
    }

    val title = item.getActiveTitle(db) ?: return false

    if (item is Artist && !Settings.KEY_FILTER_APPLY_TO_ARTISTS.get<Boolean>()) {
        return false
    }

    val keywords: Set<String> = Settings.KEY_FILTER_TITLE_KEYWORDS.get()
    for (keyword in keywords) {
        if (title.contains(keyword)) {
            return true
        }
    }

    return false
}

@Composable
fun rememberHiddenItems(hidden: Boolean = true): List<MediaItem> {
    val player = LocalPlayerState.current
    val db = player.database

    var hidden_songs: List<Song> by remember { mutableStateOf(
        db.songQueries.getByHidden(hidden)
    ) }
    var hidden_artists: List<Artist> by remember { mutableStateOf(
        db.artistQueries.getByHidden(hidden)
    ) }
    var hidden_playlists: List<Playlist> by remember { mutableStateOf(
        db.playlistQueries.getByHidden(hidden)
    ) }

    DisposableEffect(Unit) {
        val songs_listener = Query.Listener {
            hidden_songs = db.songQueries.getByHidden(hidden)
        }
        val artists_listener = Query.Listener {
            hidden_artists = db.artistQueries.getByHidden(hidden)
        }
        val playlists_listener = Query.Listener {
            hidden_playlists = db.playlistQueries.getByHidden(hidden)
        }

        db.songQueries.byHidden(hidden.toSQLBoolean()).addListener(songs_listener)
        db.artistQueries.byHidden(hidden.toSQLBoolean()).addListener(artists_listener)
        db.playlistQueries.byHidden(hidden.toSQLBoolean()).addListener(playlists_listener)

        onDispose {
            db.songQueries.byHidden(hidden.toSQLBoolean()).removeListener(songs_listener)
            db.artistQueries.byHidden(hidden.toSQLBoolean()).removeListener(artists_listener)
            db.playlistQueries.byHidden(hidden.toSQLBoolean()).removeListener(playlists_listener)
        }
    }

    return hidden_songs + hidden_artists + hidden_playlists
}

fun SongQueries.getByHidden(hidden: Boolean): List<Song> =
    byHidden(hidden.toSQLBoolean()).executeAsList().map { artist ->
        SongRef(artist)
    }

fun ArtistQueries.getByHidden(hidden: Boolean): List<Artist> =
    byHidden(hidden.toSQLBoolean()).executeAsList().map { artist ->
        ArtistRef(artist)
    }

fun PlaylistQueries.getByHidden(hidden: Boolean): List<Playlist> =
    byHidden(hidden.toSQLBoolean()).executeAsList().map { playlist ->
        val type = playlist.playlist_type?.let { PlaylistType.values()[it.toInt()] }
        if (type == PlaylistType.LOCAL) LocalPlaylistRef(playlist.id)
        else RemotePlaylistRef(playlist.id)
    }
