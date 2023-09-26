package com.toasterofbread.spmp.model

import SpMp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.google.gson.Gson
import com.toasterofbread.spmp.ProjectBuildConfig
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistRef
import com.toasterofbread.spmp.model.mediaitem.song.SongAudioQuality
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.platform.PlatformPreferences
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.PlayerOverlayMenuAction
import com.toasterofbread.spmp.youtubeapi.YoutubeApi
import com.toasterofbread.spmp.youtubeapi.formats.VideoFormatsEndpointType
import com.toasterofbread.spmp.youtubeapi.fromJson
import okhttp3.Headers.Companion.toHeaders
import java.util.*

enum class AccentColourSource {
    THEME, THUMBNAIL
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
enum class NowPlayingQueueWaveBorderMode {
    TIME, TIME_SYNC, SCROLL, NONE, LINE
}

enum class FontMode {
    DEFAULT, SYSTEM, HC_MARU_GOTHIC;

    fun getFontFilePath(language: String): String? =
        when (this) {
            DEFAULT -> getDefaultFont(language).getFontFilePath(language)
            SYSTEM -> null
            HC_MARU_GOTHIC -> "hc-maru-gothic/font.ttf"
        }

    fun getReadable(language: String): String =
        when (this) {
            DEFAULT -> {
                val default_font = getDefaultFont(language).getReadable(language)
                getString("font_option_default_\$x").replace("\$x", default_font)
            }
            SYSTEM -> getString("font_option_system")
            HC_MARU_GOTHIC -> getString("font_option_hc_maru_gothic")
        }

