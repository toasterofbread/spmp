package com.toasterofbread.spmp.resources.uilocalisation

import com.toasterofbread.spmp.resources.uilocalisation.localised.UILanguages
import com.toasterofbread.spmp.resources.uilocalisation.localised.getYoutubeArtistPageLocalisations
import com.toasterofbread.spmp.resources.uilocalisation.localised.getYoutubeFilterChipsLocalisations
import com.toasterofbread.spmp.resources.uilocalisation.localised.getYoutubeHomeFeedLocalisations
import com.toasterofbread.spmp.resources.uilocalisation.localised.getYoutubeOwnChannelLocalisations
import com.toasterofbread.spmp.resources.uilocalisation.localised.getYoutubeSearchPageLocalisations

class YoutubeUILocalisation(languages: UILanguages) {
    enum class StringID {
        ARTIST_ROW_SINGLES,
        ARTIST_ROW_SONGS,
        ARTIST_ROW_VIDEOS,
        ARTIST_ROW_OTHER
    }

    class LocalisationSet {
        val items: MutableList<Map<String, Pair<String, String?>>> = mutableListOf()
        val item_ids: MutableMap<Int, StringID> = mutableMapOf()

        @Synchronized
        fun add(vararg strings: Pair<String, String>, id: StringID? = null) {
            if (id != null) {
                item_ids[items.size] = id
            }

            items.add(mutableMapOf<String, Pair<String, String?>>().also { map ->
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
}
