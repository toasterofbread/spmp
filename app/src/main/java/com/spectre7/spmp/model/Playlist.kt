package com.spectre7.spmp.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.spectre7.spmp.api.BrowseData
import com.spectre7.spmp.ui.component.MediaItemLayout
import com.spectre7.spmp.ui.component.PlaylistPreviewLong
import com.spectre7.spmp.ui.component.PlaylistPreviewSquare
import com.spectre7.spmp.ui.layout.PlayerViewContext

class Playlist private constructor (
    id: String
): MediaItem(id) {

    var feed_layouts: List<MediaItemLayout>? = null

    class PlaylistData(id: String): Data(id) {
        var feed_layouts: List<MediaItemLayout>? = null

        override fun initWithData(data: JsonObject, klaxon: Klaxon): Data {
            val layouts = data.array<MediaItemLayout>("feed_layouts")
            if (layouts != null) {
                feed_layouts = klaxon.parseFromJsonArray(layouts)
            }
            return super.initWithData(data, klaxon)
        }
    }

    override fun initWithData(data: Data): MediaItem {
        if (data !is PlaylistData) {
            throw ClassCastException(data.javaClass.name)
        }

        feed_layouts = data.feed_layouts
        return super.initWithData(data)
    }

    override fun isLoaded(): Boolean {
        return super.isLoaded() && feed_layouts != null
    }

    override fun getJsonValues(klaxon: Klaxon): String {
        return """
            "feed_layouts": ${klaxon.toJsonString(feed_layouts)},
        """
    }

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
