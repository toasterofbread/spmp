package com.toasterofbread.spmp.ui.shortcut

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import com.toasterofbread.composekit.utils.common.addUnique
import com.toasterofbread.spmp.ui.shortcut.trigger.KeyboardShortcutTrigger

val LocalShortcutState: ProvidableCompositionLocal<ShortcutState> = compositionLocalOf { ShortcutState() }

typealias KeyDetectionState = (Key) -> Unit

class ShortcutState {
    private val pressed_modifiers: MutableList<KeyboardShortcutTrigger.KeyboardModifier> = mutableStateListOf()
    private var key_detection_state: KeyDetectionState? = null
    internal var user_shortcuts: List<Shortcut> = emptyList()

    @Composable
    fun ObserveState() {
        user_shortcuts = ObserveAllShortcuts()
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
