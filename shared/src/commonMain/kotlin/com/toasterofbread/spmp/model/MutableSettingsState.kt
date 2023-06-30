package com.toasterofbread.spmp.model

import androidx.compose.runtime.*
import com.toasterofbread.spmp.platform.ProjectPreferences

@Composable
fun <T> mutableSettingsState(settings_key: Settings, prefs: ProjectPreferences = Settings.prefs): MutableState<T> {
    val state: MutableState<T> = remember { mutableStateOf(settings_key.get(prefs)) }
    var set_to: T by remember { mutableStateOf(state.value) }

    LaunchedEffect(state.value) {
        if (state.value != set_to) {
            set_to = state.value
            settings_key.set(set_to, prefs)
        }
    }

    DisposableEffect(settings_key) {
        val listener = prefs.addListener(object : ProjectPreferences.Listener {
            override fun onChanged(prefs: ProjectPreferences, key: String) {
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
inline fun <reified T: Enum<T>> mutableSettingsEnumState(settings_key: Settings, prefs: ProjectPreferences = Settings.prefs): MutableState<T> {
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
        val listener = prefs.addListener(object : ProjectPreferences.Listener {
            override fun onChanged(prefs: ProjectPreferences, key: String) {
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
