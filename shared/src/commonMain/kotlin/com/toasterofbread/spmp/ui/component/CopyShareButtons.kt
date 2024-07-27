package com.toasterofbread.spmp.ui.component

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.platform.AppContext
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.notif_copied_x_to_clipboard
import spmp.shared.generated.resources.notif_copied_to_clipboard

@Composable
fun AppContext.CopyShareButtons(name: String? = null, getText: () -> String) {
    val clipboard: ClipboardManager = LocalClipboardManager.current
    val copied_text: String = stringResource(Res.string.notif_copied_to_clipboard)
    val copied_x_text: String = stringResource(Res.string.notif_copied_x_to_clipboard)

    IconButton({
        clipboard.setText(AnnotatedString(getText()))

        if (name != null) {
            sendToast(copied_x_text.replace("\$x", name))
        }
        else {
            sendToast(copied_text)
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