package com.toasterofbread.spmp.ui.layout.contentbar

import LocalPlayerState
import androidx.compose.animation.*
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.*
import com.toasterofbread.composekit.platform.composable.*
import com.toasterofbread.composekit.settings.ui.Theme
import com.toasterofbread.composekit.settings.ui.item.*
import com.toasterofbread.composekit.utils.common.*
import com.toasterofbread.composekit.utils.composable.*
import com.toasterofbread.spmp.model.settings.SettingsKey
import com.toasterofbread.spmp.model.settings.category.LayoutSettings
import com.toasterofbread.spmp.platform.*
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.contentbar.ContentBarReference
import com.toasterofbread.spmp.ui.layout.contentbar.element.ContentBarElement
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.burnoutcrew.reorderable.*

@OptIn(ExperimentalLayoutApi::class)
fun getLayoutSlotEditorSettingsItems(): List<SettingsItem> {
    return listOf(
        ComposableSettingsItem(
            emptyList(),
            composable = {
                val player: PlayerState = LocalPlayerState.current
                var preview_options_expanded: Boolean by remember { mutableStateOf(false) }

                DisposableEffect(Unit) {
                    onDispose {
                        FormFactor.form_factor_override = null
                    }
                }

                Column(
                    Modifier
                        .border(2.dp, player.theme.vibrant_accent, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .platformClickable(
                                onClick = { preview_options_expanded = !preview_options_expanded }
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            getString("layout_editor_preview_options"),
                            Modifier.padding(bottom = 10.dp),
                            style = MaterialTheme.typography.headlineSmall
                        )

                        Crossfade(preview_options_expanded) { expanded ->
                            IconButton({ preview_options_expanded = !expanded }) {
                                Icon(
                                    if (expanded) Icons.Default.KeyboardArrowUp
                                    else Icons.Default.KeyboardArrowDown,
                                    null
                                )
                            }
                        }
                    }

                    AnimatedVisibility(preview_options_expanded) {
                        Column {
                            SwitchButton(
                                checked = ContentBar.disable_bar_selection,
                                onCheckedChange = { checked ->
                                    ContentBar.disable_bar_selection = checked
                                }
                            ) {
                                Text(getString("layout_editor_preview_option_show_bar_content"))
                            }

                            SwitchButton(
                                checked = player.hide_player,
                                onCheckedChange = { checked ->
                                    player.hide_player = checked
                                }
                            ) {
                                Text(getString("layout_editor_preview_option_hide_player"))
                            }

                            SwitchButton(
                                checked = player.form_factor == FormFactor.PORTRAIT,
                                onCheckedChange = { checked ->
                                    FormFactor.form_factor_override =
                                        if (checked) FormFactor.PORTRAIT
                                        else FormFactor.LANDSCAPE
                                }
                            ) {
                                Text(getString("layout_editor_preview_option_portrait_mode"))
                            }
                        }
                    }
                }
            }
        ),
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

@Composable
private fun SwitchButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    text: @Composable () -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        text()

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
