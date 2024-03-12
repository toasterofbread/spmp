package com.toasterofbread.spmp.ui.layout.contentbar

import LocalPlayerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.runtime.*
import com.toasterofbread.composekit.utils.composable.ShapedIconButton
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.toasterofbread.spmp.ui.layout.contentbar.element.*

@Composable
internal fun CustomContentBarCopyPasteButtons(
    element_data: List<ContentBarElementData>,
    onPaste: (List<ContentBarElementData>) -> Unit
) {
    val player: PlayerState = LocalPlayerState.current
    val clipboard: ClipboardManager = LocalClipboardManager.current

    val colours: IconButtonColors = IconButtonDefaults.iconButtonColors(
        containerColor = player.theme.background,
        contentColor = player.theme.on_background
    )

    ShapedIconButton(
        { clipboard.setText(AnnotatedString(Json.encodeToString(element_data))) },
        colours
    ) {
        Icon(Icons.Default.ContentCopy, null)
    }

    ShapedIconButton(
        {
            try {
                onPaste(Json.decodeFromString(clipboard.getText()!!.text))
            }
            catch (e: Throwable) {
                if (player.context.canSendNotifications()) {
                    player.context.sendNotification(e)
                }
            }
        },
        colours
    ) {
        Icon(Icons.Default.ContentPaste, null)
    }
}