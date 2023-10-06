package com.toasterofbread.spmp.resources.uilocalisation

import SpMp
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.resources.getString

sealed interface LocalisedString {
    fun getString(): String
    fun getType(): Type

    fun serialise(): String {
        val data: String = when (this) {
            is RawLocalisedString -> raw_string
            is AppLocalisedString -> string_key
            is YoutubeLocalisedString -> "${type.ordinal},$index"
        }

        return "${getType().ordinal},$data"
    }

    enum class Type {
        RAW,
        APP,
        YOUTUBE;
    }

    companion object {
        fun deserialise(data: String): LocalisedString {
            val split = data.split(",", limit = 2)
            val type = Type.values()[split[0].toInt()]

            when (type) {
                Type.RAW -> return RawLocalisedString(split[1])
                Type.APP -> return AppLocalisedString(split[1])
                Type.YOUTUBE -> {
                    val (youtube_type_index, index) = split[1].split(",", limit = 2)
                    return YoutubeLocalisedString(
                        YoutubeLocalisedString.Type.values()[youtube_type_index.toInt()],
                        index.toInt()
                    )
                }
            }
        }
    }
}

data class RawLocalisedString(
    val raw_string: String
): LocalisedString {
    override fun getString(): String = raw_string
    override fun getType(): LocalisedString.Type = LocalisedString.Type.RAW
}

data class AppLocalisedString(
    val string_key: String
): LocalisedString {
    override fun getString(): String = getString(string_key)
    override fun getType(): LocalisedString.Type = LocalisedString.Type.APP
}

data class YoutubeLocalisedString(
    val type: Type,
    val index: Int
): LocalisedString {
    enum class Type {
        HOME_FEED,
        OWN_CHANNEL,
        ARTIST_PAGE,
        SEARCH_PAGE,
        FILTER_CHIP;

        fun createFromKey(key: String, source_language: String = SpMp.data_language): LocalisedString {
            val strings = getStringData()

            for (item in strings.items.withIndex()) {
                if (item.value[source_language]?.first == key) {
                    return YoutubeLocalisedString(
                        this,
                        item.index
                    )
                }
            }

            SpMp.onUnlocalisedStringFound(this.toString(), key, source_language)
            return RawLocalisedString(key)
        }
    }

    override fun getString(): String = getLocalised().let { it.second ?: it.first }
    override fun getType(): LocalisedString.Type = LocalisedString.Type.YOUTUBE

    fun getYoutubeStringId(): YoutubeUILocalisation.StringID? =
        type.getStringData().item_ids[index]

    private var localised: Pair<String, String?>? = null
    private fun getLocalised(): Pair<String, String?> {
        if (localised == null) {
            val strings = type.getStringData()
            try {
                return strings.items[index][SpMp.ui_language]!!
            }
            catch (e: Throwable) {
                throw RuntimeException("Could not get localised string ($index, ${SpMp.ui_language}, ${strings.items.toList()})", e)
            }
        }

        return localised!!
    }

    companion object {
        fun mediaItemPage(key: String, item_type: MediaItemType, source_language: String = SpMp.data_language): LocalisedString =
            when (item_type) {
                MediaItemType.ARTIST -> Type.ARTIST_PAGE.createFromKey(key)
                else -> {
                    SpMp.onUnlocalisedStringFound(item_type.toString(), key, source_language)
                    RawLocalisedString(key)
                }
            }
    }
}

private fun YoutubeLocalisedString.Type.getStringData(): YoutubeUILocalisation.LocalisationSet {
    val data = SpMp.yt_ui_localisation
    return when (this) {
        YoutubeLocalisedString.Type.HOME_FEED -> data.HOME_FEED_STRINGS
        YoutubeLocalisedString.Type.OWN_CHANNEL -> data.OWN_CHANNEL_STRINGS
        YoutubeLocalisedString.Type.ARTIST_PAGE -> data.ARTIST_PAGE_STRINGS
        YoutubeLocalisedString.Type.SEARCH_PAGE -> data.SEARCH_PAGE_STRINGS
        YoutubeLocalisedString.Type.FILTER_CHIP -> data.FILTER_CHIPS
    }
}
