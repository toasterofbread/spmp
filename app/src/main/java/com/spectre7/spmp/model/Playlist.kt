package com.spectre7.spmp.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.spectre7.spmp.api.BrowseData
import com.spectre7.spmp.ui.component.PlaylistPreviewLong
import com.spectre7.spmp.ui.component.PlaylistPreviewSquare

class Playlist private constructor (
    id: String
): MediaItem(id) {

    // Data
    lateinit var title: String
    lateinit var feed_rows: List<MediaItemRow>

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
    override fun PreviewSquare(content_colour: Color, onClick: (() -> Unit)?, onLongClick: (() -> Unit)?, modifier: Modifier) {
        PlaylistPreviewSquare(this, content_colour, modifier, onClick, onLongClick)
    }

    @Composable
    override fun PreviewLong(content_colour: Color, onClick: (() -> Unit)?, onLongClick: (() -> Unit)?, modifier: Modifier) {
        PlaylistPreviewLong(this, content_colour, modifier, onClick, onLongClick)
    }

    override fun _getUrl(): String {
        return "https://music.youtube.com/playlist?list=$id"
    }

    override fun subInitWithData(data: Any) {
        if (data !is BrowseData) {
            throw ClassCastException(data.javaClass.name)
        }

        title = data.name!!
        feed_rows = List(data.feed_rows.size) { i ->
            data.feed_rows[i].toMediaItemRow()
        }
    }
}
