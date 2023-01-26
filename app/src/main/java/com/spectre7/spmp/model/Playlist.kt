package com.spectre7.spmp.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import java.time.Instant
import java.util.*

class Playlist private constructor (
    private val _id: String
): MediaItem() {

    // Data
    lateinit var title: String
    lateinit var upload_date: Date

    companion object {
        private val playlists: MutableMap<String, Playlist> = mutableMapOf()

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
        // TODO
    }

    @Composable
    override fun PreviewLong(content_colour: Color, onClick: (() -> Unit)?, onLongClick: (() -> Unit)?, modifier: Modifier) {
        // TODO
    }

    override fun _getId(): String {
        return _id
    }

    override fun _getUrl(): String {
        return "https://music.youtube.com/playlist?list=$id"
    }

    override fun subInitWithData(data: YTApiDataResponse) {
        if (data.snippet == null) {
            throw RuntimeException("Data snippet is null\n$data")
        }
        title = data.snippet.title
        upload_date = Date.from(Instant.parse(data.snippet.publishedAt))
    }
}
