package com.spectre7.spmp.api

import SpMp
import com.spectre7.spmp.model.mediaitem.MediaItemType
import com.spectre7.spmp.model.Settings
import com.spectre7.spmp.resources.getString
import org.apache.commons.lang3.time.DurationFormatUtils
import java.time.Duration

private const val HOUR: Long = 3600000L

fun durationToString(duration: Long, hl: String, short: Boolean): String {
    if (short) {
        return DurationFormatUtils.formatDuration(
            duration,
            if (duration >= HOUR) "H:mm:ss" else "mm:ss",
            true
        )
    }
    else {
        val hms = getHMS(hl)
        if (hms != null) {
            val f = StringBuilder()

            val dur = Duration.ofMillis(duration)
            dur.toHours().also {
                if (it != 0L) {
                    f.append("$it${hms.splitter}${hms.hours}")
                }
            }
            dur.toMinutesPart().also {
                if (it != 0) {
                    f.append("${hms.splitter}$it${hms.splitter}${hms.minutes}")
                }
            }
            dur.toSecondsPart().also {
                if (it != 0) {
                    f.append("${hms.splitter}$it${hms.splitter}${hms.seconds}")
                }
            }

            return f.toString()
        }
    }

    throw NotImplementedError(hl.split('-', limit = 2).first())
}

fun parseYoutubeDurationString(string: String, hl: String): Long? {
    if (string.contains(':')) {
        val parts = string.split(':')

        if (parts.size !in 2..3) {
            return null
        }

        val seconds = parts.last().toLong()
        val minutes = parts[parts.size - 2].toLong()
        val hours = if (parts.size == 3) parts.first().toLong() else 0L

        return ((hours * 60 + minutes) * 60 + seconds) * 1000
    }
    else {
        val hms = getHMS(hl)
        if (hms != null) {
            return parseHhMmSsDurationString(string, hms)
        }
    }

    throw NotImplementedError(hl.split('-', limit = 2).first())
}

fun parseYoutubeSubscribersString(string: String, hl: String): Int? {
    val suffixes = getAmountSuffixes(hl)
    if (suffixes != null) {
        if (string.last().isDigit()) {
            return string.toFloat().toInt()
        }

        val multiplier = suffixes[string.last()] ?: throw NotImplementedError(string.last().toString())
        return (string.substring(0, string.length - 1).toFloat() * multiplier).toInt()
    }

    throw NotImplementedError(hl)
}

fun amountToString(amount: Int, hl: String): String {
    val suffixes = getAmountSuffixes(hl)
    if (suffixes != null) {
        for (suffix in suffixes) {
            if (amount >= suffix.value) {
                return "${amount / suffix.value}${suffix.key}"
            }
        }

        return amount.toString()
    }

    throw NotImplementedError(hl)
}

private fun getAmountSuffixes(hl: String): Map<Char, Int>? =
    when (hl) {
        "en" -> mapOf(
            'B' to 1000000000,
            'M' to 1000000,
            'K' to 1000
        )
        "ja" -> mapOf(
            '億' to 100000000,
            '万' to 10000,
            '千' to 1000,
            '百' to 100
        )
        else -> null
    }

