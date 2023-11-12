package com.toasterofbread.spmp.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.toasterofbread.composekit.platform.PlatformPreferences

@Composable
fun <T> mutableSettingsState(settings_key: Settings, prefs: PlatformPreferences = Settings.prefs): MutableState<T> {
    val state: MutableState<T> = remember { mutableStateOf(settings_key.get(prefs)) }
    var set_to: T by remember { mutableStateOf(state.value) }

    LaunchedEffect(state.value) {
        if (state.value != set_to) {
            set_to = state.value
            settings_key.set(set_to, prefs)
        }
    }

    DisposableEffect(settings_key) {
        val listener = prefs.addListener(object : PlatformPreferences.Listener {
            override fun onChanged(prefs: PlatformPreferences, key: String) {
                if (key == settings_key.name) {
                    set_to = settings_key.get(prefs)
                    state.value = set_to
                }
            }
        })

        onDispose {
            prefs.removeListener(listener)
        }
    }

    return state
}

@Composable
inline fun <reified T: Enum<T>> mutableSettingsEnumState(settings_key: Settings, prefs: PlatformPreferences = Settings.prefs): MutableState<T> {
    val state: MutableState<T> = remember { mutableStateOf(
        enumValues<T>()[settings_key.get(prefs)]
    ) }
    var set_to: T by remember { mutableStateOf(state.value) }

    LaunchedEffect(state.value) {
        if (state.value != set_to) {
            set_to = state.value
            settings_key.set(set_to.ordinal, prefs)
        }
    }

    DisposableEffect(settings_key) {
        val listener = prefs.addListener(object : PlatformPreferences.Listener {
            override fun onChanged(prefs: PlatformPreferences, key: String) {
                if (key == settings_key.name) {
                    set_to = enumValues<T>()[settings_key.get(prefs)]
                    state.value = set_to
                }
            }
        })

        onDispose {
            prefs.removeListener(listener)
        }
    }

    return state
}
