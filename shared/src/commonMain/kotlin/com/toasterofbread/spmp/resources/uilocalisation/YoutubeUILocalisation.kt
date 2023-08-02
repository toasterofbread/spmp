package com.toasterofbread.spmp.resources.uilocalisation

import SpMp
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.mediaitem.Artist
import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.model.mediaitem.PlaylistData
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.resources.uilocalisation.localised.Languages
import com.toasterofbread.spmp.resources.uilocalisation.localised.getYoutubeArtistPageLocalisations
import com.toasterofbread.spmp.resources.uilocalisation.localised.getYoutubeFilterChipsLocalisations
import com.toasterofbread.spmp.resources.uilocalisation.localised.getYoutubeHomeFeedLocalisations
import com.toasterofbread.spmp.resources.uilocalisation.localised.getYoutubeOwnChannelLocalisations
import com.toasterofbread.spmp.resources.uilocalisation.localised.getYoutubeSearchPageLocalisations

class LocalisedYoutubeString(
    val key: String,
    val type: Type,
    @Suppress("MemberVisibilityCanBePrivate")
    val source_language: Int? = null
) {
    private var localised: Pair<String, YoutubeUILocalisation.StringID?>? = null

    enum class Type {
        RAW,
        APP,
        HOME_FEED,
        OWN_CHANNEL,
        ARTIST_PAGE,
        SEARCH_PAGE,
        FILTER_CHIP;

        fun create(key: String, source_language: Int? = current_source_language): LocalisedYoutubeString =
            LocalisedYoutubeString(key, this, source_language)
    }

    init {
        if (type != Type.RAW && type != Type.APP) {
            check(source_language != null)
        }
    }

    private fun getLocalised(): Pair<String, YoutubeUILocalisation.StringID?> {
        if (localised == null) {
            // TODO
            val data = SpMp.yt_ui_localisation

            val strings = when (type) {
                Type.RAW -> {
                    localised = Pair(key, null)
                    return localised!!
                }
                Type.APP -> {
                    localised = Pair(getString(key), null)
                    return localised!!
                }
                Type.HOME_FEED -> data.HOME_FEED_STRINGS
                Type.OWN_CHANNEL -> data.OWN_CHANNEL_STRINGS
                Type.ARTIST_PAGE -> data.ARTIST_PAGE_STRINGS
                Type.SEARCH_PAGE -> data.SEARCH_PAGE_STRINGS
                Type.FILTER_CHIP -> data.FILTER_CHIPS
            }

            localised = data.getLocalised(key, strings, source_language!!)
            
            if (localised == null) {
                localised = Pair(key, null)
                SpMp.Log.warning("String key '$key' of type $type has not been localised (source lang=${SpMp.getLanguageCode(source_language)})")
            }
        }

        return localised!!
    }

    fun getString(): String {
        return getLocalised().first
    }

    fun getID(): YoutubeUILocalisation.StringID? {
        return getLocalised().second
    }

    companion object {
        private val current_source_language: Int get() = Settings.KEY_LANG_DATA.get()

        fun mediaItemPage(key: String, item: MediaItemData): LocalisedYoutubeString {
            check(item is Artist || (item is PlaylistData && item.browse_params != null)) {
                "$key | $item"
            }
            return Type.ARTIST_PAGE.create(key)
        }
    }
}

class YoutubeUILocalisation(languages: Languages) {
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

    internal val HOME_FEED_STRINGS: LocalisationSet = getYoutubeHomeFeedLocalisations(languages)
    internal val OWN_CHANNEL_STRINGS: LocalisationSet = getYoutubeOwnChannelLocalisations(languages)
    internal val ARTIST_PAGE_STRINGS: LocalisationSet = getYoutubeArtistPageLocalisations(languages)
    internal val SEARCH_PAGE_STRINGS: LocalisationSet = getYoutubeSearchPageLocalisations(languages)
    internal val FILTER_CHIPS: LocalisationSet = getYoutubeFilterChipsLocalisations(languages)

    internal fun getLocalised(string: String, localisations: LocalisationSet, source_language: Int): Pair<String, StringID?>? {
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
}
