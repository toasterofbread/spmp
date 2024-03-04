package com.toasterofbread.spmp.resources.uilocalisation

import SpMp
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.getDataLanguage
import com.toasterofbread.spmp.platform.getUiLanguage
import com.toasterofbread.spmp.platform.getDefaultLanguage
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.resources.uilocalisation.localised.getByLanguage

sealed interface LocalisedString {
    fun getString(context: AppContext): String
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
            val split: List<String> = data.split(",", limit = 2)

            try {
                val type = Type.entries[split[0].toInt()]

                when (type) {
                    Type.RAW -> return RawLocalisedString(split[1])
                    Type.APP -> return AppLocalisedString(split[1])
                    Type.YOUTUBE -> {
                        if (split.size < 2) {
                            return RawLocalisedString("")
                        }

                        val (youtube_type_index, index) = split[1].split(",", limit = 2)
                        return YoutubeLocalisedString(
                            YoutubeLocalisedString.Type.entries[youtube_type_index.toInt()],
                            index.toInt()
                        )
                    }
                }
            }
            catch (e: Throwable) {
                throw RuntimeException("LocalisedString deserialisation failed '$data' $split", e)
            }
        }
    }
}

data class RawLocalisedString(
    val raw_string: String
): LocalisedString {
    override fun getString(context: AppContext): String = raw_string
    override fun getType(): LocalisedString.Type = LocalisedString.Type.RAW
}

data class AppLocalisedString(
    val string_key: String
): LocalisedString {
    override fun getString(context: AppContext): String = getString(string_key)
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

        fun createFromKey(key: String, context: AppContext): LocalisedString {
            return createFromKey(key, context.getDataLanguage())
        }

        fun createFromKey(key: String, source_language: String): LocalisedString {
            val strings: YoutubeUILocalisation.LocalisationSet = getStringData()

            for ((index, item) in strings.items.withIndex()) {
                val by_language = item.getByLanguage(source_language)
                if (by_language?.value?.value?.first == key) {
                    return YoutubeLocalisedString(this, index)
                }
            }

            SpMp.onUnlocalisedStringFound(this.toString(), key, source_language)
            return RawLocalisedString(key)
        }
    }

    override fun getString(context: AppContext): String = getLocalised(context).let { it.second ?: it.first }
    override fun getType(): LocalisedString.Type = LocalisedString.Type.YOUTUBE

    fun getYoutubeStringId(): YoutubeUILocalisation.StringID? =
        type.getStringData().item_ids[index]

    private var localised: Pair<String, String?>? = null
    private fun getLocalised(context: AppContext): Pair<String, String?> {
        if (localised == null) {
            val strings: YoutubeUILocalisation.LocalisationSet = type.getStringData()

            val item: Map<String, Pair<String, String?>>? = strings.items.getOrNull(index)
            if (item == null) {
                throw RuntimeException("Could not get localised string item ($index, ${strings.items.toList()})")
            }

            val ui_language: String = context.getUiLanguage()
            localised = getLocalisationSetItemString(context, item, ui_language)
        }

        return localised!!
    }

    private fun getLocalisationSetItemString(
        context: AppContext,
        item: Map<String, Pair<String, String?>>,
        language: String
    ): Pair<String, String?> {
        try {
            return item.getByLanguage(language)!!.value.value
        }
        catch (e: Throwable) {
            val default_language: String = context.getDefaultLanguage()
            if (default_language == language) {
                throw RuntimeException("Could not get localised string ($index, $language, $item)")
            }

            println("Could not get localised string, falling back to default language ($index, $language, $item)")
            return getLocalisationSetItemString(context, item, default_language)
        }
    }

    companion object {
        fun mediaItemPage(key: String, item_type: MediaItemType, context: AppContext, source_language: String = context.getDataLanguage()): LocalisedString =
            when (item_type) {
                MediaItemType.ARTIST -> Type.ARTIST_PAGE.createFromKey(key, context)
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
