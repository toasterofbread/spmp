package com.spectre7.composesettings.model

import androidx.compose.runtime.*
import com.spectre7.utils.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.*
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.*
import androidx.compose.ui.*
import androidx.compose.foundation.*
import android.util.Log

abstract class SettingsItem {
    @Composable
    abstract fun GetItem(theme: Theme)
}

class SettingsGroup(var title: String?): SettingsItem() {
    @Composable
    override fun GetItem(theme: Theme) {
        val colour = offsetColourRGB(theme.getAccent(), if (theme.getBackground(false).isDark()) 0.5 else -0.5)
        if (title != null) {
            Text(title!!.uppercase(), color = colour, fontSize = 15.sp)
        }
    }
}

class SettingsValueState<T>(initial_value: T, val key: String) {
    private var _value: T by mutableStateOf(getInitialValue(initial_value))
    internal var autosave: Boolean = true

    var value: T
        get() = _value
        set(new_value: T) {
            _value = new_value
            if (autosave) {
                save()
            }
        }

    private fun getInitialValue(default: T): T {
        if (MainActivity.prefs.contains(key)) {
            return when (T::class) {
                Boolean::class -> MainActivity.prefs.getBoolean(key)
                Float::class -> MainActivity.prefs.getFloat(key)
                Int::class -> MainActivity.prefs.getInt(key)
                Long::class -> MainActivity.prefs.getLong(key)
                String::class -> MainActivity.prefs.getString(key)
                else -> throw java.lang.ClassCastException()
            }
        }
        else {
            return default
        }
    }

    internal fun save() {
        with (MainActivity.prefs.edit()) {
            when (T::class) {
                Boolean::class -> putBoolean(key, _value)
                Float::class -> putFloat(key, _value)
                Int::class -> putInt(key, _value)
                Long::class -> putLong(key, _value)
                String::class -> putString(key, _value)
                else -> throw java.lang.ClassCastException()
            }
            apply()
        }
    }
}

class SettingsValueToggle(
    val state: SettingsValueState<Boolean>,
    val title: String?,
    val subtitle: String?
): SettingsItem() {

    @Composable
    override fun GetItem(theme: Theme) {
        Row() {
            Column(Modifier.fillMaxWidth().weight(1f)) {
                if (title != null) {
                    Text(title)
                }
                if (subtitle != null) {
                    Text(subtitle, color = theme.getOnBackground(false).setAlpha(0.75))
                }
            }
            Switch(checked = state.value, onCheckedChange = {state.value = it})
        }
    }
}

class SettingsValueSlider(
    val state: SettingsValueState<Float>,
    val value_label: String = state.value.toString(),
    val title: String?,
    val subtitle: String?
): SettingsItem() {

    @Composable
    override fun GetItem(theme: Theme) {
        Row() {
            Column(Modifier.fillMaxWidth().weight(1f)) {
                if (title != null) {
                    Text(title)
                }
                if (subtitle != null) {
                    Text(subtitle, color = theme.getOnBackground(false).setAlpha(0.75))
                }
            }

            state.autosave = false
            Row(Modifier.fillMaxWidth()) {
                SliderValueHorizontal(
                    value = state.value,
                    onValueChange = {
                        state.value
                    },
                    onValueChangeFinished {
                        state.save()
                    },
                    thumbSizeInDp = DpSize(12.dp, 12.dp),
                    track = { a, b, c, d, e -> DefaultTrack(a, b, c, d, e, theme.getOnBackground(true).setAlpha(0.5), theme.getOnBackground(true).setAlpha(0.75), highlight = highlight) },
                    thumb = { a, b, c, d, e -> DefaultThumb(a, b, c, d, e, theme.getOnBackground(true), 1f) }
                )
                Text(value_label)
            }
        }
    }
}
