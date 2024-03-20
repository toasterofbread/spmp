package com.toasterofbread.spmp.ui.shortcut

import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import com.toasterofbread.spmp.ui.shortcut.trigger.ShortcutTrigger
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.composekit.utils.composable.LargeDropdownMenu

@Composable
fun ShortcutSelector(
    shortcut: ShortcutTrigger? = null,
    modifier: Modifier = Modifier,
    onModification: (ShortcutTrigger?) -> Unit
) {
    var selecting_type: Boolean by remember { mutableStateOf(false) }

    LargeDropdownMenu(
        selecting_type,
        { selecting_type = false },
        ShortcutTrigger.Type.entries.size + 1,
        shortcut?.getType()?.ordinal?.plus(1) ?: 0,
        itemContent = {
            val type: ShortcutTrigger.Type? = if (it == 0) null else ShortcutTrigger.Type.entries[it - 1]
            type.Preview()
        },
        onSelected = { type ->
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
        shortcut?.getType().Preview()
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
        if (type == null) {
            Text(getString("shortcut_trigger_none"))
        }
        else {
            Icon(type.getIcon(), null)
            Text(type.getName())
        }
    }
}
