package com.spectre7.spmp.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.beust.klaxon.Klaxon
import com.spectre7.spmp.ui.component.PlaylistPreviewLong
import com.spectre7.spmp.ui.component.PlaylistPreviewSquare
import com.spectre7.spmp.ui.layout.PlayerViewContext

class Playlist private constructor (
    id: String
): MediaItemWithLayouts(id) {

    var is_album: Boolean? by mutableStateOf(null)
        private set

    fun supplyIsAlbum(value: Boolean?, certain: Boolean): MediaItem {
        if (value != null && (is_album == null || certain)) {
            is_album = value
        }
        return this
    }

    companion object {
        private val playlists: MutableMap<String, Playlist> = mutableMapOf()

        @Synchronized
        fun fromId(id: String): Playlist {
            return playlists.getOrPut(id) {
                val playlist = Playlist(id)
                playlist.loadFromCache()
                return@getOrPut playlist
            }.getOrReplacedWith() as Playlist
        }
    }

    @Composable
    override fun PreviewSquare(content_colour: () -> Color, playerProvider: () -> PlayerViewContext, enable_long_press_menu: Boolean, modifier: Modifier) {
        PlaylistPreviewSquare(this, content_colour, playerProvider, enable_long_press_menu, modifier)
    }

    @Composable
    override fun PreviewLong(content_colour: () -> Color, playerProvider: () -> PlayerViewContext, enable_long_press_menu: Boolean, modifier: Modifier) {
        PlaylistPreviewLong(this, content_colour, playerProvider, enable_long_press_menu, modifier)
    }

    override val url: String get() = "https://music.youtube.com/playlist?list=$id"
}
