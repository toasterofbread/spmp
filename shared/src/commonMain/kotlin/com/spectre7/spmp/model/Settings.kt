package com.spectre7.spmp.model

import SpMp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.beust.klaxon.Klaxon
import com.spectre7.spmp.ProjectBuildConfig
import com.spectre7.spmp.model.mediaitem.Artist
import com.spectre7.spmp.model.mediaitem.enums.SongAudioQuality
import com.spectre7.spmp.platform.PlatformContext
import com.spectre7.spmp.platform.ProjectPreferences
import com.spectre7.spmp.resources.getString
import java.util.*

enum class AccentColourSource {
    THEME, THUMBNAIL, SYSTEM
}
enum class NowPlayingQueueRadioInfoPosition {
    TOP_BAR, ABOVE_ITEMS
}
enum class MusicTopBarMode {
    VISUALISER, LYRICS;

    fun getIcon(): ImageVector = when (this) {
        LYRICS -> Icons.Default.Lyrics
        VISUALISER -> Icons.Default.GraphicEq
    }

    fun getNext(can_show_visualiser: Boolean): MusicTopBarMode {
        val next =
            if (ordinal == 0) values().last()
            else values()[ordinal - 1]

        if (!can_show_visualiser && next == VISUALISER) {
            return next.getNext(false)
        }

        return next
    }

    companion object {
        val default: MusicTopBarMode get() = LYRICS
    }
}

enum class Settings {
    // Language
    KEY_LANG_UI,
    KEY_LANG_DATA,

    // Long press menu
    KEY_LPM_CLOSE_ON_ACTION,
    KEY_LPM_INCREMENT_PLAY_AFTER,

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
    KEY_LYRICS_FONT_SIZE,
    
    // Audio & Video
    KEY_STREAM_AUDIO_QUALITY,
    KEY_DOWNLOAD_AUDIO_QUALITY,

    // Download
    KEY_AUTO_DOWNLOAD_ENABLED,
    KEY_AUTO_DOWNLOAD_THRESHOLD,
    KEY_AUTO_DOWNLOAD_ON_METERED,

    // Stats
    // KEY_STATS_ENABLED,
    // KEY_STATS_LISTEN_THRESHOLD,
    // KEY_STATS_LISTEN_THRESHOLD_TYPE,

    // Accessibility Service
//    KEY_ACC_VOL_INTERCEPT_MODE,
    KEY_ACC_VOL_INTERCEPT_NOTIFICATION,
    KEY_ACC_SCREEN_OFF,

    // Top bar content
    KEY_TOPBAR_LYRICS_LINGER,
    KEY_TOPBAR_LYRICS_SHOW_FURIGANA,
    KEY_TOPBAR_SHOW_LYRICS_IN_QUEUE,
    KEY_TOPBAR_SHOW_VISUALISER_IN_QUEUE,
    KEY_TOPBAR_VISUALISER_WIDTH,

    // Recommendation feed
    KEY_FEED_SHOW_FILTERS,
    KEY_FEED_INITIAL_ROWS,
    KEY_FEED_SHOW_RADIOS,
    KEY_FEED_SHOW_LISTEN_ROW,
    KEY_FEED_SHOW_MIX_ROW,
    KEY_FEED_SHOW_NEW_ROW,
    KEY_FEED_SHOW_MOODS_ROW,
    KEY_FEED_SHOW_CHARTS_ROW,

    // Now playing queue
    KEY_NP_QUEUE_RADIO_INFO_POSITION, // TODO prefs item

    // Server
    KEY_SPMS_PORT,

    // Auth
    KEY_YTM_AUTH,
    KEY_DISCORD_ACCOUNT_TOKEN,

    // Discord status
    KEY_DISCORD_STATUS_NAME,
    KEY_DISCORD_STATUS_TEXT_A,
    KEY_DISCORD_STATUS_TEXT_B,
    KEY_DISCORD_STATUS_TEXT_C,
    KEY_DISCORD_SHOW_BUTTON_SONG,
    KEY_DISCORD_BUTTON_SONG_TEXT,
    KEY_DISCORD_SHOW_BUTTON_PROJECT,
    KEY_DISCORD_BUTTON_PROJECT_TEXT,

    // Caching
    KEY_THUMB_CACHE_ENABLED, // TODO Max size, management

    // Other
    KEY_OPEN_NP_ON_SONG_PLAYED,
    KEY_MULTISELECT_CANCEL_ON_ACTION, // TODO
    KEY_MULTISELECT_CANCEL_WHEN_NONE_SELECTED, // TODO
    KEY_SHOW_LIKES_PLAYLIST, // TODO
    KEY_VOLUME_STEPS,
    KEY_PERSISTENT_QUEUE,
    KEY_ADD_SONGS_TO_HISTORY,

