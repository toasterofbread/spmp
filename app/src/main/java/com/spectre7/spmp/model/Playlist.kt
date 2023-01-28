package com.spectre7.spmp.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.ui.component.PlaylistPreviewLong
import com.spectre7.spmp.ui.component.PlaylistPreviewSquare
import java.time.Instant
import java.util.*

class Playlist private constructor (
    id: String
): MediaItem(id) {

    // Data
    lateinit var title: String
    lateinit var upload_date: Date

    companion object {
        private val playlists: MutableMap<String, Playlist> = mutableMapOf()

        @Synchronized
        fun fromId(id: String): Playlist {
            return playlists.getOrElse(id) {
                val playlist = Playlist(id)
                playlists[id] = playlist
                return playlist
            }
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
        if (data !is YTApiDataResponse) {
            throw ClassCastException(data.javaClass.name)
        }
        if (data.snippet == null) {
            throw RuntimeException("Data snippet is null\n$data")
        }

        title = data.getLocalisation(MainActivity.data_language)?.title ?: data.snippet.title
        upload_date = Date.from(Instant.parse(data.snippet.publishedAt))
    }
}