class LocalisedYoutubeString(
    val key: String,
    val type: Type,
    val source_language: Int? = null
) {
    private var string: String? = null

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

    fun getString(): String {
        if (string == null) {
            string = when (type) {
                Type.RAW -> key
                Type.APP -> getString(key)
                Type.HOME_FEED -> {
                    val translation = SpMp.yt_ui_translation.translateHomeFeedString(key, source_language!!)
                    if (translation != null) {
                        translation
                    } else {
                        println("WARNING: Using raw key '$key' as home feed string")
                        key
                    }
                }
                Type.OWN_CHANNEL -> SpMp.yt_ui_translation.translateOwnChannelString(key, source_language!!)
                Type.ARTIST_PAGE -> SpMp.yt_ui_translation.translateArtistPageString(key, source_language!!)
            } ?: throw NotImplementedError("Key: '$key', Type: $type, Source lang: ${SpMp.getLanguageCode(source_language!!)}")
        }
        return string!!
    }

    companion object {
        private val current_source_language: Int get() = Settings.KEY_LANG_DATA.get()

        fun temp(string: String): LocalisedYoutubeString = LocalisedYoutubeString(string, Type.RAW, current_source_language)

        fun raw(string: String): LocalisedYoutubeString = LocalisedYoutubeString(string, Type.RAW, current_source_language)
        fun app(key: String): LocalisedYoutubeString = LocalisedYoutubeString(key, Type.APP, current_source_language)
        fun homeFeed(key: String): LocalisedYoutubeString = LocalisedYoutubeString(key, Type.HOME_FEED, current_source_language)
        fun ownChannel(key: String): LocalisedYoutubeString = LocalisedYoutubeString(key, Type.OWN_CHANNEL, current_source_language)
        fun artistPage(key: String): LocalisedYoutubeString = LocalisedYoutubeString(key, Type.ARTIST_PAGE, current_source_language)

        fun filterChip(key: String): Int? = SpMp.yt_ui_translation.getFilterChipIndex(key, current_source_language)
        fun filterChip(index: Int): String = SpMp.yt_ui_translation.getFilterChip(index)

        fun mediaItemPage(key: String, item_type: MediaItemType): LocalisedYoutubeString =
            when (item_type) {
                MediaItemType.ARTIST -> artistPage(key)
                else -> throw NotImplementedError(item_type.name)
            }
    }
}

class YoutubeUITranslation(languages: List<String>) {
    private val HOME_FEED_STRINGS: List<Map<Int, Pair<String, String?>>>
    private val OWN_CHANNEL_STRINGS: List<Map<Int, Pair<String, String?>>>
    private val ARTIST_PAGE_STRINGS: List<Map<Int, Pair<String, String?>>>
    private val FILTER_CHIPS: List<Map<Int, Pair<String, String?>>>

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

    fun translateHomeFeedString(string: String, source_language: Int): String? = getTranslated(string, HOME_FEED_STRINGS, source_language)
    fun translateOwnChannelString(string: String, source_language: Int): String? = getTranslated(string, OWN_CHANNEL_STRINGS, source_language)
    fun translateArtistPageString(string: String, source_language: Int): String? = getTranslated(string, ARTIST_PAGE_STRINGS, source_language)

    fun getFilterChipIndex(string: String, source_language: Int): Int? {
        val index = FILTER_CHIPS.indexOfFirst { it[source_language]?.first == string }
        if (index == -1) {
            return null
        }
        return index
    }
    fun getFilterChip(index: Int): String {
        val chip = FILTER_CHIPS.elementAt(index)[Settings.KEY_LANG_UI.get()]!!
        return chip.second ?: chip.first
    }

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
            addString(
                en to "From your library",
                ja to "ライブラリから"
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
        FILTER_CHIPS = mutableListOf<Map<Int, Pair<String, String?>>>().apply {
            addString(
                en to "Relax",
                ja to "リラックス"
            )
            addString(
                en to "Energize",
                ja to "エナジー"
            )
            addString(
                en to "Workout",
                ja to "ワークアウト"
            )
            addString(
                en to "Commute",
                ja to "通勤・通学"
            )
            addString(
                en to "Focus",
                ja to "フォーカス"
            )
        }
    }
}

private data class HMSData(val hours: String, val minutes: String, val seconds: String, val splitter: String = "")

private fun getHMS(hl: String): HMSData? =
    when (hl.split('-', limit = 2).first()) {
        "en" -> HMSData("hours", "minutes", "seconds", " ")
        "ja" -> HMSData("時間", "分", "秒")
        else -> null
    }

private fun parseHhMmSsDurationString(string: String, hms: HMSData): Long? {
    val parts = string.split(' ')

    val h = parts.indexOf(hms.hours)
    val hours =
        if (h != -1) parts[h - 1].toLong()
        else null

    val m = parts.indexOf(hms.minutes)
    val minutes =
        if (m != -1) parts[m - 1].toLong()
        else null

    val s = parts.indexOf(hms.seconds)
    val seconds =
        if (s != -1) parts[s - 1].toLong()
        else null

    if (hours == null && minutes == null && seconds == null) {
        return null
    }

    return (((hours ?: 0) * 60 + (minutes ?: 0)) * 60 + (seconds ?: 0)) * 1000
}
