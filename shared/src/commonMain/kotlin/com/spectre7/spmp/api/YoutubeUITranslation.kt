package com.spectre7.spmp.api

import com.spectre7.spmp.model.MediaItem
import com.spectre7.spmp.model.Settings
import com.spectre7.utils.getString

class LocalisedYoutubeString(
    val key: String,
    val type: Type,
    val source_language: Int? = null
) {
    enum class Type {
        RAW,
        COMMON,
        HOME_FEED,
        OWN_CHANNEL,
        ARTIST_PAGE
    }

    init {
        if (type != Type.RAW && type != Type.COMMON) {
            check(source_language != null)
        }
    }

    fun getString(): String = when (type) {
        Type.RAW -> key
        Type.COMMON -> getString(key)
        Type.HOME_FEED -> SpMp.yt_ui_translation.translateHomeFeedString(key, source_language!!)
        Type.OWN_CHANNEL -> SpMp.yt_ui_translation.translateOwnChannelString(key, source_language!!)
        Type.ARTIST_PAGE -> SpMp.yt_ui_translation.translateArtistPageString(key, source_language!!)
    } ?: throw NotImplementedError("Key: $key, Type: $type, Source lang: ${SpMp.languages.keys.elementAt(source_language!!)}")

    companion object {
        private val current_source_language: Int get() = Settings.KEY_LANG_DATA.get()

        fun temp(string: String): LocalisedYoutubeString = LocalisedYoutubeString(string, Type.RAW, current_source_language)

        fun raw(string: String): LocalisedYoutubeString = LocalisedYoutubeString(string, Type.RAW, current_source_language)
        fun common(key: String): LocalisedYoutubeString = LocalisedYoutubeString(key, Type.COMMON, current_source_language)
        fun homeFeed(key: String): LocalisedYoutubeString = LocalisedYoutubeString(key, Type.HOME_FEED, current_source_language)
        fun ownChannel(key: String): LocalisedYoutubeString = LocalisedYoutubeString(key, Type.OWN_CHANNEL, current_source_language)
        fun artistPage(key: String): LocalisedYoutubeString = LocalisedYoutubeString(key, Type.ARTIST_PAGE, current_source_language)

        fun mediaItemPage(key: String, item_type: MediaItem.Type): LocalisedYoutubeString =
            when (item_type) {
                MediaItem.Type.ARTIST -> artistPage(key)
                else -> throw NotImplementedError(item_type.name)
            }
    }
}

class YoutubeUITranslation(languages: Set<String>) {
    private val HOME_FEED_STRINGS: List<Map<Int, Pair<String, String?>>>
    private val OWN_CHANNEL_STRINGS: List<Map<Int, Pair<String, String?>>>
    private val ARTIST_PAGE_STRINGS: List<Map<Int, Pair<String, String?>>>

    private fun getTranslated(string: String, translations: List<Map<Int, Pair<String, String?>>>, source_language: Int): String? {
        val target: Int = Settings.KEY_LANG_UI.get()

        for (translation in translations) {
            if (translation[source_language]?.first == string) {
                val translated = translation[target] ?: break
                return translated.second ?: translated.first
            }
        }

        return null
    }

//    fun translateString(string: String): String {
//        return getStringOrNull(string)
//                ?: translateHomeFeedString(string)
//                ?: translateOwnChannelString(string)
//                ?: throw NotImplementedError(string)
//    }

    fun translateHomeFeedString(string: String, source_language: Int): String? = getTranslated(string, HOME_FEED_STRINGS, source_language)
    fun translateOwnChannelString(string: String, source_language: Int): String? = getTranslated(string, OWN_CHANNEL_STRINGS, source_language)
    fun translateArtistPageString(string: String, source_language: Int): String? = getTranslated(string, ARTIST_PAGE_STRINGS, source_language)

    init {
        fun MutableList<Map<Int, Pair<String, String?>>>.addString(vararg strings: Pair<Int, String>) {
            add(mutableMapOf<Int, Pair<String, String?>>().also { map ->
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

        val en = languages.indexOf("en")
        check(en != -1)
        val ja = languages.indexOf("ja")
        check(ja != -1)

        HOME_FEED_STRINGS = mutableListOf<Map<Int, Pair<String, String?>>>().apply {
            addString(
                en to "Listen again",
                ja to "もう一度聴く"
            )
            addString(
                en to "Quick picks",
                ja to "おすすめ"
            )
            addString(
                en to "START RADIO BASED ON A SONG",
                ja to "曲を選んでラジオを再生"
            )
            addString(
                en to "Covers and remixes",
                ja to "カバーとリミックス"
            )
            addString(
                en to "Covers and remixes",
                ja to "カバーとリミックス"
            )
            addString(
                en to "Forgotten favourites",
                ja to "最近聞いていないお気に入り"
            )
            addString(
                en to "TODO",
                ja to "ライブラリから"
            )
            addString(
                en to "コミュニティから",
                ja to "From the community"
            )
            addString(
                en to "Recommended music videos",
                ja to "おすすめのミュージック ビデオ"
            )
            addString(
                en to "Live performances",
                ja to "ライブ"
            )
            addString(
                en to "Recommended radios",
                ja to "おすすめのラジオ"
            )
            addString(
                en to "FOR YOU",
                ja to "あなたへのおすすめ"
            )
            addString(
                en to "Trending songs",
                ja to "急上昇曲"
            )
            addString(
                en to "Rock Artists",
                ja to "ロック アーティスト"
            )
            addString(
                en to "Hits by decade",
                ja to "Hits by decade",
                ja to "TODO"
            )
            addString(
                en to "JUST UPDATED",
                ja to "JUST UPDATED",
                ja to "TODO"
            )
            addString(
                en to "Today's hits",
                ja to "Today's hits",
                ja to "TODO"
            )
            addString(
                en to "Long listening",
                ja to "長編ミュージック ビデオ"
            )
            addString(
                en to "Celebrating Africa Month",
                ja to "Celebrating Africa Month",
                ja to "TODO"
            )
            addString(
                en to "Feel good",
                ja to "Feel good",
                ja to "TODO"
            )
            addString(
                en to "Fresh new music",
                ja to "Fresh new music",
                ja to "TODO"
            )
            addString(
                en to "#TBT",
                ja to "#TBT"
            )
        }
        OWN_CHANNEL_STRINGS = mutableListOf<Map<Int, Pair<String, String?>>>().apply {
            addString(
                en to "Songs on repeat",
                ja to "繰り返し再生されている曲"
            )
            addString(
                en to "Artists on repeat",
                ja to "繰り返し再生するアーティスト"
            )
            addString(
                en to "Videos on repeat",
                ja to "繰り返し再生されている動画"
            )
            addString(
                en to "Playlists on repeat",
                ja to "繰り返し再生するプレイリスト"
            )
        }
        ARTIST_PAGE_STRINGS = mutableListOf<Map<Int, Pair<String, String?>>>().apply {
            addString(
                en to "Songs",
                ja to "曲"
            )
            addString(
                en to "Albums",
                ja to "アルバム"
            )
            addString(
                en to "Videos",
                ja to "動画"
            )
            addString(
                en to "Singles",
                ja to "シングル"
            )
            addString(
                en to "Playlists",
                ja to "プレイリスト"
            )
            addString(
                en to "From your library",
                ja to "ライブラリから"
            )
            addString(
                en to "Fans might also like",
                ja to "おすすめのアーティスト"
            )
            addString(
                en to "Featured on",
                ja to "収録プレイリスト"
            )
        }
    }
}
