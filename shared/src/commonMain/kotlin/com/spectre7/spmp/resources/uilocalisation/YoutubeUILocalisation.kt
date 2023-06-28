package com.spectre7.spmp.resources.uilocalisation

import SpMp
import com.spectre7.spmp.model.mediaitem.enums.MediaItemType
import com.spectre7.spmp.model.Settings
import com.spectre7.spmp.resources.getString
import org.apache.commons.lang3.time.DurationFormatUtils
import java.time.Duration

class LocalisedYoutubeString(
    val key: String,
    val type: Type,
    val source_language: Int? = null
) {
    private var localised: Pair<String, YoutubeUILocalisation.StringID?>? = null

    enum class Type {
        RAW,
        APP,
        HOME_FEED,
        OWN_CHANNEL,
        ARTIST_PAGE
    }

    init {
        if (type != Type.RAW && type != Type.APP) {
            check(source_language != null)
        }
    }

    private fun getLocalised() {
        if (localised == null) {
            localised = when (type) {
                Type.RAW -> Pair(key, null)
                Type.APP -> Pair(getString(key), null)
                Type.HOME_FEED -> {
                    val localisation = SpMp.yt_ui_localisation.localiseHomeFeedString(key, source_language!!)
                    if (localisation != null) {
                        localisation
                    } else {
                        println("WARNING: Using raw key '$key' as home feed string")
                        Pair(key, null)
                    }
                }
                Type.OWN_CHANNEL -> SpMp.yt_ui_localisation.localiseOwnChannelString(key, source_language!!)
                Type.ARTIST_PAGE -> SpMp.yt_ui_localisation.localiseArtistPageString(key, source_language!!)
            } ?: throw NotImplementedError("Key: '$key', Type: $type, Source lang: ${SpMp.getLanguageCode(source_language!!)}")
        }
    }

    fun getString(): String {
        getLocalised()
        return localised!!.first
    }

    fun getID(): YoutubeUILocalisation.StringID? {
        getLocalised()
        return localised!!.second
    }

    companion object {
        private val current_source_language: Int get() = Settings.KEY_LANG_DATA.get()

        fun temp(string: String): LocalisedYoutubeString = LocalisedYoutubeString(string, Type.RAW, current_source_language)

        fun raw(string: String): LocalisedYoutubeString = LocalisedYoutubeString(string, Type.RAW, current_source_language)
        fun app(key: String): LocalisedYoutubeString = LocalisedYoutubeString(key, Type.APP, current_source_language)
        fun homeFeed(key: String): LocalisedYoutubeString = LocalisedYoutubeString(key, Type.HOME_FEED, current_source_language)
        fun ownChannel(key: String): LocalisedYoutubeString = LocalisedYoutubeString(key, Type.OWN_CHANNEL, current_source_language)
        fun artistPage(key: String): LocalisedYoutubeString = LocalisedYoutubeString(key, Type.ARTIST_PAGE, current_source_language)

        fun filterChip(key: String): Int? = SpMp.yt_ui_localisation.getFilterChipIndex(key, current_source_language)
        fun filterChip(index: Int): String = SpMp.yt_ui_localisation.getFilterChip(index)

        fun mediaItemPage(key: String, item_type: MediaItemType): LocalisedYoutubeString =
            when (item_type) {
                MediaItemType.ARTIST, MediaItemType.PLAYLIST_BROWSEPARAMS -> artistPage(key)
                else -> throw NotImplementedError(item_type.name)
            }
    }
}

class YoutubeUILocalisation(languages: List<String>) {
    enum class StringID {
        ARTIST_PAGE_SINGLES
    }

    class LocalisationSet {
        val items: MutableList<Map<Int, Pair<String, String?>>> = mutableListOf()
        val item_ids: MutableMap<Int, StringID> = mutableMapOf()
        
        @Synchronized
        fun add(vararg strings: Pair<Int, String>, id: StringID? = null) {
            if (id != null) {
                item_ids[items.size] = id
            }

            items.add(mutableMapOf<Int, Pair<String, String?>>().also { map ->
                for (string in strings) {
                    val existing = map[string.first]
                    if (existing != null) {
                        map[string.first] = existing.copy(second = string.second)
                    }
                    else {
                        map[string.first] = Pair(string.second, null)
                    }
                }
            })
        }
    }

    private fun getLanguage(key: String, languages: List<String>): Int =
        languages.indexOf(key).also {
            check(it != -1)
        }

    private val HOME_FEED_STRINGS: LocalisationSet = getYoutubeHomeFeedLocalisations { getLanguage(it, languages) }
    private val OWN_CHANNEL_STRINGS: LocalisationSet = getYoutubeOwnChannelLocalisations { getLanguage(it, languages) }
    private val ARTIST_PAGE_STRINGS: LocalisationSet = getYoutubeArtistPageLocalisations { getLanguage(it, languages) }
    private val FILTER_CHIPS: LocalisationSet = getYoutubeFilterChipsLocalisations { getLanguage(it, languages) }

    private fun getLocalised(string: String, localisations: LocalisationSet, source_language: Int): Pair<String, StringID?>? {
        val target: Int = Settings.KEY_LANG_UI.get()

        for (localisation in localisations.items.withIndex()) {
            if (localisation.value[source_language]?.first == string) {
                val string_data: Pair<String, String?> = localisation.value[target] ?: break
                val id = localisations.item_ids[localisation.index]
                return Pair(string_data.second ?: string_data.first, id)
            }
        }

        return null
    }

    fun localiseHomeFeedString(string: String, source_language: Int): Pair<String, StringID?>? = getLocalised(string, HOME_FEED_STRINGS, source_language)
    fun localiseOwnChannelString(string: String, source_language: Int): Pair<String, StringID?>? = getLocalised(string, OWN_CHANNEL_STRINGS, source_language)
    fun localiseArtistPageString(string: String, source_language: Int): Pair<String, StringID?>? = getLocalised(string, ARTIST_PAGE_STRINGS, source_language)

    fun getFilterChipIndex(string: String, source_language: Int): Int? {
        val index = FILTER_CHIPS.items.indexOfFirst { it[source_language]?.first == string }
        if (index == -1) {
            return null
        }
        return index
    }
    fun getFilterChip(index: Int): String {
        val chip = FILTER_CHIPS.items.elementAt(index)[Settings.KEY_LANG_UI.get()]!!
        return chip.second ?: chip.first
    }
}
