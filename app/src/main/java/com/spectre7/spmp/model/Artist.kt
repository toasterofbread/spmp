package com.spectre7.spmp.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.api.ArtistData
import com.spectre7.spmp.ui.component.ArtistPreviewLong
import com.spectre7.spmp.ui.component.ArtistPreviewSquare
import java.time.Instant
import java.util.*

class Artist private constructor (
    id: String
): MediaItem(id) {

    // Data
    lateinit var name: String
    lateinit var description: String
    lateinit var creation_date: Date
    lateinit var view_count: String
    lateinit var subscriber_count: String
    lateinit var videoCount: String
    var hidden_subscriber_count: Boolean = false

    companion object {
        private val artists: MutableMap<String, Artist> = mutableMapOf()

        @Synchronized
        fun fromId(id: String): Artist {
            return artists.getOrElse(id) {
                val ret = Artist(id)
                artists[id] = ret
                return ret
            }
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
        if (data !is YTApiDataResponse) {
            throw ClassCastException(data.javaClass.name)
        }

        val snippet = data.snippet!!
        val loc = data.getLocalisation(MainActivity.data_language)

        name = loc?.title ?: snippet.title
        description = loc?.description ?: snippet.description!!

        creation_date = Date.from(Instant.parse(snippet.publishedAt))

        view_count = data.statistics!!.viewCount
        subscriber_count = data.statistics.subscriberCount!!
        hidden_subscriber_count = data.statistics.hiddenSubscriberCount
        videoCount = data.statistics.videoCount!!
    }

    fun getFormattedSubscriberCount(): String {
        val subs = subscriber_count.toInt()
        if (subs >= 1000000) {
            return "${subs / 1000000}M"
        }
        else if (subs >= 1000) {
            return "${subs / 1000}K"
        }
        else {
            return "$subs"
        }
    }

}