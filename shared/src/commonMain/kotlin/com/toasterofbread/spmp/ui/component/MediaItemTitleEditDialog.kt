package com.toasterofbread.spmp.ui.component

import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.platform.composable.PlatformAlertDialog
import com.toasterofbread.spmp.resources.getString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaItemTitleEditDialog(item: MediaItem, modifier: Modifier = Modifier, close: () -> Unit) {
    var edited_title: String by remember { mutableStateOf(item.title ?: "") }
    PlatformAlertDialog(
        close,
        { Button({
            item.editRegistry {
                it.title = edited_title
            }
            close()
        }) {
            Text(getString("action_confirm_action"))
        } },
        modifier,
        dismissButton = {
            Button(close) {
                Text(getString("action_cancel"))
            }
        },
        title = {
            Text(getString("edit_\$x_title_dialog_title").replace("\$x", item.type.getReadable(false)))
        },
        text = {
            TextField(
                edited_title,
                { edited_title = it }
            )
        }
    )
}
