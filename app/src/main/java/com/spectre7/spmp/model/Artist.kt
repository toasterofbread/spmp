package com.spectre7.spmp.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.spectre7.spmp.ui.component.ArtistPreview
import java.time.Instant
import java.util.*

class Artist private constructor (
    private val _id: String
): MediaItem() {

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

        fun fromId(id: String): Artist {
            return artists.getOrElse(id) {
                val ret = Artist(id)
                artists[id] = ret
                return ret
            }
        }
    }

    override fun _getId(): String {
        return _id
    }

    override fun _getUrl(): String {
        return "https://music.youtube.com/channel/$id"
    }

    override fun subInitWithData(data: YTApiDataResponse) {
        name = data.snippet!!.title
        description = data.snippet.description!!
        creation_date = Date.from(Instant.parse(data.snippet.publishedAt))

        view_count = data.statistics!!.view_count
        subscriber_count = data.statistics.subscriber_count!!
        hidden_subscriber_count = data.statistics.hidden_subscriber_count
        videoCount = data.statistics.videoCount!!
    }

    @Composable
    override fun Preview(large: Boolean, modifier: Modifier, colour: Color) {
        return ArtistPreview(this, large, colour, modifier)
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