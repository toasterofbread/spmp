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

@Composable
fun LayoutSlotEditor(modifier: Modifier) {
    val player: PlayerState = LocalPlayerState.current

    val custom_bars_data: String by LayoutSettings.Key.CUSTOM_BARS.rememberMutableState()

    var editing_portrait: Boolean = player.form_factor == FormFactor.PORTRAIT

    DisposableEffect(Unit) {
        ContentBar.bar_selection_state = object : ContentBar.BarSelectionState {
            override val available_bars: List<Pair<ContentBar, Int>>
                get() {
                    val custom_bars: List<CustomContentBar> = Json.decodeFromString(custom_bars_data)
                    return (
                        InternalContentBar.getAll().mapIndexed { index, bar -> Pair(bar, index) }
                        + custom_bars.mapIndexed { index, bar -> Pair(bar, -(index + 1)) }
                    )
                }

            override fun onBarSelected(slot: LayoutSlot, bar: Pair<ContentBar, Int>?) {
                println("SELECTED $slot $bar")
            }
        }

        onDispose {
            ContentBar.bar_selection_state = null
        }
    }

    Crossfade(editing_portrait, modifier) { portrait ->
        val slots_key: SettingsKey = if (portrait) LayoutSettings.Key.PORTRAIT_SLOTS else LayoutSettings.Key.LANDSCAPE_SLOTS
        val available_slots: List<LayoutSlot> = if (portrait) PortraitLayoutSlot.entries else LandscapeLayoutSlot.entries

        SlotEditor(slots_key, available_slots)
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
