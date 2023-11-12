package com.toasterofbread.spmp.ui.layout.apppage.settingspage

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.toasterofbread.composekit.settings.ui.item.BasicSettingsValueState
import com.toasterofbread.composekit.settings.ui.item.SettingsStringSetItem
import com.toasterofbread.spmp.resources.getString

fun AppStringSetItem(
    state: BasicSettingsValueState<Set<String>>,
    title: String?,
    subtitle: String?,
    add_dialog_title: String,
    single_line_content: Boolean = true,
    max_height: Dp = 300.dp,
    itemToText: @Composable (String) -> String = { it },
    textToItem: (String) -> String = { it }
): SettingsStringSetItem =
    SettingsStringSetItem(
        state = state,
        title = title,
        subtitle = subtitle,
        add_dialog_title = add_dialog_title,
        msg_item_already_added = getString("settings_string_set_item_already_added"),
        msg_set_empty = getString("settings_string_set_item_empty"),
        single_line_content = single_line_content,
        max_height = max_height,
        itemToText = itemToText,
        textToItem = textToItem,
    )
