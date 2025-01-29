package com.toasterofbread.spmp.model.mediaitem.playlist

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.toasterofbread.spmp.model.mediaitem.MediaItemHolder

class PlaylistHolder(initial_playlist: Playlist): MediaItemHolder {
    override val item: Playlist? get() = current_playlist
    private var current_playlist: Playlist? by mutableStateOf(initial_playlist)

    private val replacement_listener = object : Listener {
        override fun onPlaylistDeleted(deleted: Playlist) {
            if (deleted.id == current_playlist?.id) {
                current_playlist = null
            }
        }

        override fun onPlaylistReplaced(from: Playlist, to: Playlist) {
            if (from.id == current_playlist?.id) {
                current_playlist = to
            }
        }
    }

    init {
        addReplacementListener(replacement_listener)
    }

    private interface Listener {
        fun onPlaylistDeleted(deleted: Playlist)
        fun onPlaylistReplaced(from: Playlist, to: Playlist)
    }

    companion object {
        private val replacement_listeners: MutableList<Listener> = mutableListOf()
        private fun addReplacementListener(listener: Listener) {
            replacement_listeners.add(listener)
        }

        fun onPlaylistDeleted(deleted: Playlist) {
            for (listener in replacement_listeners) {
                listener.onPlaylistDeleted(deleted)
            }
        }

        fun onPlaylistReplaced(from: Playlist, to: Playlist) {
            assert(from.id != to.id)

            for (listener in replacement_listeners) {
                listener.onPlaylistReplaced(from, to)
            }
        }
    }
}
