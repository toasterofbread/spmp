package com.toasterofbread.spmp.ui.component

import LocalPlayerState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.platform.composable.PlatformAlertDialog
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.utils.common.launchSingle

@Composable
fun MediaItemTitleEditDialog(item: MediaItem, modifier: Modifier = Modifier, close: () -> Unit) {
    val player = LocalPlayerState.current
    val coroutine_scope = rememberCoroutineScope()

    var edited_title: String by remember { mutableStateOf(item.getActiveTitle(player.database) ?: "") }

    PlatformAlertDialog(
        close,
        {
            Button({
                coroutine_scope.launchSingle {
                    item.setActiveTitle(edited_title, player.context)
                    close()
                }
            }) {
                Text(getString("action_confirm_action"))
            }
        },
        modifier,
        dismissButton = {
            Button(close) {
                Text(getString("action_cancel"))
            }
        },
        title = {
            Text(getString("edit_\$x_title_dialog_title").replace("\$x", item.getType().getReadable(false)))
        },
        text = {
            TextField(
                edited_title,
                { edited_title = it }
            )
        }
    )
}
