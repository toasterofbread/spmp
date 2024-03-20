package com.toasterofbread.spmp.ui.shortcut

import com.toasterofbread.spmp.ui.shortcut.trigger.ShortcutTrigger
import com.toasterofbread.spmp.ui.shortcut.Shortcut
import com.toasterofbread.spmp.ui.layout.contentbar.LayoutSlot
import com.toasterofbread.spmp.ui.layout.contentbar.ContentBarReference
import com.toasterofbread.spmp.ui.layout.contentbar.CustomContentBar
import com.toasterofbread.spmp.ui.layout.contentbar.element.ContentBarElementButton
import com.toasterofbread.spmp.model.settings.category.LayoutSettings
import androidx.compose.runtime.*
import kotlinx.serialization.json.*

@Composable
fun ObserveAllShortcuts(): List<Shortcut> {
    val portrait_slot_data: String by LayoutSettings.Key.PORTRAIT_SLOTS.rememberMutableState()
    val portrait_slots: Map<String, ContentBarReference> = remember(portrait_slot_data) { Json.decodeFromString(portrait_slot_data) }

    val landscape_slot_data: String by LayoutSettings.Key.LANDSCAPE_SLOTS.rememberMutableState()
    val landscape_slots: Map<String, ContentBarReference> = remember(landscape_slot_data) { Json.decodeFromString(landscape_slot_data) }

    val custom_bars_data: String by LayoutSettings.Key.CUSTOM_BARS.rememberMutableState()
    val custom_bars: List<CustomContentBar> = remember(custom_bars_data) { Json.decodeFromString(custom_bars_data) }

    val shortcuts: MutableList<Shortcut> = mutableListOf()

    for (slot in portrait_slots.values + landscape_slots.values) {
        if (slot.type != ContentBarReference.Type.CUSTOM) {
            continue
        }

        val bar: CustomContentBar = custom_bars.getOrNull(slot.index) ?: continue
        for (element in bar.elements) {
            val shortcut: Shortcut = element.getShortcut() ?: continue
            shortcuts.add(shortcut)
        }
    }

    return shortcuts
}
