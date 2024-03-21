package com.toasterofbread.spmp.model.appaction.shortcut

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toasterofbread.composekit.utils.composable.StickyHeightColumn
import com.toasterofbread.composekit.platform.composable.ScrollBarLazyRow
import com.toasterofbread.spmp.model.appaction.AppAction
import com.toasterofbread.spmp.model.settings.category.ShortcutSettings
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.shortcut.ShortcutPreview
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Composable
fun ShortcutsEditor(modifier: Modifier = Modifier) {
    var shortcuts_data: String by ShortcutSettings.Key.CONFIGURED_SHORTCUTS.rememberMutableState()
    val shortcuts: List<Shortcut> = remember(shortcuts_data) { Json.decodeFromString(shortcuts_data) }

    StickyHeightColumn(
        modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.Add, null)

            ScrollBarLazyRow(
                Modifier.fillMaxWidth().weight(1f).height(60.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(AppAction.Type.entries) { action_type ->
                    Button({
                        val new_shortcut: Shortcut = Shortcut(trigger = null, action = action_type.createAction())
                        shortcuts_data = Json.encodeToString(shortcuts.plus(new_shortcut))
                    }) {
                        Icon(action_type.getIcon(), null, Modifier.padding(end = 10.dp))
                        Text(action_type.getName())
                    }
                }
            }
        }

        for ((index, shortcut) in shortcuts.withIndex()) {
            ShortcutPreview(
                shortcut,
                onModification = { new_shortcut ->
                    val new_shortcuts: MutableList<Shortcut> = shortcuts.toMutableList()
                    if (new_shortcut == null) {
                        new_shortcuts.removeAt(index)
                    }
                    else {
                        new_shortcuts[index] = new_shortcut
                    }
                    shortcuts_data = Json.encodeToString(new_shortcuts)
                }
            )
        }
    }
}
