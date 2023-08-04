package com.toasterofbread.spmp.model.mediaitem.db

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.cash.sqldelight.Query
import com.toasterofbread.Database
import com.toasterofbread.spmp.model.mediaitem.AccountPlaylistRef
import com.toasterofbread.spmp.model.mediaitem.Artist
import com.toasterofbread.spmp.model.mediaitem.ArtistRef
import com.toasterofbread.spmp.model.mediaitem.LocalPlaylistRef
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.Playlist
import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.model.mediaitem.SongRef
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import mediaitem.ArtistQueries
import mediaitem.MediaItemQueries
import mediaitem.PlaylistQueries
import mediaitem.SongQueries

@Composable
fun rememberAnyItemsArePinned(db: Database, pinned: Boolean = true): Boolean {
    val any_are_pinned = remember { mutableStateOf(
        db.mediaItemQueries.anyArePinned(pinned)
    ) }

    DisposableEffect(Unit) {
        val listener = Query.Listener {
            any_are_pinned.value = db.mediaItemQueries.anyArePinned(pinned)
        }

        val query = db.mediaItemQueries.anyArePinned(pinned.toSQLBoolean())
        query.addListener(listener)

        onDispose {
            query.removeListener(listener)
        }
    }

    return any_are_pinned.value
}

@Composable
fun rememberPinnedItems(db: Database, pinned: Boolean = true): List<MediaItem> {
    var pinned_songs: List<Song> by remember { mutableStateOf(
        db.songQueries.getByPinned(pinned)
    ) }
    var pinned_artists: List<Artist> by remember { mutableStateOf(
        db.artistQueries.getByPinned(pinned)
    ) }
    var pinned_playlists: List<Playlist> by remember { mutableStateOf(
        db.playlistQueries.getByPinned(pinned)
    ) }

    DisposableEffect(Unit) {
        val songs_listener = Query.Listener {
            pinned_songs = db.songQueries.getByPinned(pinned)
        }
        val artists_listener = Query.Listener {
            pinned_artists = db.artistQueries.getByPinned(pinned)
        }
        val playlists_listener = Query.Listener {
            pinned_playlists = db.playlistQueries.getByPinned(pinned)
        }

        db.songQueries.byPinned(pinned.toSQLBoolean()).addListener(songs_listener)
        db.artistQueries.byPinned(pinned.toSQLBoolean()).addListener(artists_listener)
        db.playlistQueries.byPinned(pinned.toSQLBoolean()).addListener(playlists_listener)

        onDispose {
            db.songQueries.byPinned(pinned.toSQLBoolean()).removeListener(songs_listener)
            db.artistQueries.byPinned(pinned.toSQLBoolean()).removeListener(artists_listener)
            db.playlistQueries.byPinned(pinned.toSQLBoolean()).removeListener(playlists_listener)
        }
    }

    return pinned_songs + pinned_artists + pinned_playlists
}

fun MediaItemQueries.anyArePinned(pinned: Boolean): Boolean =
    anyArePinned(pinned.toSQLBoolean()).executeAsList().isNotEmpty()

fun SongQueries.getByPinned(pinned: Boolean): List<Song> =
    byPinned(pinned.toSQLBoolean()).executeAsList().map { artist ->
        SongRef(artist)
    }

fun ArtistQueries.getByPinned(pinned: Boolean): List<Artist> =
    byPinned(pinned.toSQLBoolean()).executeAsList().map { artist ->
        ArtistRef(artist)
    }

fun PlaylistQueries.getByPinned(pinned: Boolean): List<Playlist> =
    byPinned(pinned.toSQLBoolean()).executeAsList().map { playlist ->
        val type = playlist.playlist_type?.let { PlaylistType.values()[it.toInt()] }
        if (type == PlaylistType.LOCAL) LocalPlaylistRef(playlist.id)
        else AccountPlaylistRef(playlist.id)
    }
