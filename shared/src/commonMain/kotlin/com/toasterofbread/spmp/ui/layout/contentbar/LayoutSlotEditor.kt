package com.toasterofbread.spmp.ui.layout.contentbar

import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import LocalPlayerState
import kotlinx.serialization.json.Json
import org.burnoutcrew.reorderable.ReorderableLazyListState
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import com.toasterofbread.composekit.platform.composable.ScrollBarLazyColumn
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxSize
import org.burnoutcrew.reorderable.reorderable
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorder
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.items
import com.toasterofbread.spmp.platform.form_factor
import com.toasterofbread.spmp.platform.FormFactor
import com.toasterofbread.spmp.model.settings.SettingsKey
import androidx.compose.runtime.*
import androidx.compose.animation.Crossfade
import com.toasterofbread.spmp.model.settings.category.LayoutSettings
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import kotlinx.serialization.encodeToString
import com.toasterofbread.composekit.settings.ui.item.SettingsItem
import com.toasterofbread.composekit.settings.ui.item.ComposableSettingsItem
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.composekit.utils.common.getContrasted
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.material3.Switch
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.LaunchedEffect
import com.toasterofbread.composekit.utils.common.fromHexString
import com.toasterofbread.composekit.utils.common.toHexString
import com.toasterofbread.composekit.settings.ui.Theme

@OptIn(ExperimentalLayoutApi::class)
fun getLayoutSlotEditorSettingsItems(): List<SettingsItem> {
    return listOf(
        ComposableSettingsItem(
            emptyList(),
            composable = {
                val player: PlayerState = LocalPlayerState.current

                Column(
                    Modifier
                        .border(2.dp, player.theme.vibrant_accent, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                    ) {
                    Text(
                        "Preview options",
                        Modifier.padding(bottom = 10.dp),
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Spacer(Modifier.fillMaxWidth().weight(1f))

                    SwitchButton(
                        checked = ContentBar.disable_bar_selection,
                        onCheckedChange = { checked ->
                            ContentBar.disable_bar_selection = checked
                        }
                    ) {
                        Text("Show bar content")
                    }

                    SwitchButton(
                        checked = player.hide_player,
                        onCheckedChange = { checked ->
                            player.hide_player = checked
                        }
                    ) {
                        Text("Hide player")
                    }

                    SwitchButton(
                        checked = player.form_factor == FormFactor.PORTRAIT,
                        onCheckedChange = { checked ->
                            FormFactor.form_factor_override =
                                if (checked) FormFactor.PORTRAIT
                                else FormFactor.LANDSCAPE
                        }
                    ) {
                        Text("Portrait mode")
                    }

                    DisposableEffect(Unit) {
                        onDispose {
                            FormFactor.form_factor_override = null
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

    val custom_bars_data: String by LayoutSettings.Key.CUSTOM_BARS.rememberMutableState()
    var slot_colours_data: String by LayoutSettings.Key.SLOT_COLOURS.rememberMutableState()

    val slots_key: SettingsKey = when (player.form_factor) {
        FormFactor.PORTRAIT -> LayoutSettings.Key.PORTRAIT_SLOTS
        FormFactor.LANDSCAPE -> LayoutSettings.Key.LANDSCAPE_SLOTS
    }
    val available_slots: List<LayoutSlot> = when (player.form_factor) {
        FormFactor.PORTRAIT -> PortraitLayoutSlot.entries
        FormFactor.LANDSCAPE -> LandscapeLayoutSlot.entries
    }

    DisposableEffect(Unit) {
        ContentBar.bar_selection_state = object : ContentBar.BarSelectionState {
            override val available_bars: List<Pair<ContentBar, Int>>
                get() {
                    val custom_bars: List<CustomContentBar> = Json.decodeFromString(custom_bars_data)
                    return (
                        InternalContentBar.getAll().mapIndexed { index, bar -> Pair(bar, index + 1) }
                        + custom_bars.mapIndexed { index, bar -> Pair(bar, -(index + 1)) }
                    )
                }

            override fun onBarSelected(slot: LayoutSlot, bar: Pair<ContentBar, Int>?) {
                val slots: MutableMap<String, Int> = Json.decodeFromString<Map<String, Int>>(slots_key.get<String>()).toMutableMap()
                slots[slot.getKey()] = bar?.second ?: 0
                slots_key.set(Json.encodeToString(slots))
            }

            override fun onThemeColourSelected(slot: LayoutSlot, colour: Theme.Colour) {
                setColour(slot, colour.ordinal.toString())
            }

            override fun onCustomColourSelected(slot: LayoutSlot, colour: Color) {
                setColour(slot, colour.toHexString())
            }

            private fun setColour(slot: LayoutSlot, colour: String) {
                val colours: MutableMap<String, String> =
                    Json.decodeFromString<Map<String, String>>(slot_colours_data).toMutableMap()

                colours[slot.getKey()] = colour
                slot_colours_data = Json.encodeToString(colours)
            }
        }

        onDispose {
            ContentBar.bar_selection_state = null
        }
    }

    Crossfade(Pair(slots_key, available_slots), modifier) {
        val (key, available) = it
        SlotEditor(key, available)
    }
}

@Composable
private fun SlotEditor(slots_key: SettingsKey, available_slots: List<LayoutSlot>, modifier: Modifier = Modifier) {
    val slots_data: String by slots_key.rememberMutableState()

    var slots: List<Int?> by remember { mutableStateOf(
        available_slots.map { Json.decodeFromString<Map<String, Int>>(slots_data).get(it.getKey()) }
    ) }

    val bar_height: Dp = 100.dp

    val slot_list_state: ReorderableLazyListState = rememberReorderableLazyListState(
        onMove = { from, to ->
            val new = slots.toMutableList()
            new.add(to.index, new.removeAt(from.index))
            slots = new
        },
        onDragEnd = { from, to ->

        }
    )

    Column(modifier) {
        SlotOrderEditorLandscape(available_slots, slots, slot_list_state)
    }
}

@Composable
private fun SlotOrderEditorPortrait(
    available_slots: List<LayoutSlot>,
    slots: List<Int?>,
    slot_list_state: ReorderableLazyListState,
    modifier: Modifier = Modifier
) {
    TODO()
}

@Composable
private fun SlotOrderEditorLandscape(
    available_slots: List<LayoutSlot>,
    slots: List<Int?>,
    slot_list_state: ReorderableLazyListState,
    modifier: Modifier = Modifier
) {
    val player: PlayerState = LocalPlayerState.current
    val slot_height: Dp = 30.dp

    Row(modifier) {
        Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (slot in available_slots) {
                Text(slot.getName(), Modifier.height(slot_height))
            }
        }

        // ScrollBarLazyColumn(
        //     state = slot_list_state.listState,
        //     modifier = Modifier
        //         .fillMaxHeight()
        //         .fillMaxWidth()
        //         .weight(1f)
        //         .reorderable(slot_list_state),
        //     horizontalAlignment = Alignment.CenterHorizontally,
        //     verticalArrangement = Arrangement.spacedBy(20.dp)
        // ) {
        //     items(slots) { slot ->
        //         ReorderableItem(
        //             slot_list_state,
        //             slot,
        //             Modifier.detectReorder(slot_list_state)
        //         ) {
        //             slot.Bar(
        //                 player.app_page,
        //                 Modifier
        //                     .border(2.dp, Color.White)
        //                     .height(slot_height)
        //             )
        //         }
        //     }
        // }
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
