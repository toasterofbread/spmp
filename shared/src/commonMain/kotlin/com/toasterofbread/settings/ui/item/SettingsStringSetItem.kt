package com.toasterofbread.composesettings.ui.item

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.toasterofbread.composesettings.ui.SettingsPage
import com.toasterofbread.spmp.platform.PlatformPreferences
import com.toasterofbread.spmp.platform.composable.PlatformAlertDialog
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.utils.common.setAlpha
import com.toasterofbread.utils.composable.ScrollabilityIndicatorColumn
import com.toasterofbread.utils.composable.ShapedIconButton

class SettingsStringSetItem(
    val state: BasicSettingsValueState<Set<String>>,
    val title: String?,
    val subtitle: String?,
    val add_dialog_title: String,
    val single_line_content: Boolean = true,
    val max_height: Dp = 300.dp,
    val itemToText: @Composable (String) -> String = { it },
    val textToItem: (String) -> String = { it }
): SettingsItem() {
    override fun initialiseValueStates(prefs: PlatformPreferences, default_provider: (String) -> Any) {
        state.init(prefs, default_provider)
    }

    override fun releaseValueStates(prefs: PlatformPreferences) {
        state.release(prefs)
    }

    override fun resetValues() {
        state.reset()
    }

    @Composable
    override fun Item(
        theme: Theme,
        openPage: (Int, Any?) -> Unit,
        openCustomPage: (SettingsPage) -> Unit
    ) {
        val icon_button_colours = IconButtonDefaults.iconButtonColors(
            containerColor = theme.accent,
            contentColor = theme.on_accent,
            disabledContainerColor = theme.accent.setAlpha(0.5f)
        )

        var show_add_item_dialog: Boolean by remember { mutableStateOf(false) }
        if (show_add_item_dialog) {
            var new_item_content: String by remember { mutableStateOf("") }
            val item_already_added = state.get().contains(new_item_content)
            val can_add_item = new_item_content.isNotEmpty() && !item_already_added

            PlatformAlertDialog(
                onDismissRequest = { show_add_item_dialog = false },
                confirmButton = {
                    Crossfade(can_add_item) { enabled ->
                        ShapedIconButton(
                            {
                                state.set(state.get().plus(textToItem(new_item_content)))
                                show_add_item_dialog = false
                            },
                            colours = icon_button_colours,
                            enabled = enabled
                        ) {
                            Icon(Icons.Default.Done, null)
                        }
                    }
                },
                dismissButton = {
                    ShapedIconButton(
                        {
                            show_add_item_dialog = false
                        },
                        colours = icon_button_colours
                    ) {
                        Icon(Icons.Default.Close, null)
                    }
                },
                title = { Text(add_dialog_title) },
                text = {
                    TextField(
                        new_item_content,
                        { new_item_content = it },
                        singleLine = single_line_content,
                        isError = item_already_added,
                        label = if (item_already_added) {{
                            Text(getString("settings_string_set_item_already_added"))
                        }} else null
                    )
                }
            )
        }

        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    ItemTitleText(title, theme)
                    ItemText(subtitle, theme)
                }

                ShapedIconButton(
                    { show_add_item_dialog = true },
                    colours = icon_button_colours
                ) {
                    Icon(Icons.Default.Add, null)
                }
            }

            Crossfade(state.get(), Modifier.fillMaxWidth()) { set ->
                if (set.isEmpty()) {
                    Text(
                        getString("settings_string_set_item_empty"),
                        Modifier.fillMaxWidth().padding(top = 20.dp),
                        textAlign = TextAlign.Center
                    )
                }
                else {
                    val scroll_state = rememberLazyListState()
                    ScrollabilityIndicatorColumn(scroll_state, Modifier.heightIn(max = max_height)) {
                        LazyColumn(state = scroll_state) {
                            for (item in set) {
                                item {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(itemToText(item), Modifier.fillMaxWidth().weight(1f))

                                        IconButton(
                                            { state.set(set.minus(item)) }
                                        ) {
                                            Icon(Icons.Default.Remove, null, tint = theme.on_background)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
