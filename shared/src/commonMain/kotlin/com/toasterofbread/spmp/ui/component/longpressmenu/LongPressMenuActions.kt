package com.toasterofbread.spmp.ui.component.longpressmenu

import SpMp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.theme.Theme

const val MENU_ITEM_SPACING: Int = 20

@Composable
internal fun LongPressMenuActions(data: LongPressMenuData, accent_colour: Color, onAction: () -> Unit) {
    val accent_colour_provider = remember (accent_colour) { { accent_colour } }

    // Data-provided actions
    data.Actions(
        LongPressMenuActionProvider(
            Theme.current.on_background_provider,
            accent_colour_provider,
            Theme.current.background_provider,
            onAction
        ),
        MENU_ITEM_SPACING.dp
    )

    data.item.url?.also { url ->
        // Share
        if (SpMp.context.canShare()) {
            LongPressMenuActionProvider.ActionButton(Icons.Filled.Share, getString("lpm_action_share"), accent_colour_provider, onClick = {
                SpMp.context.shareText(url, if (data.item is Song) data.item.title else null)
            }, onAction = onAction)
        }

        // Open
        if (SpMp.context.canOpenUrl()) {
            LongPressMenuActionProvider.ActionButton(Icons.Filled.OpenWith, getString("lpm_action_open_external"), accent_colour_provider, onClick = {
                SpMp.context.openUrl(url)
            }, onAction = onAction)
        }
    }
}
