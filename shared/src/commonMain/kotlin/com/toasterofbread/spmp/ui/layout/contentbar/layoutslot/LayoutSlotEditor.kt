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
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.toastbits.composekit.platform.composable.BackHandler
import dev.toastbits.composekit.settings.ui.item.*
import dev.toastbits.composekit.utils.composable.NullableValueAnimatedVisibility
import com.toasterofbread.spmp.platform.*
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.contentbar.ContentBarReference
import com.toasterofbread.spmp.ui.layout.contentbar.InternalContentBar
import com.toasterofbread.spmp.ui.layout.contentbar.ContentBar
import com.toasterofbread.spmp.ui.layout.contentbar.CustomContentBar
import com.toasterofbread.spmp.ui.layout.contentbar.CustomContentBarEditor
import com.toasterofbread.spmp.ui.layout.contentbar.CustomBarsContentBarList
import com.toasterofbread.spmp.ui.layout.contentbar.TemplateCustomContentBar
import com.toasterofbread.spmp.ui.layout.contentbar.CustomContentBarTemplate
import com.toasterofbread.spmp.ui.layout.contentbar.element.ContentBarElementContentBar
import com.toasterofbread.spmp.ui.layout.contentbar.element.ContentBarElement
import com.toasterofbread.spmp.ui.layout.contentbar.CircularReferenceWarning
import dev.toastbits.composekit.platform.PreferencesProperty
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

