package com.toasterofbread.spmp.ui.layout.apppage.settingspage

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.toastbits.composekit.settings.ui.item.StringSetSettingsItem
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.appTextField
import dev.toastbits.composekit.platform.PreferencesProperty

fun AppStringSetItem(
    state: PreferencesProperty<Set<String>>,
    add_dialog_title: String,
    single_line_content: Boolean = true,
    max_height: Dp = 300.dp,
    itemToText: @Composable (String) -> String = { it },
    textToItem: (String) -> String = { it }
): StringSetSettingsItem =
    StringSetSettingsItem(
        state = state,
        add_dialog_title = add_dialog_title,
        msg_item_already_added = getString("settings_string_set_item_already_added"),
        msg_set_empty = getString("settings_string_set_item_empty"),
        single_line_content = single_line_content,
        max_height = max_height,
        itemToText = itemToText,
        textToItem = textToItem,
        getFieldModifier = { Modifier.appTextField() }
    )
