package com.toasterofbread.spmp.resources.uilocalisation

import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType

data class LocalisedYoutubeString constructor(
    val key: String,
    val type: Type,
    @Suppress("MemberVisibilityCanBePrivate")
    val source_language: Int
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

        fun create(key: String, source_language: Int = current_source_language): LocalisedYoutubeString =
            LocalisedYoutubeString(key, this, source_language)
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
                SpMp.onUnlocalisedStringFound(this)
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

        fun mediaItemPage(key: String, item_type: MediaItemType): LocalisedYoutubeString =
            when (item_type) {
                MediaItemType.ARTIST, MediaItemType.PLAYLIST_BROWSEPARAMS -> Type.ARTIST_PAGE.create(key)
                else -> {
                    SpMp.onUnlocalisedStringFound(item_type.toString(), key, current_source_language)
                    Type.RAW.create(key)
                }
            }
    }
}
