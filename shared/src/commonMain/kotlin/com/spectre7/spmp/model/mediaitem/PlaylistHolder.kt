package com.spectre7.spmp.model.mediaitem

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class PlaylistHolder(initial_playlist: Playlist): MediaItemHolder {
    private var playlist: Playlist? by mutableStateOf(if (initial_playlist.is_deleted) null else initial_playlist)
    override val item: Playlist?
        get() = playlist

    private val playlist_listener = object : Playlist.Listener {
        override fun onReplaced(with: Playlist) {
            check(with != playlist)
            with.addListener(this)
            playlist = with
        }

        override fun onDeleted() {
            check(playlist != null)
            playlist = null
        }
    }

    init {
        playlist?.addListener(playlist_listener)
    }
}
