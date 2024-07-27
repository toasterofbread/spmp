package com.toasterofbread.spmp.ui.component.shortcut.trigger

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.Icons
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.shortcut_trigger_none
import spmp.shared.generated.resources.shortcut_trigger_keyboard
import spmp.shared.generated.resources.shortcut_trigger_mouse_button

@Serializable
sealed interface ShortcutTrigger {
    fun getType(): Type

    @Composable
    fun IndicatorContent(modifier: Modifier)

    @Composable
    fun ConfigurationItems(item_modifier: Modifier, onModification: (ShortcutTrigger) -> Unit)

    enum class Type {
        KEYBOARD,
        MOUSE_BUTTON;

        fun create(): ShortcutTrigger =
            when (this) {
                KEYBOARD -> KeyboardShortcutTrigger()
                MOUSE_BUTTON -> MouseButtonShortcutTrigger()
            }

        fun getIcon(): ImageVector =
            when (this) {
                KEYBOARD -> Icons.Default.Keyboard
                MOUSE_BUTTON -> Icons.Default.Mouse
            }
    }
}

@Composable
fun ShortcutTrigger.Type?.getName(): String =
    when (this) {
        null -> stringResource(Res.string.shortcut_trigger_none)
        ShortcutTrigger.Type.KEYBOARD -> stringResource(Res.string.shortcut_trigger_keyboard)
        ShortcutTrigger.Type.MOUSE_BUTTON -> stringResource(Res.string.shortcut_trigger_mouse_button)
    }