fun getLayoutSlotEditorSettingsItems(context: AppContext): List<SettingsItem> {
    return listOf(
        ComposableSettingsItem(
            listOf(
                context.settings.layout.PORTRAIT_SLOTS,
                context.settings.layout.LANDSCAPE_SLOTS,
                context.settings.layout.CUSTOM_BARS
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
    val form_factor: FormFactor by FormFactor.observe()

    var custom_bars: List<CustomContentBar> by player.settings.layout.CUSTOM_BARS.observe()
    var slot_colours: Map<String, ColourSource> by player.settings.layout.SLOT_COLOURS.observe()
    var slot_config: Map<String, JsonElement> by player.settings.layout.SLOT_CONFIGS.observe()

    val slots_property: PreferencesProperty<Map<String, ContentBarReference?>> =
        when (form_factor) {
            FormFactor.PORTRAIT -> player.settings.layout.PORTRAIT_SLOTS
            FormFactor.LANDSCAPE -> player.settings.layout.LANDSCAPE_SLOTS
        }
    val available_slots: List<LayoutSlot> =
        when (form_factor) {
            FormFactor.PORTRAIT -> PortraitLayoutSlot.entries
            FormFactor.LANDSCAPE -> LandscapeLayoutSlot.entries
        }

    val configured_slots: Map<String, ContentBarReference?> by slots_property.observe()

    val missing_bar_warnings: List<String> = remember(configured_slots) {
        var has_primary: Boolean = false
        var has_secondary: Boolean = false

        for (slot in available_slots) {
            val bar: ContentBar

            if (!configured_slots.contains(slot.getKey())) {
                bar = slot.getDefaultContentBar() ?: continue
            }
            else {
                bar = configured_slots[slot.getKey()]?.getBar(player.context) ?: continue
            }

            if (bar == InternalContentBar.PRIMARY) {
                has_primary = true
                continue
            }

            if (bar == InternalContentBar.SECONDARY) {
                has_secondary = true
                continue
            }

            val elements: List<ContentBarElement>

            if (bar is CustomContentBar) {
                elements = bar.elements
            }
            else if (bar is TemplateCustomContentBar) {
                elements = bar.template.getElements()
            }
            else {
                continue
            }

            for (element in elements) {
                val element_bar: ContentBarReference = (element as? ContentBarElementContentBar)?.bar ?: continue
                if (element_bar.type != ContentBarReference.Type.INTERNAL) {
                    continue
                }

                if (element_bar.index == InternalContentBar.PRIMARY.index) {
                    has_primary = true
                }
                else if (element_bar.index == InternalContentBar.SECONDARY.index) {
                    has_secondary = true
                }
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

    val state: ContentBar.BarSelectionState = remember(slots_property, available_slots) {
        object : ContentBar.BarSelectionState {
            private fun parseSlots(): Map<String, ContentBarReference?> =
                slots_property.get()

            override val built_in_bars: List<ContentBarReference> get() = (
                InternalContentBar.ALL.map { bar ->
                    ContentBarReference.ofInternalBar(bar)
                }
                + CustomContentBarTemplate.entries.map { template ->
                    ContentBarReference.ofTemplate(template)
                }
            )

            override val custom_bars: List<ContentBarReference> get() =
                custom_bars.indices.map { index ->
                    ContentBarReference.ofCustomBar(index)
                }

            override fun onBarSelected(slot: LayoutSlot, bar: ContentBarReference?) {
                val slots: MutableMap<String, ContentBarReference?> = parseSlots().toMutableMap()
                slots[slot.getKey()] = bar
                slots_property.set(slots)
            }

            override fun onColourSelected(slot: LayoutSlot, colour: ColourSource) {
                val colours: MutableMap<String, ColourSource> = slot_colours.toMutableMap()
                colours[slot.getKey()] = colour
                slot_colours = colours
            }

            override fun onSlotConfigChanged(slot: LayoutSlot, config: JsonElement?) {
                val configs: MutableMap<String, JsonElement> = slot_config.toMutableMap()

                if (config == null) {
                    configs.remove(slot.getKey())
                }
                else {
                    configs[slot.getKey()] = config
                }

                slot_config = configs
            }

            override fun createCustomBar(): ContentBarReference {
                val new_bar: CustomContentBar = CustomContentBar(
                    bar_name = getString("content_bar_custom_no_\$x").replace("\$x", (custom_bars.size + 1).toString())
                )
                custom_bars += new_bar

                return ContentBarReference.ofCustomBar(custom_bars.size - 1)
            }

            override fun onCustomBarEditRequested(bar: ContentBarReference) {
                editing_custom_bar = bar
            }

            override fun deleteCustomBar(bar: ContentBarReference) {
                check(bar.type == ContentBarReference.Type.CUSTOM)

                val bars: MutableList<CustomContentBar> = custom_bars.toMutableList()

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

                custom_bars = bars
                slots_property.set(slots)
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

        var show_circular_reference_warning: Boolean by remember { mutableStateOf(false) }
        if (show_circular_reference_warning) {
            CircularReferenceWarning { show_circular_reference_warning = false }
        }

        Crossfade(Triple(slots_property, available_slots, editing_custom_bar), modifier) {
            val (key, available, editing_bar) = it

            if (editing_bar != null) {
                val editor: CustomContentBarEditor = remember {
                    object : CustomContentBarEditor() {
                        override fun commit(edited_bar: CustomContentBar): Boolean {
                            val bars: MutableList<CustomContentBar> = custom_bars.toMutableList()
                            bars[editing_bar.index] = edited_bar

                            if (doesCustomBarRecurseInfinitely(editing_bar.index, bars)) {
                                show_circular_reference_warning = true
                                return false
                            }

                            custom_bars = bars
                            return true
                        }
                    }
                }

                val bar: ContentBar? = remember(editing_bar) { editing_bar.getBar(player.context) }
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

private fun doesCustomBarRecurseInfinitely(bar_index: Int, custom_bars: List<CustomContentBar>): Boolean {
    val traversed: MutableList<Int> = mutableListOf(bar_index)
    val bars: MutableList<Int> = mutableListOf(bar_index)

    while (bars.isNotEmpty()) {
        val custom_bar: CustomContentBar = custom_bars[bars.removeLast()]
        for (element in custom_bar.elements) {
            for (bar_reference in element.getContainedBars()) {
                if (bar_reference.type != ContentBarReference.Type.CUSTOM) {
                    continue
                }

                if (traversed.contains(bar_reference.index)) {
                    return true
                }

                bars.add(bar_reference.index)
                traversed.add(bar_reference.index)
            }
        }
    }

    return false
}
