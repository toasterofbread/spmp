package com.toasterofbread.spmp.model.appaction.shortcut

import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import com.toasterofbread.spmp.ui.component.shortcut.trigger.ShortcutTrigger
import dev.toastbits.composekit.components.utils.composable.LargeDropdownMenu
import com.toasterofbread.spmp.ui.component.shortcut.trigger.getName

@Composable
fun ShortcutTriggerSelector(
    title: String,
    trigger: ShortcutTrigger? = null,
    modifier: Modifier = Modifier,
    onModification: (ShortcutTrigger?) -> Unit
) {
    var selecting_type: Boolean by remember { mutableStateOf(false) }

    LargeDropdownMenu(
        title = title,
        isOpen = selecting_type,
        onDismissRequest = { selecting_type = false },
        items = (0 until ShortcutTrigger.Type.entries.size + 1).toList(),
        selectedItem = trigger?.getType()?.ordinal?.plus(1) ?: 0,
        itemContent = {
            val type: ShortcutTrigger.Type? = if (it == 0) null else ShortcutTrigger.Type.entries[it - 1]
            type.Preview()
        },
        onSelected = { _, type ->
            if (type == 0) {
                onModification(null)
            }
            else {
                onModification(ShortcutTrigger.Type.entries[type - 1].create())
            }
            selecting_type = false
        }
    )

    Button(
        { selecting_type = true },
        modifier,
    ) {
        trigger?.getType().Preview()
    }
}

@Composable
private fun ShortcutTrigger.Type?.Preview(modifier: Modifier = Modifier) {
    val type: ShortcutTrigger.Type? = this

    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (type != null) {
            Icon(type.getIcon(), null)
        }

        Text(type.getName())
    }
}
