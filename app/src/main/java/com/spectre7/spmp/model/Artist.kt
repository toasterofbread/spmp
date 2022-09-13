package com.spectre7.spmp.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.spectre7.spmp.api.DataApi
import com.spectre7.spmp.ui.components.ArtistPreview
import com.spectre7.spmp.MainActivity
import java.util.*

data class ArtistData (
    val locale: String?,
    var name: String,
    val description: String
) {
    init {
        name = name.removeSuffix(" - Topic")
    }
}

data class Artist (
    private val id: String,
    val nativeData: ArtistData,
    val creationDate: Date,
    val thumbnail_url: String,
    val thumbnail_hq_url: String,

    val viewCount: String,
    val subscriberCount: String,
    val hiddenSubscriberCount: Boolean,
    val videoCount: String
): Previewable() {

    private var counterpartId: String? = null

    fun getCounterpartId(): String? {
        if (counterpartId == null) {
            counterpartId = MainActivity.youtube.getArtistCounterpartId(this)
        }
        return counterpartId
    }

    companion object {
        fun fromId(channel_id: String): Artist {
            return DataApi.getArtist(channel_id)!!
        }
    }

    override fun getId(): String {
        return id
    }

    override fun getThumbUrl(hq: Boolean): String {
        return if (hq) thumbnail_hq_url else thumbnail_url
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