    // Internal
    INTERNAL_PINNED_ITEMS,
    INTERNAL_TOPBAR_MODE_HOME,
    INTERNAL_TOPBAR_MODE_NOWPLAYING,
    INTERNAL_TOPBAR_MODE_LIBRARY,
    INTERNAL_TOPBAR_MODE_RADIOBUILDER,
    INTERNAL_TOPBAR_MODE_SETTINGS,
    INTERNAL_TOPBAR_MODE_LOGIN,
    INTERNAL_TOPBAR_MODE_PLAYLIST,
    INTERNAL_TOPBAR_MODE_ARTIST,
    INTERNAL_TOPBAR_MODE_VIEWMORE,
    INTERNAL_TOPBAR_MODE_SEARCH
    
    ;

    fun <T> get(preferences: ProjectPreferences = prefs): T {
        return Settings.get(this, preferences)
    }

    fun <T> get(context: PlatformContext): T {
        return Settings.get(this, context.getPrefs())
    }

    inline fun <reified T: Enum<T>> getEnum(): T = Settings.getEnum(this)

    fun <T> set(value: T?, preferences: ProjectPreferences = prefs) {
        Settings.set(this, value, preferences)
    }

    @Composable
    fun <T> rememberMutableState(preferences: ProjectPreferences = prefs): MutableState<T> =
        mutableSettingsState(this, preferences)

    @Composable
    inline fun <reified T: Enum<T>> rememberMutableEnumState(preferences: ProjectPreferences = prefs): MutableState<T> =
        mutableSettingsEnumState(this, preferences)

