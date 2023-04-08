package com.spectre7.spmp.model

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.beust.klaxon.Klaxon
import com.spectre7.spmp.BuildConfig
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.PlayerAccessibilityService
import com.spectre7.spmp.api.YoutubeMusicAuthInfo
import java.util.*

enum class AccentColourSource {
    THEME, THUMBNAIL, SYSTEM
}
enum class NowPlayingQueueRadioInfoPosition {
    TOP_BAR, ABOVE_ITEMS
}

enum class Settings {
    // Language
    KEY_LANG_UI,
    KEY_LANG_DATA,

    // Theming
    KEY_CURRENT_THEME,
    KEY_THEMES,
    KEY_ACCENT_COLOUR_SOURCE,
    KEY_NOWPLAYING_THEME_MODE,

    // Lyrics
    KEY_LYRICS_FOLLOW_ENABLED,
    KEY_LYRICS_FOLLOW_OFFSET,
    KEY_LYRICS_DEFAULT_FURIGANA,
    KEY_LYRICS_TEXT_ALIGNMENT,
    KEY_LYRICS_EXTRA_PADDING,
    KEY_LYRICS_ENABLE_WORD_SYNC,
    
    // Audio & Video
    KEY_STREAM_AUDIO_QUALITY,
    KEY_DOWNLOAD_AUDIO_QUALITY,

    // Download
    KEY_AUTO_DOWNLOAD_THRESHOLD,
    KEY_AUTO_DOWNLOAD_SIZE_LIMIT,

    // Stats
    // KEY_STATS_ENABLED,
    // KEY_STATS_LISTEN_THRESHOLD,
    // KEY_STATS_LISTEN_THRESHOLD_TYPE,

    // Accessibility Service
    KEY_ACC_VOL_INTERCEPT_MODE,
    KEY_ACC_VOL_INTERCEPT_NOTIFICATION,
    KEY_ACC_SCREEN_OFF,

    // Home feed
    KEY_FEED_INITIAL_ROWS,
    KEY_FEED_SHOW_RADIOS,
    KEY_FEED_SHOW_LISTEN_ROW,
    KEY_FEED_SHOW_MIX_ROW,
    KEY_FEED_SHOW_NEW_ROW,
    KEY_FEED_SHOW_MOODS_ROW,
    KEY_FEED_SHOW_CHARTS_ROW,

    // Now playing queue
    KEY_NP_QUEUE_RADIO_INFO_POSITION,

    // Auth
    KEY_YTM_AUTH,

    // Other
    KEY_OPEN_NP_ON_SONG_PLAYED,
    KEY_VOLUME_STEPS,
    KEY_PERSISTENT_QUEUE,
    KEY_ADD_SONGS_TO_HISTORY,
    KEY_ENABLE_DISCORD_PRESENCE;

