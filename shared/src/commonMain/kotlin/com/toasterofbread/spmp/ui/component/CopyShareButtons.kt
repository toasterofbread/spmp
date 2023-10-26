package com.toasterofbread.spmp.ui.component

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.resources.getString

@Composable
fun AppContext.CopyShareButtons(name: String? = null, getText: () -> String) {
    val clipboard = LocalClipboardManager.current
    IconButton({
        clipboard.setText(AnnotatedString(getText()))

        if (name != null) {
            sendToast(getString("notif_copied_x_to_clipboard").replace("\$x", name))
        }
        else {
            sendToast(getString("notif_copied_to_clipboard"))
        }
    }) {
        Icon(Icons.Default.ContentCopy, null, Modifier.size(20.dp))
    }

    if (canShare()) {
        IconButton({
            shareText(getText())
        }) {
            Icon(Icons.Default.Share, null, Modifier.size(20.dp))
        }
    }
}