    companion object {
        val prefs: ProjectPreferences get() = SpMp.context.getPrefs()

        fun <T> set(enum_key: Settings, value: T?, preferences: ProjectPreferences = prefs) {
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
        fun <T> get(enum_key: Settings, preferences: ProjectPreferences = prefs, default: T? = null): T {
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

        inline fun <reified T> getJsonArray(enum_key: Settings, klaxon: Klaxon = Klaxon(), preferences: ProjectPreferences = prefs, default: String? = null): List<T> {
            return klaxon.parseArray(get(enum_key, preferences, default))!!
        }

        inline fun <reified T: Enum<T>> getEnum(enum_key: Settings, preferences: ProjectPreferences = prefs, default: T? = null): T {
            val default_value: Int = default?.ordinal ?: getDefault(enum_key)
            return enumValues<T>()[preferences.getInt(enum_key.name, default_value)!!]
        }

        @Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
        fun <T> getDefault(enum_key: Settings): T {
            return when (enum_key) {
                KEY_LANG_UI, KEY_LANG_DATA -> SpMp.getLanguageIndex(Locale.getDefault().language)

                KEY_LPM_CLOSE_ON_ACTION -> true
                KEY_LPM_INCREMENT_PLAY_AFTER -> true

                KEY_ACCENT_COLOUR_SOURCE -> AccentColourSource.THUMBNAIL.ordinal
                KEY_CURRENT_THEME -> 0
                KEY_THEMES -> "[]"
                KEY_NOWPLAYING_THEME_MODE -> 0

                KEY_LYRICS_FOLLOW_ENABLED -> true
                KEY_LYRICS_FOLLOW_OFFSET -> 0.25f
                KEY_LYRICS_DEFAULT_FURIGANA -> true
                KEY_LYRICS_TEXT_ALIGNMENT -> 0 // Left, center, right
                KEY_LYRICS_EXTRA_PADDING -> true
                KEY_LYRICS_ENABLE_WORD_SYNC -> false
                KEY_LYRICS_FONT_SIZE -> 0.5f

                KEY_STREAM_AUDIO_QUALITY -> SongAudioQuality.MEDIUM.ordinal
                KEY_DOWNLOAD_AUDIO_QUALITY -> SongAudioQuality.MEDIUM.ordinal

                KEY_AUTO_DOWNLOAD_ENABLED -> true
                KEY_AUTO_DOWNLOAD_THRESHOLD -> 1 // Listens
                KEY_AUTO_DOWNLOAD_ON_METERED -> false

//                KEY_ACC_VOL_INTERCEPT_MODE -> PlayerAccessibilityService.PlayerAccessibilityServiceVolumeInterceptMode.NEVER.ordinal
                KEY_ACC_VOL_INTERCEPT_NOTIFICATION -> false
                KEY_ACC_SCREEN_OFF -> false

                // Top bar content
                KEY_TOPBAR_LYRICS_LINGER -> true
                KEY_TOPBAR_LYRICS_SHOW_FURIGANA -> true
                KEY_TOPBAR_SHOW_LYRICS_IN_QUEUE -> true
                KEY_TOPBAR_SHOW_VISUALISER_IN_QUEUE -> false
                KEY_TOPBAR_VISUALISER_WIDTH -> 0.8f

                // Recommendation feed
                KEY_FEED_SHOW_FILTERS -> true
                KEY_FEED_INITIAL_ROWS -> 5
                KEY_FEED_SHOW_RADIOS -> false
                KEY_FEED_SHOW_LISTEN_ROW -> true
                KEY_FEED_SHOW_MIX_ROW -> true
                KEY_FEED_SHOW_NEW_ROW -> true
                KEY_FEED_SHOW_MOODS_ROW -> true
                KEY_FEED_SHOW_CHARTS_ROW -> true

                KEY_NP_QUEUE_RADIO_INFO_POSITION -> NowPlayingQueueRadioInfoPosition.TOP_BAR.ordinal

                KEY_YTM_AUTH -> {
                    with(ProjectBuildConfig) {
                        if (IS_DEBUG)
                            YoutubeMusicAuthInfo(
                                Artist.fromId(YTM_CHANNEL_ID!!),
                                YTM_COOKIE!!,
                                Klaxon().parse(YTM_HEADERS!!.reader())!!
                            )
                        else emptySet()
                    }
                }
                KEY_DISCORD_ACCOUNT_TOKEN -> ProjectBuildConfig.DISCORD_ACCOUNT_TOKEN ?: ""

                KEY_DISCORD_STATUS_NAME -> getString("discord_status_default_name")
                KEY_DISCORD_STATUS_TEXT_A -> getString("discord_status_default_text_a")
                KEY_DISCORD_STATUS_TEXT_B -> getString("discord_status_default_text_b")
                KEY_DISCORD_STATUS_TEXT_C -> getString("discord_status_default_text_c")
                KEY_DISCORD_SHOW_BUTTON_SONG -> true
                KEY_DISCORD_BUTTON_SONG_TEXT -> getString("discord_status_default_button_song")
                KEY_DISCORD_SHOW_BUTTON_PROJECT -> true
                KEY_DISCORD_BUTTON_PROJECT_TEXT -> getString("discord_status_default_button_project")

                // Caching
                KEY_THUMB_CACHE_ENABLED -> true

                KEY_VOLUME_STEPS -> 50
                KEY_OPEN_NP_ON_SONG_PLAYED -> true
                KEY_MULTISELECT_CANCEL_ON_ACTION -> true
                KEY_MULTISELECT_CANCEL_WHEN_NONE_SELECTED -> true
                KEY_SHOW_LIKES_PLAYLIST -> true
                KEY_PERSISTENT_QUEUE -> true
                KEY_ADD_SONGS_TO_HISTORY -> false

                KEY_SPMS_PORT -> 3973

                // Internal
                INTERNAL_PINNED_ITEMS -> emptySet<String>()
                INTERNAL_TOPBAR_MODE_HOME -> MusicTopBarMode.LYRICS.ordinal
                INTERNAL_TOPBAR_MODE_NOWPLAYING -> MusicTopBarMode.LYRICS.ordinal
                INTERNAL_TOPBAR_MODE_LIBRARY -> MusicTopBarMode.LYRICS.ordinal
                INTERNAL_TOPBAR_MODE_RADIOBUILDER -> MusicTopBarMode.LYRICS.ordinal
                INTERNAL_TOPBAR_MODE_SETTINGS -> MusicTopBarMode.LYRICS.ordinal
                INTERNAL_TOPBAR_MODE_LOGIN -> MusicTopBarMode.LYRICS.ordinal
                INTERNAL_TOPBAR_MODE_PLAYLIST -> MusicTopBarMode.LYRICS.ordinal
                INTERNAL_TOPBAR_MODE_ARTIST -> MusicTopBarMode.LYRICS.ordinal
                INTERNAL_TOPBAR_MODE_VIEWMORE -> MusicTopBarMode.LYRICS.ordinal
                INTERNAL_TOPBAR_MODE_SEARCH -> MusicTopBarMode.LYRICS.ordinal

            } as T
        }

        fun <T> provideDefault(key: String): T {
            return getDefault(values().first { it.name == key })
        }
    }
}
