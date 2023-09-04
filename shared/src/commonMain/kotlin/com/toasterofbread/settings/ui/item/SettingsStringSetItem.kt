package com.toasterofbread.composesettings.ui.item

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.toasterofbread.composesettings.ui.SettingsPage
import com.toasterofbread.spmp.platform.PlatformPreferences
import com.toasterofbread.spmp.platform.composable.PlatformAlertDialog
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.utils.composable.ShapedIconButton
import com.toasterofbread.utils.common.setAlpha
import kotlinx.coroutines.launch

class SettingsStringSetItem(
    val state: BasicSettingsValueState<Set<String>>,
    val title: String?,
    val subtitle: String?,
    val add_dialog_title: String,
    val single_line_content: Boolean = true,
    val max_height: Dp = 300.dp
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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun GetItem(
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
                                state.set(state.get().plus(new_item_content))
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
                    ScrollabilityIndicatorLazyColumn(Modifier.heightIn(max = max_height)) {
                        for (item in set) {
                            item {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(item, Modifier.fillMaxWidth().weight(1f))

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

@Composable
fun ScrollabilityIndicatorLazyColumn(
    modifier: Modifier = Modifier,
    show_up_indicator: Boolean = false,
    scroll_amount: Float? = with(LocalDensity.current) { 50.dp.toPx() },
    content: LazyListScope.() -> Unit
) {
    val column_state = rememberLazyListState()

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (show_up_indicator) {
            ScrollabilityIndicator(true, column_state, scroll_amount)
        }

        LazyColumn(
            state = column_state,
            content = content,
            modifier = Modifier.weight(1f)
        )

        ScrollabilityIndicator(false, column_state, scroll_amount)
    }
}

@Composable
private fun ColumnScope.ScrollabilityIndicator(up: Boolean, list_state: LazyListState, scroll_amount: Float? = null) {
    val coroutine_scope = rememberCoroutineScope()
    val show = if (up) list_state.canScrollBackward else list_state.canScrollForward
    val icon = if (up) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown

    Box(
        Modifier
            .height(24.dp)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                if (scroll_amount != null) {
                    coroutine_scope.launch {
                        list_state.animateScrollBy(if (up) -scroll_amount else scroll_amount)
                    }
                }
            }
    ) {
        this@ScrollabilityIndicator.AnimatedVisibility(show) {
            Icon(icon, null)
        }
    }
}