    companion object {
        fun getDefaultFont(language: String): FontMode =
            when (language) {
                "ja-JP" -> HC_MARU_GOTHIC
                else -> SYSTEM
            }
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
    KEY_NOWPLAYING_DEFAULT_GRADIENT_DEPTH,

    // Lyrics
    KEY_LYRICS_FOLLOW_ENABLED,
    KEY_LYRICS_FOLLOW_OFFSET,
    KEY_LYRICS_DEFAULT_FURIGANA,
    KEY_LYRICS_TEXT_ALIGNMENT,
    KEY_LYRICS_EXTRA_PADDING,
    KEY_LYRICS_ENABLE_WORD_SYNC,
    KEY_LYRICS_FONT_SIZE,
    KEY_LYRICS_DEFAULT_SOURCE,

    KEY_LYRICS_SHOW_IN_LIBRARY,
    KEY_LYRICS_SHOW_IN_RADIOBUILDER,
    KEY_LYRICS_SHOW_IN_SETTINGS,
    KEY_LYRICS_SHOW_IN_LOGIN,
    KEY_LYRICS_SHOW_IN_PLAYLIST,
    KEY_LYRICS_SHOW_IN_ARTIST,
    KEY_LYRICS_SHOW_IN_VIEWMORE,
    KEY_LYRICS_SHOW_IN_SEARCH,

    // Audio & Video
    KEY_VIDEO_FORMATS_METHOD,
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
    KEY_TOPBAR_DISPLAY_OVER_ARTIST_IMAGE,

    // Recommendation feed
    KEY_FEED_SHOW_FILTER_BAR,
    KEY_FEED_SHOW_SONG_DOWNLOAD_INDICATORS,
    KEY_FEED_INITIAL_ROWS,
    KEY_FEED_SQUARE_PREVIEW_TEXT_LINES,
    KEY_FEED_SHOW_RADIOS,
    KEY_FEED_HIDDEN_ROWS,

    // Player
    KEY_PLAYER_OVERLAY_CUSTOM_ACTION,
    KEY_PLAYER_OVERLAY_SWAP_LONG_SHORT_PRESS_ACTIONS,
    KEY_NP_QUEUE_RADIO_INFO_POSITION, // TODO prefs item
    KEY_NP_QUEUE_WAVE_BORDER_MODE,
    KEY_RESUME_ON_BT_CONNECT,
    KEY_PAUSE_ON_BT_DISCONNECT,
    KEY_RESUME_ON_WIRED_CONNECT,
    KEY_PAUSE_ON_WIRED_DISCONNECT,

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
    KEY_DISCORD_STATUS_DISABLE_WHEN_INVISIBLE,
    KEY_DISCORD_STATUS_DISABLE_WHEN_DND,
    KEY_DISCORD_STATUS_DISABLE_WHEN_IDLE,
    KEY_DISCORD_STATUS_DISABLE_WHEN_OFFLINE,
    KEY_DISCORD_STATUS_DISABLE_WHEN_ONLINE,

    // Caching
    KEY_THUMB_CACHE_ENABLED, // TODO Max size, management

    // Filter
    KEY_FILTER_ENABLE,
    KEY_FILTER_TITLE_KEYWORDS,
    KEY_FILTER_APPLY_TO_ARTISTS,
    KEY_FILTER_APPLY_TO_ARTIST_ITEMS,
    KEY_FILTER_APPLY_TO_PLAYLIST_ITEMS,

    // YoutubeApi
    KEY_YOUTUBEAPI_TYPE,
    KEY_YOUTUBEAPI_URL,

    // Other
    KEY_OPEN_NP_ON_SONG_PLAYED,
    KEY_MULTISELECT_CANCEL_ON_ACTION,
    KEY_MULTISELECT_CANCEL_WHEN_NONE_SELECTED, // TODO
    KEY_SHOW_LIKES_PLAYLIST,
    KEY_VOLUME_STEPS,
    KEY_PERSISTENT_QUEUE,
    KEY_ADD_SONGS_TO_HISTORY,
    KEY_TREAT_SINGLES_AS_SONG,
    KEY_FONT,
    KEY_STOP_PLAYER_ON_APP_CLOSE,
    KEY_LIBRARY_PATH,
    KEY_NAVBAR_HEIGHT_MULTIPLIER,

    // Internal
    INTERNAL_TOPBAR_MODE_HOME,
    INTERNAL_TOPBAR_MODE_NOWPLAYING,
    INTERNAL_DISCORD_WARNING_ACCEPTED

    ;

    fun <T> get(preferences: PlatformPreferences = prefs): T {
        return Settings.get(this, preferences)
    }

    fun <T> get(context: PlatformContext): T {
        return Settings.get(this, context.getPrefs())
    }

    inline fun <reified T: Enum<T>> getEnum(preferences: PlatformPreferences = prefs): T = Settings.getEnum(this, preferences)

    fun <T> set(value: T?, preferences: PlatformPreferences = prefs) {
        Settings.set(this, value, preferences)
    }

    @Composable
    fun <T> rememberMutableState(preferences: PlatformPreferences = prefs): MutableState<T> =
        mutableSettingsState(this, preferences)

    @Composable
    inline fun <reified T: Enum<T>> rememberMutableEnumState(preferences: PlatformPreferences = prefs): MutableState<T> =
        mutableSettingsEnumState(this, preferences)

    companion object {
        val prefs: PlatformPreferences get() = SpMp.context.getPrefs()

        fun <T> set(enum_key: Settings, value: T?, preferences: PlatformPreferences = prefs) {
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
        fun <T> get(enum_key: Settings, preferences: PlatformPreferences = prefs, default: T? = null): T {
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

        inline fun <reified T> getJsonArray(enum_key: Settings, gson: Gson = Gson(), preferences: PlatformPreferences = prefs, default: String? = null): List<T> {
            return gson.fromJson(get(enum_key, preferences, default))!!
        }

        inline fun <reified T: Enum<T>> getEnum(enum_key: Settings, preferences: PlatformPreferences = prefs, default: T? = null): T {
            val default_value: Int = default?.ordinal ?: getDefault(enum_key)
            return enumValues<T>()[preferences.getInt(enum_key.name, default_value)!!]
        }

        @Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
        fun <T> getDefault(enum_key: Settings): T {
            return when (enum_key) {
                KEY_LANG_UI, KEY_LANG_DATA -> ""

                KEY_LPM_CLOSE_ON_ACTION -> true
                KEY_LPM_INCREMENT_PLAY_AFTER -> true

                KEY_ACCENT_COLOUR_SOURCE -> AccentColourSource.THUMBNAIL.ordinal
                KEY_CURRENT_THEME -> 0
                KEY_THEMES -> "[]"
                KEY_NOWPLAYING_THEME_MODE -> 0
                KEY_NOWPLAYING_DEFAULT_GRADIENT_DEPTH -> 1f

                KEY_LYRICS_FOLLOW_ENABLED -> true
                KEY_LYRICS_FOLLOW_OFFSET -> 0.25f
                KEY_LYRICS_DEFAULT_FURIGANA -> true
                KEY_LYRICS_TEXT_ALIGNMENT -> 0 // Left, center, right
                KEY_LYRICS_EXTRA_PADDING -> true
                KEY_LYRICS_ENABLE_WORD_SYNC -> false
                KEY_LYRICS_FONT_SIZE -> 0.5f
                KEY_LYRICS_DEFAULT_SOURCE -> 0

                KEY_LYRICS_SHOW_IN_LIBRARY -> true
                KEY_LYRICS_SHOW_IN_RADIOBUILDER -> true
                KEY_LYRICS_SHOW_IN_SETTINGS -> true
                KEY_LYRICS_SHOW_IN_LOGIN -> true
                KEY_LYRICS_SHOW_IN_PLAYLIST -> true
                KEY_LYRICS_SHOW_IN_ARTIST -> true
                KEY_LYRICS_SHOW_IN_VIEWMORE -> true
                KEY_LYRICS_SHOW_IN_SEARCH -> true

                KEY_VIDEO_FORMATS_METHOD -> VideoFormatsEndpointType.DEFAULT.ordinal
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
                KEY_TOPBAR_VISUALISER_WIDTH -> 0.9f
                KEY_TOPBAR_DISPLAY_OVER_ARTIST_IMAGE -> false

                // Recommendation feed
                KEY_FEED_SHOW_FILTER_BAR -> true
                KEY_FEED_SHOW_SONG_DOWNLOAD_INDICATORS -> false
                KEY_FEED_INITIAL_ROWS -> 4
                KEY_FEED_SQUARE_PREVIEW_TEXT_LINES -> 1
                KEY_FEED_SHOW_RADIOS -> false
                KEY_FEED_HIDDEN_ROWS -> emptySet<String>()

                KEY_PLAYER_OVERLAY_CUSTOM_ACTION -> PlayerOverlayMenuAction.DEFAULT_CUSTOM.ordinal
                KEY_PLAYER_OVERLAY_SWAP_LONG_SHORT_PRESS_ACTIONS -> false
                KEY_NP_QUEUE_RADIO_INFO_POSITION -> NowPlayingQueueRadioInfoPosition.TOP_BAR.ordinal
                KEY_NP_QUEUE_WAVE_BORDER_MODE -> NowPlayingQueueWaveBorderMode.TIME.ordinal
                KEY_RESUME_ON_BT_CONNECT -> true
                KEY_PAUSE_ON_BT_DISCONNECT -> true
                KEY_RESUME_ON_WIRED_CONNECT -> true
                KEY_PAUSE_ON_WIRED_DISCONNECT -> true

                KEY_YTM_AUTH -> {
                    with(ProjectBuildConfig) {
                        if (YTM_CHANNEL_ID != null && YTM_HEADERS != null)
                            YoutubeApi.UserAuthState.packSetData(
                                ArtistRef(YTM_CHANNEL_ID),
                                Gson().fromJson<Map<String, String>>(YTM_HEADERS.reader()).toHeaders()
                            )
                        else emptySet()
                    }
                }
                KEY_DISCORD_ACCOUNT_TOKEN -> ProjectBuildConfig.DISCORD_ACCOUNT_TOKEN ?: ""

                KEY_DISCORD_STATUS_NAME -> ProjectBuildConfig.DISCORD_STATUS_TEXT_NAME_OVERRIDE ?: getString("discord_status_default_name")
                KEY_DISCORD_STATUS_TEXT_A -> ProjectBuildConfig.DISCORD_STATUS_TEXT_TEXT_A_OVERRIDE ?: getString("discord_status_default_text_a")
                KEY_DISCORD_STATUS_TEXT_B -> ProjectBuildConfig.DISCORD_STATUS_TEXT_TEXT_B_OVERRIDE ?: getString("discord_status_default_text_b")
                KEY_DISCORD_STATUS_TEXT_C -> ProjectBuildConfig.DISCORD_STATUS_TEXT_TEXT_C_OVERRIDE ?: getString("discord_status_default_text_c")
                KEY_DISCORD_SHOW_BUTTON_SONG -> true
                KEY_DISCORD_BUTTON_SONG_TEXT -> ProjectBuildConfig.DISCORD_STATUS_TEXT_BUTTON_SONG_OVERRIDE ?: getString("discord_status_default_button_song")
                KEY_DISCORD_SHOW_BUTTON_PROJECT -> true
                KEY_DISCORD_BUTTON_PROJECT_TEXT -> ProjectBuildConfig.DISCORD_STATUS_TEXT_BUTTON_PROJECT_OVERRIDE ?: getString("discord_status_default_button_project")

                KEY_DISCORD_STATUS_DISABLE_WHEN_INVISIBLE -> false
                KEY_DISCORD_STATUS_DISABLE_WHEN_DND -> false
                KEY_DISCORD_STATUS_DISABLE_WHEN_IDLE -> false
                KEY_DISCORD_STATUS_DISABLE_WHEN_OFFLINE -> false
                KEY_DISCORD_STATUS_DISABLE_WHEN_ONLINE -> false

                // Caching
                KEY_THUMB_CACHE_ENABLED -> true

                // Filter
                KEY_FILTER_ENABLE -> true
                KEY_FILTER_TITLE_KEYWORDS -> emptySet<String>()
                KEY_FILTER_APPLY_TO_ARTISTS -> false
                KEY_FILTER_APPLY_TO_ARTIST_ITEMS -> false
                KEY_FILTER_APPLY_TO_PLAYLIST_ITEMS -> false

                KEY_YOUTUBEAPI_TYPE -> YoutubeApi.Type.DEFAULT.ordinal
                KEY_YOUTUBEAPI_URL -> YoutubeApi.Type.DEFAULT.getDefaultUrl()

                KEY_VOLUME_STEPS -> 50
                KEY_OPEN_NP_ON_SONG_PLAYED -> true
                KEY_MULTISELECT_CANCEL_ON_ACTION -> true
                KEY_MULTISELECT_CANCEL_WHEN_NONE_SELECTED -> true
                KEY_SHOW_LIKES_PLAYLIST -> true
                KEY_PERSISTENT_QUEUE -> true
                KEY_ADD_SONGS_TO_HISTORY -> false
                KEY_TREAT_SINGLES_AS_SONG -> false
                KEY_FONT -> FontMode.DEFAULT.ordinal
                KEY_STOP_PLAYER_ON_APP_CLOSE -> false
                KEY_LIBRARY_PATH -> ""
                KEY_NAVBAR_HEIGHT_MULTIPLIER -> 1f

                KEY_SPMS_PORT -> 3973

                // Internal
                INTERNAL_TOPBAR_MODE_HOME -> MusicTopBarMode.LYRICS.ordinal
                INTERNAL_TOPBAR_MODE_NOWPLAYING -> MusicTopBarMode.LYRICS.ordinal
                INTERNAL_DISCORD_WARNING_ACCEPTED -> false
            } as T
        }

        fun <T> provideDefault(key: String): T {
            return getDefault(values().first { it.name == key })
        }
    }
}
