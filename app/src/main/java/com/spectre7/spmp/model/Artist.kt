package com.spectre7.spmp.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.spectre7.spmp.api.isSubscribedToArtist
import com.spectre7.spmp.api.subscribeOrUnsubscribeArtist
import com.spectre7.spmp.ui.component.ArtistPreviewLong
import com.spectre7.spmp.ui.component.ArtistPreviewSquare
import com.spectre7.spmp.ui.component.MediaItemLayout
import com.spectre7.spmp.ui.layout.PlayerViewContext
import com.spectre7.utils.sendToast
import kotlin.concurrent.thread

class Artist private constructor (
    id: String
): MediaItem(id) {

    init {
        artist = this
    }

    var feed_layouts: List<MediaItemLayout>? = null
    var subscribe_channel_id: String? = null

    var subscribed: Boolean? by mutableStateOf(null)

    class ArtistData(id: String): Data(id) {
        var feed_layouts: List<MediaItemLayout>? = null
        var subscribe_channel_id: String? = null

        override fun initWithData(data: JsonObject, klaxon: Klaxon): Data {
            val layouts = data.array<MediaItemLayout>("feed_layouts")
            if (layouts != null) {
                feed_layouts = klaxon.parseFromJsonArray(layouts)
            }
            subscribe_channel_id = data.string("subscribe_channel_id")
            return super.initWithData(data, klaxon)
        }
    }

    override fun initWithData(data: Data): MediaItem {
        if (data !is ArtistData) {
            throw ClassCastException(data.javaClass.name)
        }

        feed_layouts = data.feed_layouts
        subscribe_channel_id = data.subscribe_channel_id ?: id
        return super.initWithData(data)
    }

    override fun isLoaded(): Boolean {
        return super.isLoaded() && feed_layouts != null && subscribe_channel_id != null
    }

    override fun getJsonValues(klaxon: Klaxon): String {
        return """
            "feed_layouts": ${klaxon.toJsonString(feed_layouts)},
            "subscribe_channel_id": ${stringToJson(subscribe_channel_id)}
        """
    }

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
    }

    @Composable
    override fun PreviewSquare(content_colour: () -> Color, playerProvider: () -> PlayerViewContext, enable_long_press_menu: Boolean, modifier: Modifier) {
        ArtistPreviewSquare(this, content_colour, playerProvider, enable_long_press_menu, modifier)
    }

    @Composable
    override fun PreviewLong(content_colour: () -> Color, playerProvider: () -> PlayerViewContext, enable_long_press_menu: Boolean, modifier: Modifier) {
        ArtistPreviewLong(this, content_colour, playerProvider, enable_long_press_menu, modifier)
    }

    override val url: String get() = "https://music.youtube.com/channel/$id"

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

    fun updateSubscribed() {
        subscribed = isSubscribedToArtist(this).getNullableDataOrThrow()
    }

    fun toggleSubscribe(toggle_before_fetch: Boolean = false, notify_failure: Boolean = false) {
        thread {
            if (subscribed == null) {
                throw IllegalStateException()
            }

            val target = !subscribed!!

            if (toggle_before_fetch) {
                subscribed = target
            }

            subscribeOrUnsubscribeArtist(this, target).getDataOrThrow()
            updateSubscribed()

            if (notify_failure && subscribed != target) {
                sendToast(
                    if (target) "Subscribing to $title failed"
                    else        "Unsubscribed from $title failed"
                )
            }
        }
    }

}