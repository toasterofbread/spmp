package com.toasterofbread.spmp.ui.layout.apppage.settingspage

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.appTextField
import dev.toastbits.composekit.settingsitem.domain.PlatformSettingsProperty
import dev.toastbits.composekit.settingsitem.presentation.ui.component.item.StringSetSettingsItem
import org.jetbrains.compose.resources.StringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.settings_string_set_item_already_added
import spmp.shared.generated.resources.settings_string_set_item_empty

fun AppStringSetItem(
    state: PlatformSettingsProperty<Set<String>>,
    add_dialog_title: StringResource,
    single_line_content: Boolean = true,
    height: Dp = 300.dp,
    itemToText: @Composable (String) -> String = { it },
    textToItem: (String) -> String = { it }
): StringSetSettingsItem =
    StringSetSettingsItem(
        state = state,
        add_dialog_title = add_dialog_title,
        msg_item_already_added = Res.string.settings_string_set_item_already_added,
        msg_set_empty = Res.string.settings_string_set_item_empty,
        single_line_content = single_line_content,
        height = height,
        itemToText = itemToText,
        textToItem = textToItem,
        getFieldModifier = { Modifier.appTextField() }
    )
