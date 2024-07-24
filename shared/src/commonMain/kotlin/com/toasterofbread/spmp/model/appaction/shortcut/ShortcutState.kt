package com.toasterofbread.spmp.model.appaction.shortcut

import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.input.key.*
import dev.toastbits.composekit.utils.common.addUnique
import com.toasterofbread.spmp.ui.component.shortcut.trigger.*
import com.toasterofbread.spmp.model.state.OldPlayerStateImpl
import com.toasterofbread.spmp.model.appaction.shortcut.getDefaultShortcuts
import kotlinx.serialization.json.Json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToLong
import LocalPlayerState

val LocalShortcutState: ProvidableCompositionLocal<ShortcutState> = compositionLocalOf { ShortcutState() }

typealias KeyDetectionState = (Key) -> Unit
typealias ButtonDetectionState = (Int) -> Unit

private val NUMBER_KEYS: List<Key?> = listOf(
    Key.Zero, Key.One, Key.Two, Key.Three, Key.Four, Key.Five, Key.Six, Key.Seven, Key.Eight, Key.Nine, null
)

class ShortcutState {
    private val pressed_modifiers: MutableList<KeyboardShortcutTrigger.KeyboardModifier> = mutableStateListOf()
    private var navigate_song_with_numbers: Boolean = false

    private var key_detection_state: KeyDetectionState? = null
    private var button_detection_state: ButtonDetectionState? = null

    private var keyboard_shortcuts: List<Shortcut> = emptyList()
    private var mouse_button_shortcuts: List<Shortcut> = emptyList()

    @Composable
    fun ObserveState() {
        val player: OldPlayerStateImpl = LocalPlayerState.current
        navigate_song_with_numbers = player.settings.shortcut.NAVIGATE_SONG_WITH_NUMBERS.observe().value

        val shortcuts: List<Shortcut>? by player.settings.shortcut.CONFIGURED_SHORTCUTS.observe()
        LaunchedEffect(shortcuts) {
            val keyboard_shortcuts: MutableList<Shortcut> = mutableListOf()
            val mouse_button_shortcuts: MutableList<Shortcut> = mutableListOf()

            for (shortcut in (shortcuts ?: getDefaultShortcuts())) {
                when (shortcut.trigger) {
                    null -> {}
                    is KeyboardShortcutTrigger -> keyboard_shortcuts.add(shortcut)
                    is MouseButtonShortcutTrigger -> mouse_button_shortcuts.add(shortcut)
                }
            }

            this@ShortcutState.keyboard_shortcuts = keyboard_shortcuts
            this@ShortcutState.mouse_button_shortcuts = mouse_button_shortcuts
        }
    }

    fun onModifierDown(modifier: KeyboardShortcutTrigger.KeyboardModifier) {
        pressed_modifiers.addUnique(modifier)
    }

    fun onModifierUp(modifier: KeyboardShortcutTrigger.KeyboardModifier) {
        pressed_modifiers.remove(modifier)
    }

    fun onKeyPress(
        event: KeyEvent,
        text_input_active: Boolean,
        player: OldPlayerStateImpl
    ): Boolean {
        key_detection_state?.also {
            it.invoke(event.key)
            return true
        }

        for (shortcut in keyboard_shortcuts) {
            if (text_input_active && !shortcut.action.isUsableDuringTextInput()) {
                continue
            }

            val trigger: KeyboardShortcutTrigger = shortcut.trigger as KeyboardShortcutTrigger
            if (trigger.isTriggeredBy(event)) {
                player.coroutine_scope.launch {
                    shortcut.action.executeAction(player)
                }
                return true
            }
        }

        if (navigate_song_with_numbers) {
            val number_index: Int = NUMBER_KEYS.indexOf(event.key)
            if (number_index != -1 && KeyboardShortcutTrigger.KeyboardModifier.entries.none { it.isPressedInEvent(event) }) {
                player.withPlayer {
                    val seek_target: Long = (duration_ms * (number_index.toFloat() / NUMBER_KEYS.size)).roundToLong()
                    seekTo(seek_target)
                }
                return true
            }
        }

        return false
    }

    fun onButtonPress(button_code: Int, player: OldPlayerStateImpl): Boolean {
        button_detection_state?.also {
            it.invoke(button_code)
            return true
        }

        for (shortcut in mouse_button_shortcuts) {
            val trigger: MouseButtonShortcutTrigger = shortcut.trigger as MouseButtonShortcutTrigger
            if (trigger.isTriggeredBy(button_code)) {
                player.coroutine_scope.launch {
                    shortcut.action.executeAction(player)
                }
                return true
            }
        }

        return false
    }

    fun setKeyDetectionState(state: KeyDetectionState?) {
        key_detection_state = state
    }

    fun setButtonDetectionState(state: ButtonDetectionState?) {
        button_detection_state = state
    }
}
