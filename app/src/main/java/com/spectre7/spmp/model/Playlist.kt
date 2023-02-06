package com.spectre7.spmp.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.spectre7.spmp.api.BrowseData
import com.spectre7.spmp.ui.component.MediaItemLayout
import com.spectre7.spmp.ui.component.PlaylistPreviewLong
import com.spectre7.spmp.ui.component.PlaylistPreviewSquare
import com.spectre7.spmp.ui.layout.PlayerViewContext

class Playlist private constructor (
    id: String
): MediaItem(id) {

    // Data
    override lateinit var title: String
    lateinit var feed_layout: MediaItemLayout
    var artist: Artist? = null

    companion object {
        private val playlists: MutableMap<String, Playlist> = mutableMapOf()

        @Synchronized
        fun fromId(id: String): Playlist {
            return playlists.getOrElse(id) {
                val playlist = Playlist(id)
                playlists[id] = playlist
                return playlist
            }.getOrReplacedWith() as Playlist
        }

        fun serialisable(id: String): Serialisable {
            return Serialisable(Type.PLAYLIST.ordinal, id)
        }
    }

    @Composable
    override fun PreviewSquare(content_colour: () -> Color, player: PlayerViewContext, enable_long_press_menu: Boolean, modifier: Modifier) {
        PlaylistPreviewSquare(this, content_colour, player, enable_long_press_menu, modifier)
    }

    @Composable
    override fun PreviewLong(content_colour: () -> Color, player: PlayerViewContext, enable_long_press_menu: Boolean, modifier: Modifier) {
        PlaylistPreviewLong(this, content_colour, player, enable_long_press_menu, modifier)
    }

    override fun getAssociatedArtist(): Artist? {
        return artist
    }

    override fun _getUrl(): String {
        return "https://music.youtube.com/playlist?list=$id"
    }

    override fun subInitWithData(data: Any) {
        if (data !is BrowseData) {
            throw ClassCastException(data.javaClass.name)
        }

        title = data.name!!
        feed_layout = data.feed_rows.single().toMediaItemLayout()

        if (data.subscribe_channel_id != null) {
            artist = Artist.fromId(data.subscribe_channel_id!!).loadData() as Artist
        }
    }
}
