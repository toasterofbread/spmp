package com.toasterofbread.spmp.ui.component.longpressmenu

import LocalPlayerState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.toInfoString
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.utils.composable.WidthShrinkText
import com.toasterofbread.utils.common.isDebugBuild

const val MENU_ITEM_SPACING: Int = 20

@Composable
internal fun ColumnScope.LongPressMenuInfoActions(
    data: LongPressMenuData,
    spacing: Dp,
    getAccentColour: () -> Color,
    onAction: () -> Unit,
) {
    val player = LocalPlayerState.current

    Column(Modifier.fillMaxHeight().weight(1f), verticalArrangement = Arrangement.spacedBy(spacing)) {
        data.infoContent?.invoke(this, getAccentColour)
    }

    // Share
    if (player.context.canShare()) {
        LongPressMenuActionProvider.ActionButton(
            Icons.Filled.Share,
            getString("lpm_action_share"),
            getAccentColour,
            onClick = {
                player.context.shareText(
                    data.item.getURL(player.context),
                    if (data.item is Song) data.item.getActiveTitle(player.database) else null
                )
            },
            onAction = onAction
        )
    }

    // Open
    if (player.context.canOpenUrl()) {
        LongPressMenuActionProvider.ActionButton(
            Icons.Filled.OpenWith,
            getString("lpm_action_open_external"),
            getAccentColour,
            onClick = {
                player.context.openUrl(data.item.getURL(player.context))
            },
            onAction = onAction
        )
    }

    if (isDebugBuild()) {
        Row(
            Modifier.clickable {
                val item_data = data.item.getEmptyData()
                item_data.populateData(item_data, player.database)
                println(item_data.toInfoString())
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
    val player = LocalPlayerState.current

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
        data.item.Hidden.set(true, player.context.database)
    }, onAction = onAction)
}
