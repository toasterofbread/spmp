package com.spectre7.spmp.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.spectre7.spmp.ui.component.ArtistPreview
import java.time.Instant
import java.util.*

class Artist private constructor (
    private val id: String
): YtItem() {

    // Data
    lateinit var name: String
    lateinit var description: String
    lateinit var creationDate: Date
    lateinit var thumbnail_url: String
    lateinit var thumbnail_hq_url: String
    lateinit var viewCount: String
    lateinit var subscriberCount: String
    lateinit var videoCount: String
    var hiddenSubscriberCount: Boolean = false

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

    override fun getId(): String {
        return id
    }

    override fun getThumbUrl(hq: Boolean): String {
        return if (hq) thumbnail_hq_url else thumbnail_url
    }

    override fun initWithData(data: ServerInfoResponse, onFinished: () -> Unit) {
        name = data.snippet.title
        description = data.snippet.description!!
        creationDate = Date.from(Instant.parse(data.snippet.publishedAt))

        thumbnail_url = data.snippet.thumbnails.default.url
        thumbnail_hq_url = data.snippet.thumbnails.high.url

        viewCount = data.statistics.viewCount
        subscriberCount = data.statistics.subscriberCount!!
        hiddenSubscriberCount = data.statistics.hiddenSubscriberCount
        videoCount = data.statistics.videoCount!!

        loaded = true
        onFinished()
    }

    @Composable
    override fun Preview(large: Boolean, modifier: Modifier, colour: Color) {
        return ArtistPreview(this, large, colour, modifier)
    }

    fun getFormattedSubscriberCount(): String {
        val subs = subscriberCount.toInt()
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