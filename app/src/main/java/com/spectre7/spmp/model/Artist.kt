package com.spectre7.spmp.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.spectre7.spmp.api.BrowseData
import com.spectre7.spmp.ui.component.ArtistPreviewLong
import com.spectre7.spmp.ui.component.ArtistPreviewSquare

class Artist private constructor (
    id: String
): MediaItem(id) {

    // Data
    lateinit var name: String
    var description: String? = null
    lateinit var feed_rows: List<MediaItemRow>

    companion object {
        private val artists: MutableMap<String, Artist> = mutableMapOf()

        @Synchronized
        fun fromId(id: String): Artist {
            return artists.getOrElse(id) {
                val ret = Artist(id)
                artists[id] = ret
                return ret
            }.getOrReplacedWith() as Artist
        }

        fun serialisable(id: String): Serialisable {
            return Serialisable(Type.ARTIST.ordinal, id)
        }
    }

    @Composable
    override fun PreviewSquare(content_colour: Color, onClick: (() -> Unit)?, onLongClick: (() -> Unit)?, modifier: Modifier) {
        ArtistPreviewSquare(this, content_colour, modifier, onClick, onLongClick)
    }

    @Composable
    override fun PreviewLong(content_colour: Color, onClick: (() -> Unit)?, onLongClick: (() -> Unit)?, modifier: Modifier) {
        ArtistPreviewLong(this, content_colour, modifier, onClick, onLongClick)
    }

    override fun _getUrl(): String {
        return "https://music.youtube.com/channel/$id"
    }

    override fun subInitWithData(data: Any) {
        if (data !is BrowseData) {
            throw ClassCastException(data.javaClass.name)
        }

        name = data.name
        description = data.description
        feed_rows = List(data.feed_rows.size) { i ->
            data.feed_rows[i].toMediaItemRow()
        }
    }

    fun getFormattedSubscriberCount(): String {
        return "Unknown"
//        val subs = subscriber_count.toInt()
//        if (subs >= 1000000) {
//            return "${subs / 1000000}M"
//        }
//        else if (subs >= 1000) {
//            return "${subs / 1000}K"
//        }
//        else {
//            return "$subs"
//        }
    }

}