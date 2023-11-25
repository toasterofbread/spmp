package com.toasterofbread.spmp.model.settings

import SpMp
import androidx.compose.runtime.*
import com.google.gson.Gson
import com.toasterofbread.composekit.platform.PlatformPreferences
import com.toasterofbread.spmp.model.settings.category.SettingsCategory
import com.toasterofbread.spmp.model.settings.category.YTApiSettings
import com.toasterofbread.spmp.youtubeapi.fromJson
import java.util.*

object Settings {
    val prefs: PlatformPreferences get() = SpMp.prefs

    fun <T> set(enum_key: SettingsKey, value: T?, preferences: PlatformPreferences = prefs) {
        preferences.edit {
            @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
            when (value) {
                null -> remove(enum_key.getName())
                is Boolean -> putBoolean(enum_key.getName(), value)
                is Float -> putFloat(enum_key.getName(), value)
                is Int -> putInt(enum_key.getName(), value)
                is Long -> putLong(enum_key.getName(), value)
                is String -> putString(enum_key.getName(), value)
                is Set<*> -> putStringSet(enum_key.getName(), value as Set<String>)
                is Enum<*> -> putInt(enum_key.getName(), value.ordinal)
                else -> throw NotImplementedError("$enum_key ${value!!::class.simpleName}")
            }
        }
    }

    @Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
    fun <T> get(enum_key: SettingsKey, preferences: PlatformPreferences = prefs, default: T? = null): T {
        val default_value: T = default ?: enum_key.getDefaultValue()
        return when (default_value) {
            is Boolean -> preferences.getBoolean(enum_key.getName(), default_value as Boolean)
            is Float -> preferences.getFloat(enum_key.getName(), default_value as Float)
            is Int -> preferences.getInt(enum_key.getName(), default_value as Int)
            is Long -> preferences.getLong(enum_key.getName(), default_value as Long)
            is String -> preferences.getString(enum_key.getName(), default_value as String)
            is Set<*> -> preferences.getStringSet(enum_key.getName(), default_value as Set<String>)
            else -> throw NotImplementedError("$enum_key $default_value ${default_value!!::class.simpleName}")
        } as T
    }

    inline fun <reified T> getJsonArray(enum_key: SettingsKey, gson: Gson = Gson(), preferences: PlatformPreferences = prefs, default: String? = null): List<T> {
        return gson.fromJson(get(enum_key, preferences, default))!!
    }

    inline fun <reified T: Enum<T>> getEnum(enum_key: SettingsKey, preferences: PlatformPreferences = prefs, default: T? = null): T {
        val default_value: Int = default?.ordinal ?: enum_key.getDefaultValue()
        return enumValues<T>()[preferences.getInt(enum_key.getName(), default_value)!!]
    }

    fun <T> provideDefault(name: String): T {
        for (category in SettingsCategory.all) {
            val key = category.getKeyOfName(name)
            if (key != null) {
                return key.getDefaultValue()
            }
        }
        throw NotImplementedError(name)
    }
}
