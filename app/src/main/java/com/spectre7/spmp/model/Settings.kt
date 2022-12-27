package com.spectre7.spmp.model

import android.content.SharedPreferences
import com.spectre7.spmp.MainActivity
import java.util.Locale

enum class Settings {
    KEY_LANG_UI,
    KEY_LANG_DATA,

    KEY_ACCENT_COLOUR_SOURCE,
    KEY_NOWPLAYING_THEME_MODE,

    KEY_LYRICS_FOLLOW_ENABLED,
    KEY_LYRICS_FOLLOW_OFFSET,
    KEY_LYRICS_DEFAULT_FURIGANA,
    KEY_LYRICS_TEXT_ALIGNMENT;

    companion object {
        val prefs: SharedPreferences get() = MainActivity.prefs

        fun <T> get(enum_key: Settings, default: T? = null): T {
            val default_value: T = default ?: getDefault(enum_key.name)
            return when (default_value!!::class) {
                Boolean::class -> prefs.getBoolean(enum_key.name, default_value as Boolean)
                Float::class -> prefs.getFloat(enum_key.name, default_value as Float)
                Int::class -> prefs.getInt(enum_key.name, default_value as Int)
                Long::class -> prefs.getLong(enum_key.name, default_value as Long)
                String::class -> prefs.getString(enum_key.name, default_value as String)
                else -> throw java.lang.ClassCastException()
            } as T
        }

        fun <T> getDefault(key: String): T {
            return when (key) {
                KEY_LANG_UI.name, KEY_LANG_DATA.name -> MainActivity.languages.keys.indexOf(Locale.getDefault().language)
                KEY_ACCENT_COLOUR_SOURCE.name -> 0
                KEY_NOWPLAYING_THEME_MODE.name -> 0
                KEY_LYRICS_FOLLOW_ENABLED.name -> true
                KEY_LYRICS_FOLLOW_OFFSET.name -> 0.5f
                KEY_LYRICS_DEFAULT_FURIGANA.name -> true
                KEY_LYRICS_TEXT_ALIGNMENT.name -> 0
                else -> TODO()
            } as T
        }

        fun <T> getDefaultProvider(): (String) -> T {
            return { key: String ->
                getDefault(key)
            }
        }
    }
}