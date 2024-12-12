package com.toasterofbread.spmp.ui.component

import LocalPlayerState
import androidx.compose.material3.AlertDialog
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
import dev.toastbits.composekit.util.platform.launchSingle
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.appTextField
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.action_confirm_action
import spmp.shared.generated.resources.action_cancel
import spmp.shared.generated.resources.`edit_$x_title_dialog_title`

@Composable
fun MediaItemTitleEditDialog(item: MediaItem, modifier: Modifier = Modifier, close: () -> Unit) {
    val player = LocalPlayerState.current
    val coroutine_scope = rememberCoroutineScope()

    var edited_title: String by remember { mutableStateOf(item.getActiveTitle(player.database) ?: "") }

    AlertDialog(
        close,
        {
            Button({
                coroutine_scope.launchSingle {
                    item.setActiveTitle(edited_title, player.context)
                    close()
                }
            }) {
                Text(stringResource(Res.string.action_confirm_action))
            }
        },
        modifier,
        dismissButton = {
            Button(close) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
        title = {
            Text(stringResource(Res.string.`edit_$x_title_dialog_title`).replace("\$x", stringResource(item.getType().getReadable())))
        },
        text = {
            TextField(
                edited_title,
                { edited_title = it },
                Modifier.appTextField()
            )
        }
    )
}
