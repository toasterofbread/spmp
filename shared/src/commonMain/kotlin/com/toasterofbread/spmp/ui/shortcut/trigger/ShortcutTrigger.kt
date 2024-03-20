package com.toasterofbread.spmp.ui.shortcut.trigger

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import kotlinx.serialization.json.*
import kotlinx.serialization.*
import kotlinx.serialization.Serializable

@Serializable
sealed interface ShortcutTrigger {
    fun getType(): Type

    @Composable
    fun IndicatorContent(modifier: Modifier)

    @Composable
    fun ConfigurationItems(item_modifier: Modifier, onModification: (ShortcutTrigger) -> Unit)

    enum class Type {
        KEYBOARD_SHORTCUT;

        fun getName(): String =
            when (this) {
                KEYBOARD_SHORTCUT -> "Keyboard // TODO"
            }

        fun getIcon(): ImageVector =
            when (this) {
                KEYBOARD_SHORTCUT -> Icons.Default.Keyboard
            }

        fun create(): ShortcutTrigger =
            when (this) {
                KEYBOARD_SHORTCUT -> KeyboardShortcutTrigger()
            }
    }

    // companion object {
    //     @Serializable
    //     private data class TriggerData(type: Type, trigger: ShortcutTrigger)

    //     fun serialise(trigger: ShortcutTrigger): String =
    //         Json.encodeToString(TriggerData(type, trigger))

    //     fun deserialise(data: String): ShortcutTrigger {
    //         val data: JsonObject = Json.decodeFromString(data)

    //         val type: Type = Type.entries.first { it.name == data["type"].jsonPrimitive.content }
    //         return when (type) {
    //             Type.KEYBOARD_SHORTCUT -> Json.decodeFromJsonElement<KeyboardShortcutTrigger>(data.trigger)
    //         }
    //     }
    // }
}