    companion object {
        val prefs: SharedPreferences get() = getPrefs()
        fun getPrefs(context: Context = MainActivity.context): SharedPreferences {
            return MainActivity.getSharedPreferences(context)
        }

        private var local_auth_keys_used = false

        fun <T> set(enum_key: Settings, value: T?, preferences: SharedPreferences = prefs) {
            preferences.edit {
                @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
                when (value) {
                    null -> remove(enum_key.name)
                    is Boolean -> putBoolean(enum_key.name, value)
                    is Float -> putFloat(enum_key.name, value)
                    is Int -> putInt(enum_key.name, value)
                    is Long -> putLong(enum_key.name, value)
                    is String -> putString(enum_key.name, value)
                    is Set<*> -> putStringSet(enum_key.name, value as Set<String>)
                    else -> throw NotImplementedError("$enum_key ${value!!::class.simpleName}")
                }
            }
        }

        @Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
        fun <T> get(enum_key: Settings, preferences: SharedPreferences = prefs, default: T? = null): T {
            val default_value: T = default ?: getDefault(enum_key)
            return when (default_value) {
                is Boolean -> preferences.getBoolean(enum_key.name, default_value as Boolean)
                is Float -> preferences.getFloat(enum_key.name, default_value as Float)
                is Int -> preferences.getInt(enum_key.name, default_value as Int)
                is Long -> preferences.getLong(enum_key.name, default_value as Long)
                is String -> preferences.getString(enum_key.name, default_value as String)
                is Set<*> -> preferences.getStringSet(enum_key.name, default_value as Set<String>)
                else -> throw NotImplementedError("$enum_key $default_value ${default_value!!::class.simpleName}")
            } as T
        }

        inline fun <reified T> getJsonArray(enum_key: Settings, klaxon: Klaxon = Klaxon(), preferences: SharedPreferences = prefs, default: String? = null): List<T> {
            return klaxon.parseArray(get(enum_key, preferences, default))!!
        }

        inline fun <reified T: Enum<T>> getEnum(enum_key: Settings, preferences: SharedPreferences = prefs, default: T? = null): T {
            val default_value: Int = default?.ordinal ?: getDefault(enum_key)
            return enumValues<T>()[preferences.getInt(enum_key.name, default_value)]
        }

        @Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
        fun <T> getDefault(enum_key: Settings): T {
            return when (enum_key) {
                KEY_LANG_UI, KEY_LANG_DATA -> MainActivity.languages.keys.indexOf(Locale.getDefault().language)

                KEY_ACCENT_COLOUR_SOURCE -> AccentColourSource.THUMBNAIL.ordinal
                KEY_CURRENT_THEME -> 0
                KEY_THEMES -> "[]"
                KEY_NOWPLAYING_THEME_MODE -> 0

                KEY_LYRICS_FOLLOW_ENABLED -> true
                KEY_LYRICS_FOLLOW_OFFSET -> 0.5f
                KEY_LYRICS_DEFAULT_FURIGANA -> true
                KEY_LYRICS_TEXT_ALIGNMENT -> 0 // Left, center, right
                KEY_LYRICS_EXTRA_PADDING -> false
                KEY_LYRICS_ENABLE_WORD_SYNC -> false

                KEY_STREAM_AUDIO_QUALITY -> Song.AudioQuality.MEDIUM.ordinal
                KEY_DOWNLOAD_AUDIO_QUALITY -> Song.AudioQuality.MEDIUM.ordinal

                // KEY_STATS_ENABLED -> true
                // KEY_STATS_LISTEN_THRESHOLD -> 1f // Minutes or percentage
                // KEY_STATS_LISTEN_THRESHOLD_TYPE -> 0 // Absolute, percentage

                KEY_AUTO_DOWNLOAD_THRESHOLD -> 3 // Listens
                KEY_AUTO_DOWNLOAD_SIZE_LIMIT -> 1000000000 // Bytes

                KEY_ACC_VOL_INTERCEPT_MODE -> PlayerAccessibilityService.VOLUME_INTERCEPT_MODE.NEVER.ordinal
                KEY_ACC_VOL_INTERCEPT_NOTIFICATION -> false
                KEY_ACC_SCREEN_OFF -> false

                KEY_FEED_INITIAL_ROWS -> 5
                KEY_FEED_SHOW_RADIOS -> false
                KEY_FEED_SHOW_LISTEN_ROW -> true
                KEY_FEED_SHOW_MIX_ROW -> true
                KEY_FEED_SHOW_NEW_ROW -> true
                KEY_FEED_SHOW_MOODS_ROW -> true
                KEY_FEED_SHOW_CHARTS_ROW -> true

                KEY_NP_QUEUE_RADIO_INFO_POSITION -> NowPlayingQueueRadioInfoPosition.TOP_BAR.ordinal

                KEY_YTM_AUTH -> {
                    if (BuildConfig.LocalKeys != null && !local_auth_keys_used) {
                        local_auth_keys_used = true
                        YoutubeMusicAuthInfo(
                            Artist.fromId(BuildConfig.LocalKeys["YTM_CHANNEL_ID"]!!),
                            BuildConfig.LocalKeys["YTM_COOKIE"]!!,
                            Klaxon().parse(BuildConfig.LocalKeys["YTM_HEADERS"]!!.reader())!!
                        )
                    }
                    else {
                        emptySet()
                    }
                }

                KEY_VOLUME_STEPS -> 50
                KEY_OPEN_NP_ON_SONG_PLAYED -> true
                KEY_PERSISTENT_QUEUE -> true
                KEY_ADD_SONGS_TO_HISTORY -> false
                KEY_ENABLE_DISCORD_PRESENCE -> false
            } as T
        }

        fun <T> provideDefault(key: String): T {
            return getDefault(values().first { it.name == key })
        }
    }
}