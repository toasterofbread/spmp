package com.toasterofbread.spmp.ui.layout.contentbar

import LocalPlayerState
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.Crossfade
import com.toasterofbread.composekit.settings.ui.item.SettingsItem
import com.toasterofbread.composekit.settings.ui.item.ComposableSettingsItem
import com.toasterofbread.composekit.settings.ui.Theme
import com.toasterofbread.composekit.platform.composable.BackHandler
import com.toasterofbread.spmp.model.settings.category.LayoutSettings
import com.toasterofbread.spmp.model.settings.SettingsKey
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.platform.FormFactor
import com.toasterofbread.spmp.platform.form_factor
import com.toasterofbread.spmp.resources.getString
import kotlinx.serialization.json.Json
import kotlinx.serialization.*
import com.toasterofbread.composekit.utils.common.toHexString

@OptIn(ExperimentalLayoutApi::class)
fun getLayoutSlotEditorSettingsItems(): List<SettingsItem> {
    return listOf(
        ComposableSettingsItem(
            listOf(
                LayoutSettings.Key.PORTRAIT_SLOTS.getName(),
                LayoutSettings.Key.LANDSCAPE_SLOTS.getName(),
                LayoutSettings.Key.CUSTOM_BARS.getName()
            ),
            composable = {
                LayoutSlotEditor(it)
            }
        )
    )
}

@Composable
fun LayoutSlotEditor(modifier: Modifier = Modifier) {
    val player: PlayerState = LocalPlayerState.current

    var custom_bars_data: String by LayoutSettings.Key.CUSTOM_BARS.rememberMutableState()
    var slot_colours_data: String by LayoutSettings.Key.SLOT_COLOURS.rememberMutableState()

    val slots_key: SettingsKey = when (player.form_factor) {
        FormFactor.PORTRAIT -> LayoutSettings.Key.PORTRAIT_SLOTS
        FormFactor.LANDSCAPE -> LayoutSettings.Key.LANDSCAPE_SLOTS
    }
    val available_slots: List<LayoutSlot> = when (player.form_factor) {
        FormFactor.PORTRAIT -> PortraitLayoutSlot.entries
        FormFactor.LANDSCAPE -> LandscapeLayoutSlot.entries
    }

    var editing_custom_bar: ContentBarReference? by remember { mutableStateOf(null) }

    BackHandler(editing_custom_bar != null) {
        editing_custom_bar = null
    }

    val state: ContentBar.BarSelectionState = remember {
        object : ContentBar.BarSelectionState {
            private fun parseSlots(): Map<String, Int> =
                Json.decodeFromString(slots_key.get<String>())

            override val built_in_bars: List<ContentBarReference> get() =
                InternalContentBar.getAll()
                    .mapIndexed { index, bar ->
                        ContentBarReference(bar, index + 1)
                    }

            override val custom_bars: List<ContentBarReference> get() =
                Json.decodeFromString<List<CustomContentBar>>(custom_bars_data)
                    .mapIndexed { index, bar ->
                        ContentBarReference(bar, -(index + 1))
                    }

            override fun onBarSelected(slot: LayoutSlot, bar: ContentBarReference?) {
                val slots: MutableMap<String, Int> = parseSlots().toMutableMap()
                slots[slot.getKey()] = bar?.second ?: 0
                slots_key.set(Json.encodeToString(slots))
            }

            override fun onThemeColourSelected(slot: LayoutSlot, colour: Theme.Colour) {
                setColour(slot, colour.ordinal.toString())
            }

            override fun onCustomColourSelected(slot: LayoutSlot, colour: Color) {
                setColour(slot, colour.toHexString())
            }

            override fun createCustomBar(): ContentBarReference {
                val bars: List<CustomContentBar> = Json.decodeFromString(custom_bars_data)

                val new_bar: CustomContentBar = CustomContentBar(
                    bar_name = getString("content_bar_custom_no_\$x").replace("\$x", (bars.size + 1).toString())
                )
                custom_bars_data = Json.encodeToString(bars + new_bar)

                return ContentBarReference(new_bar, -(bars.size + 1))
            }

            override fun onCustomBarEditRequested(bar: ContentBarReference) {
                editing_custom_bar = bar
            }

            override fun deleteCustomBar(bar: ContentBarReference) {
                val bars = Json.decodeFromString<List<CustomContentBar>>(custom_bars_data).toMutableList()

                val removed_index: Int = -bar.second - 1
                bars.removeAt(removed_index)

                val slots: MutableMap<String, Int> = parseSlots().toMutableMap()
                for (slot in slots.entries) {
                    if (slot.value >= 0) {
                        continue
                    }

                    val slot_index: Int = -slot.value - 1
                    if (slot_index == removed_index) {
                        slots[slot.key] = 0
                    }
                    else if (slot_index > removed_index) {
                        slots[slot.key] = slot.value + 1
                    }
                }

                custom_bars_data = Json.encodeToString(bars)
                slots_key.set(Json.encodeToString(slots))
            }

            private fun setColour(slot: LayoutSlot, colour: String) {
                val colours: MutableMap<String, String> =
                    Json.decodeFromString<Map<String, String>>(slot_colours_data).toMutableMap()

                colours[slot.getKey()] = colour
                slot_colours_data = Json.encodeToString(colours)
            }
        }
    }

    DisposableEffect(Unit) {
        ContentBar.bar_selection_state = state
        onDispose {
            ContentBar.bar_selection_state = null
        }
    }

    Crossfade(Triple(slots_key, available_slots, editing_custom_bar), modifier) {
        val (key, available, editing_bar) = it

        if (editing_bar != null) {
            CustomContentBarEditor(editing_bar.first as CustomContentBar) { edited_bar: CustomContentBar ->
                val bars = Json.decodeFromString<List<CustomContentBar>>(custom_bars_data).toMutableList()
                bars[-editing_bar.second - 1] = edited_bar
                custom_bars_data = Json.encodeToString(bars)
            }
        }
        else {
            CustomBarsContentBarList(
                state,
                onSelected = null,
                onDismissed = {}
            )
        }
    }
}
