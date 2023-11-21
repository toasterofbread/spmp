package com.toasterofbread.spmp.youtubeapi

import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistData
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.resources.getString

abstract class RadioBuilderEndpoint: YoutubeApi.Endpoint() {
    abstract suspend fun getRadioBuilderArtists(
        selectThumbnail: (List<MediaItemThumbnailProvider.Thumbnail>) -> MediaItemThumbnailProvider.Thumbnail
    ): Result<List<RadioBuilderArtist>>

    abstract fun buildRadioToken(artists: Set<RadioBuilderArtist>, modifiers: Set<RadioBuilderModifier?>): String

    abstract suspend fun getBuiltRadio(radio_token: String, context: AppContext): Result<RemotePlaylistData?>
}

data class RadioBuilderArtist(
    val name: String,
    val token: String,
    val thumbnail: MediaItemThumbnailProvider.Thumbnail
)

sealed interface RadioBuilderModifier {
    val string: String?
    fun getReadable(): String

    enum class Internal: RadioBuilderModifier {
        ARTIST;

        override val string: String? get() = throw IllegalStateException()
        override fun getReadable(): String = throw IllegalStateException()
    }

    enum class Variety: RadioBuilderModifier {
        LOW, MEDIUM, HIGH;
        override val string: String? get() = when (this) {
            LOW -> "rX"
            MEDIUM -> null
            HIGH -> "rZ"
        }
        override fun getReadable(): String = getString(when (this) {
            LOW -> "radio_builder_modifier_variety_low"
            MEDIUM -> "radio_builder_modifier_variety_medium"
            HIGH -> "radio_builder_modifier_variety_high"
        })
    }

    enum class SelectionType: RadioBuilderModifier {
        FAMILIAR, BLEND, DISCOVER;
        override val string: String? get() = when (this) {
            FAMILIAR -> "iY"
            BLEND -> null
            DISCOVER -> "iX"
        }
        override fun getReadable(): String = getString(when (this) {
            FAMILIAR -> "radio_builder_modifier_selection_type_familiar"
            BLEND -> "radio_builder_modifier_selection_type_blend"
            DISCOVER -> "radio_builder_modifier_selection_type_discover"
        })
    }

    enum class FilterA: RadioBuilderModifier {
        POPULAR, HIDDEN, NEW;
        override val string: String? get() = when (this) {
            POPULAR -> "pY"
            HIDDEN -> "pX"
            NEW -> "dX"
        }
        override fun getReadable(): String = getString(when (this) {
            POPULAR -> "radio_builder_modifier_filter_a_popular"
            HIDDEN -> "radio_builder_modifier_filter_a_hidden"
            NEW -> "radio_builder_modifier_filter_a_new"
        })
    }

    enum class FilterB: RadioBuilderModifier {
        PUMP_UP, CHILL, UPBEAT, DOWNBEAT, FOCUS;
        override val string: String? get() = when (this) {
            PUMP_UP -> "mY"
            CHILL -> "mX"
            UPBEAT -> "mb"
            DOWNBEAT -> "mc"
            FOCUS -> "ma"
        }
        override fun getReadable(): String = getString(when (this) {
            PUMP_UP -> "radio_builder_modifier_filter_pump_up"
            CHILL -> "radio_builder_modifier_filter_chill"
            UPBEAT -> "radio_builder_modifier_filter_upbeat"
            DOWNBEAT -> "radio_builder_modifier_filter_downbeat"
            FOCUS -> "radio_builder_modifier_filter_focus"
        })
    }

    companion object {
        fun fromString(modifier: String): RadioBuilderModifier? {
            return when (modifier) {
                "iY" -> SelectionType.FAMILIAR
                "iX" -> SelectionType.DISCOVER
                "pY" -> FilterA.POPULAR
                "pX" -> FilterA.HIDDEN
                "dX" -> FilterA.NEW
                "mY" -> FilterB.PUMP_UP
                "mX" -> FilterB.CHILL
                "mb" -> FilterB.UPBEAT
                "mc" -> FilterB.DOWNBEAT
                "ma" -> FilterB.FOCUS
                "rX" -> Variety.LOW
                "rZ" -> Variety.HIGH
                else -> null
            }
        }
    }
}
