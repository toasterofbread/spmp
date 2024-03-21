package com.toasterofbread.spmp.model.appaction.shortcut

import androidx.compose.runtime.*
import androidx.compose.ui.input.key.Key
import com.toasterofbread.composekit.utils.common.addUnique
import com.toasterofbread.spmp.ui.component.shortcut.trigger.KeyboardShortcutTrigger
import com.toasterofbread.spmp.model.settings.category.ShortcutSettings
import kotlinx.serialization.json.Json

val LocalShortcutState: ProvidableCompositionLocal<ShortcutState> = compositionLocalOf { ShortcutState() }

typealias KeyDetectionState = (Key) -> Unit

class ShortcutState {
    private val pressed_modifiers: MutableList<KeyboardShortcutTrigger.KeyboardModifier> = mutableStateListOf()
    private var key_detection_state: KeyDetectionState? = null

    var all_shortcuts: List<Shortcut> = emptyList()
        private set

    @Composable
    fun ObserveState() {
        val shortcuts_data: String by ShortcutSettings.Key.CONFIGURED_SHORTCUTS.rememberMutableState()
        all_shortcuts = remember(shortcuts_data) { Json.decodeFromString(shortcuts_data) }
    }

    fun onModifierDown(modifier: KeyboardShortcutTrigger.KeyboardModifier) {
        pressed_modifiers.addUnique(modifier)
    }

    fun onModifierUp(modifier: KeyboardShortcutTrigger.KeyboardModifier) {
        pressed_modifiers.remove(modifier)
    }

    fun onKeyPress(key: Key): Boolean {
        key_detection_state?.also {
            it.invoke(key)
            return true
        }
        return false
    }

    fun setKeyDetectionState(state: KeyDetectionState?) {
        key_detection_state = state
    }
}
