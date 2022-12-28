package com.spectre7.spmp.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.spectre7.spmp.ui.component.ArtistPreview
import java.time.Instant
import java.util.*

class Artist private constructor (
    private val _id: String
): YtItem() {

    // Data
    lateinit var name: String
    lateinit var description: String
    lateinit var creationDate: Date
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

    override fun _getId(): String {
        return _id
    }

    override fun initWithData(data: ServerInfoResponse, onFinished: () -> Unit) {
        name = data.snippet!!.title
        description = data.snippet.description!!
        creationDate = Date.from(Instant.parse(data.snippet.publishedAt))

        viewCount = data.statistics!!.viewCount
        subscriberCount = data.statistics.subscriberCount!!
        hiddenSubscriberCount = data.statistics.hiddenSubscriberCount
        videoCount = data.statistics.videoCount!!

        loaded = true
        super.initWithData(data, onFinished)
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