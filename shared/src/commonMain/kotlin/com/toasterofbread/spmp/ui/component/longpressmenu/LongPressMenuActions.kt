package com.toasterofbread.spmp.ui.component.longpressmenu

import SpMp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.model.mediaitem.setHidden
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.utils.composable.WidthShrinkText
import com.toasterofbread.utils.isDebugBuild

const val MENU_ITEM_SPACING: Int = 20

@Composable
internal fun ColumnScope.LongPressMenuInfoActions(data: LongPressMenuData, getAccentColour: () -> Color, onAction: () -> Unit) {
    data.infoContent?.invoke(this, getAccentColour)

    // Share
    if (SpMp.context.canShare()) {
        LongPressMenuActionProvider.ActionButton(Icons.Filled.Share, getString("lpm_action_share"), getAccentColour, onClick = {
            SpMp.context.shareText(data.item.getURL(), if (data.item is Song) data.item.title else null)
        }, onAction = onAction)
    }

    // Open
    if (SpMp.context.canOpenUrl()) {
        LongPressMenuActionProvider.ActionButton(Icons.Filled.OpenWith, getString("lpm_action_open_external"), getAccentColour, onClick = {
            SpMp.context.openUrl(data.item.getURL())
        }, onAction = onAction)
    }

    if (isDebugBuild()) {
        Row(
            Modifier.clickable {
                println(data.item)
            },
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Print, null, tint = getAccentColour())
            WidthShrinkText(getString("lpm_action_print_info"), fontSize = 15.sp)
        }
    }
}

@Composable
internal fun ColumnScope.LongPressMenuActions(data: LongPressMenuData, getAccentColour: () -> Color, onAction: () -> Unit) {
    // Data-provided actions
    data.Actions(
        LongPressMenuActionProvider(
            Theme.on_background_provider,
            getAccentColour,
            Theme.background_provider,
            onAction
        ),
        MENU_ITEM_SPACING.dp
    )

    // Hide
    LongPressMenuActionProvider.ActionButton(Icons.Filled.VisibilityOff, getString("lpm_action_hide"), getAccentColour, onClick = {
        data.item.setHidden(true)
    }, onAction = onAction)
}
