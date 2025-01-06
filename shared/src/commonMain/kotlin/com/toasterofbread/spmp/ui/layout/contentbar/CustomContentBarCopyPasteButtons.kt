package com.toasterofbread.spmp.ui.layout.contentbar

import LocalPlayerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.Modifier
import androidx.compose.runtime.*
import dev.toastbits.composekit.components.utils.composable.ShapedIconButton
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.toasterofbread.spmp.ui.layout.contentbar.element.*

@Composable
internal fun CustomContentBarCopyPasteButtons(
    elements: List<ContentBarElement>,
    item_modifier: Modifier = Modifier,
    onPaste: (List<ContentBarElement>) -> Unit
) {
    val player: PlayerState = LocalPlayerState.current
    val clipboard: ClipboardManager = LocalClipboardManager.current

    val colours: IconButtonColors = IconButtonDefaults.iconButtonColors(
        containerColor = player.theme.background,
        contentColor = player.theme.onBackground
    )

    ShapedIconButton(
        { clipboard.setText(AnnotatedString(Json.encodeToString(elements))) },
        colours,
        item_modifier
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
        colours,
        item_modifier
    ) {
        Icon(Icons.Default.ContentPaste, null)
    }
}
