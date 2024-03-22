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

    val state: ContentBar.BarSelectionState = remember(slots_key, available_slots) {
        object : ContentBar.BarSelectionState {
            private fun parseSlots(): Map<String, ContentBarReference?> =
                Json.decodeFromString(slots_key.get<String>())

            override val built_in_bars: List<ContentBarReference> get() =
                InternalContentBar.ALL.map { bar ->
                    ContentBarReference.ofInternalBar(bar)
                }

            override val custom_bars: List<ContentBarReference> get() =
                Json.decodeFromString<List<CustomContentBar>>(custom_bars_data).indices.map { index ->
                    ContentBarReference.ofCustomBar(index)
                }

            override fun onBarSelected(slot: LayoutSlot, bar: ContentBarReference?) {
                val slots: MutableMap<String, ContentBarReference?> = parseSlots().toMutableMap()
                slots[slot.getKey()] = bar
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

                return ContentBarReference.ofCustomBar(bars.size)
            }

            override fun onCustomBarEditRequested(bar: ContentBarReference) {
                editing_custom_bar = bar
            }

            override fun deleteCustomBar(bar: ContentBarReference) {
                check(bar.type == ContentBarReference.Type.CUSTOM)

                val bars = Json.decodeFromString<List<CustomContentBar>>(custom_bars_data).toMutableList()

                val removed_index: Int = bar.index
                bars.removeAt(removed_index)

                val slots: MutableMap<String, ContentBarReference?> = parseSlots().toMutableMap()
                for ((key, slot) in slots.entries) {
                    if (slot?.type != ContentBarReference.Type.CUSTOM) {
                        continue
                    }

                    if (slot.index == removed_index) {
                        slots[key] = null
                    }
                    else if (slot.index > removed_index) {
                        slots[key] = slot.copy(index = slot.index - 1)
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

    DisposableEffect(state) {
        ContentBar.bar_selection_state = state
        onDispose {
            ContentBar.bar_selection_state = null
        }
    }

    Crossfade(Triple(slots_key, available_slots, editing_custom_bar), modifier) {
        val (key, available, editing_bar) = it

        if (editing_bar != null) {
            val editor: CustomContentBarEditor = remember {
                object : CustomContentBarEditor() {
                    override fun commit(edited_bar: CustomContentBar) {
                        val bars = Json.decodeFromString<List<CustomContentBar>>(custom_bars_data).toMutableList()
                        bars[editing_bar.index] = edited_bar
                        custom_bars_data = Json.encodeToString(bars)
                    }
                }
            }

            val bar: ContentBar? = remember(editing_bar) { editing_bar.getBar() }
            editor.Editor(bar as CustomContentBar)
        }
        else {
            CustomBarsContentBarList(
                state,
                onSelected = null,
                onDismissed = {},
                bar_background_colour = player.theme.vibrant_accent.copy(alpha = 0.15f)
            )
        }
    }
}
