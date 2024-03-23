package com.toasterofbread.spmp.ui.layout.contentbar.layoutslot

import LocalPlayerState
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.toasterofbread.composekit.platform.composable.BackHandler
import com.toasterofbread.composekit.settings.ui.Theme
import com.toasterofbread.composekit.settings.ui.item.*
import com.toasterofbread.composekit.utils.common.toHexString
import com.toasterofbread.composekit.utils.composable.NullableValueAnimatedVisibility
import com.toasterofbread.spmp.model.settings.SettingsKey
import com.toasterofbread.spmp.model.settings.category.LayoutSettings
import com.toasterofbread.spmp.platform.*
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.contentbar.ContentBarReference
import com.toasterofbread.spmp.ui.layout.contentbar.InternalContentBar
import com.toasterofbread.spmp.ui.layout.contentbar.ContentBar
import com.toasterofbread.spmp.ui.layout.contentbar.CustomContentBar
import com.toasterofbread.spmp.ui.layout.contentbar.CustomContentBarEditor
import com.toasterofbread.spmp.ui.layout.contentbar.CustomBarsContentBarList
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.ColourSource
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

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
                LayoutSlotEditor(it) {
                    goBack()
                }
            }
        )
    )
}

@Composable
fun LayoutSlotEditor(
    modifier: Modifier = Modifier,
    onClose: () -> Unit
) {
    val player: PlayerState = LocalPlayerState.current

    var custom_bars_data: String by LayoutSettings.Key.CUSTOM_BARS.rememberMutableState()
    var slot_colours_data: String by LayoutSettings.Key.SLOT_COLOURS.rememberMutableState()
    var slot_config_data: String by LayoutSettings.Key.SLOT_CONFIGS.rememberMutableState()

    val slots_key: SettingsKey = when (player.form_factor) {
        FormFactor.PORTRAIT -> LayoutSettings.Key.PORTRAIT_SLOTS
        FormFactor.LANDSCAPE -> LayoutSettings.Key.LANDSCAPE_SLOTS
    }
    val available_slots: List<LayoutSlot> = when (player.form_factor) {
        FormFactor.PORTRAIT -> PortraitLayoutSlot.entries
        FormFactor.LANDSCAPE -> LandscapeLayoutSlot.entries
    }

    val slots_data: String by slots_key.rememberMutableState()
    val configured_slots: Map<String, ContentBarReference?> = remember(slots_data) { Json.decodeFromString(slots_data) }

    val missing_bar_warnings: List<String> = remember(configured_slots) {
        var has_primary: Boolean = false
        var has_secondary: Boolean = false

        for (slot in configured_slots.values) {
            if (slot?.type != ContentBarReference.Type.INTERNAL) {
                continue
            }

            if (slot.index == InternalContentBar.PRIMARY.index) {
                has_primary = true
            }
            else if (slot.index == InternalContentBar.SECONDARY.index) {
                has_secondary = true
            }
        }

        return@remember listOfNotNull(
            if (!has_primary) getString("content_bar_selection_warn_no_primary")
            else null,
            if (!has_secondary) getString("content_bar_selection_warn_no_secondary")
            else null
        )
    }

    var show_warning_exit_dialog: Boolean by remember { mutableStateOf(false) }
    var editing_custom_bar: ContentBarReference? by remember { mutableStateOf(null) }

    BackHandler(editing_custom_bar != null || missing_bar_warnings.isNotEmpty()) {
        if (editing_custom_bar != null) {
            editing_custom_bar = null
        }
        else {
            show_warning_exit_dialog = true
        }
    }

    if (show_warning_exit_dialog) {
        AlertDialog(
            onDismissRequest = { show_warning_exit_dialog = false },
            confirmButton = {
                Button(onClose) {
                    Text(getString("action_confirm_action"))
                }
            },
            dismissButton = {
                Button({ show_warning_exit_dialog = false }) {
                    Text(getString("action_deny_action"))
                }
            },
            title = { Text(getString("content_bar_selection_exit_warning_title")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Warning, null)

                        Column {
                            for (warning in missing_bar_warnings) {
                                Text(warning)
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Text(getString("content_bar_selection_exit_warning_text"))
                }
            }
        )
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

            override fun onColourSelected(slot: LayoutSlot, colour: ColourSource) {
                val colours: MutableMap<String, ColourSource> =
                    Json.decodeFromString<Map<String, ColourSource>>(slot_colours_data).toMutableMap()

                colours[slot.getKey()] = colour
                slot_colours_data = Json.encodeToString(colours)
            }

            override fun onSlotConfigChanged(slot: LayoutSlot, config: JsonElement?) {
                val configs: MutableMap<String, JsonElement> =
                    Json.decodeFromString<Map<String, JsonElement>>(slot_config_data).toMutableMap()

                if (config == null) {
                    configs.remove(slot.getKey())
                }
                else {
                    configs[slot.getKey()] = config
                }

                slot_config_data = Json.encodeToString(configs)
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
        }
    }

    DisposableEffect(state) {
        ContentBar.bar_selection_state = state
        onDispose {
            ContentBar.bar_selection_state = null
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        NullableValueAnimatedVisibility(
            missing_bar_warnings.takeIf { it.isNotEmpty() },
            Modifier.fillMaxWidth(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) { warnings ->
            if (warnings == null) {
                return@NullableValueAnimatedVisibility
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .background(player.theme.card)
                    .border(2.dp, Color.Red)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Icon(Icons.Default.Warning, null)

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (warning in warnings) {
                        Text(warning)
                    }
                }
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
}
