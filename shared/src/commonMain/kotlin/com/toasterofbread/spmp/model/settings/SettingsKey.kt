package com.toasterofbread.spmp.model.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import dev.toastbits.composekit.platform.PlatformPreferences
import com.toasterofbread.spmp.model.settings.category.SettingsCategory
import com.toasterofbread.spmp.platform.AppContext

interface SettingsKey {
    val category: SettingsCategory
    fun <T> getDefaultValue(): T

    fun getName(): String = category.getNameOfKey(this)
    fun isHidden(): Boolean = false

    fun <T> get(preferences: PlatformPreferences = Settings.prefs): T {
        return Settings.get(this, preferences)
    }

    fun <T> get(context: AppContext): T {
        return Settings.get(this, context.getPrefs())
    }

    fun <T> set(value: T?, preferences: PlatformPreferences = Settings.prefs) {
        Settings.set(this, value, preferences)
    }

    @Composable
    fun <T> rememberMutableState(): MutableState<T> = rememberMutableState(Settings.prefs)

    @Composable
    fun <T> rememberMutableState(preferences: PlatformPreferences): MutableState<T> =
        mutableSettingsState(this, preferences)
}

inline fun <reified T: Enum<T>> SettingsKey.getEnum(preferences: PlatformPreferences = Settings.prefs): T =
    Settings.getEnum(this, preferences)

@Composable
inline fun <reified T: Enum<T>> SettingsKey.rememberMutableEnumState(preferences: PlatformPreferences = Settings.prefs): MutableState<T> =
    mutableSettingsEnumState(this, preferences)
