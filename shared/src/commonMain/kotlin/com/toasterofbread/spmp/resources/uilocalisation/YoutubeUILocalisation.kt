package com.toasterofbread.spmp.resources.uilocalisation

import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.resources.uilocalisation.localised.Languages
import com.toasterofbread.spmp.resources.uilocalisation.localised.getYoutubeArtistPageLocalisations
import com.toasterofbread.spmp.resources.uilocalisation.localised.getYoutubeFilterChipsLocalisations
import com.toasterofbread.spmp.resources.uilocalisation.localised.getYoutubeHomeFeedLocalisations
import com.toasterofbread.spmp.resources.uilocalisation.localised.getYoutubeOwnChannelLocalisations
import com.toasterofbread.spmp.resources.uilocalisation.localised.getYoutubeSearchPageLocalisations

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
