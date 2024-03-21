package com.toasterofbread.spmp.ui.component.shortcut.trigger

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.Icons
import com.toasterofbread.spmp.resources.getString

@Serializable
sealed interface ShortcutTrigger {
    fun getType(): Type

    @Composable
    fun IndicatorContent(modifier: Modifier)

    @Composable
    fun ConfigurationItems(item_modifier: Modifier, onModification: (ShortcutTrigger) -> Unit)

    enum class Type {
        KEYBOARD;

        fun create(): ShortcutTrigger =
            when (this) {
                KEYBOARD -> KeyboardShortcutTrigger()
            }

        fun getIcon(): ImageVector =
            when (this) {
                KEYBOARD -> Icons.Default.Keyboard
            }
    }
}

fun ShortcutTrigger.Type?.getName(): String =
    when (this) {
        null -> getString("shortcut_trigger_none")
        ShortcutTrigger.Type.KEYBOARD -> getString("shortcut_trigger_keyboard")
    }
