package com.toasterofbread.spmp.ui.layout.contentbar.layoutslot

import LocalPlayerState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.platform.*
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.contentbar.ContentBar
import dev.toastbits.composekit.components.platform.composable.platformClickable
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.layout_editor_preview_option_show_bar_content
import spmp.shared.generated.resources.layout_editor_preview_option_hide_player
import spmp.shared.generated.resources.layout_editor_preview_option_portrait_mode

@Composable
fun LayoutSlotEditorPreviewOptions(modifier: Modifier = Modifier) {
    val player: PlayerState = LocalPlayerState.current
    val form_factor: FormFactor by FormFactor.observe()

    DisposableEffect(Unit) {
        onDispose {
            FormFactor.setOverride(null)
        }
    }

    Column(modifier) {
        SwitchButton(
            checked = ContentBar.disable_bar_selection,
            onCheckedChange = { checked ->
                ContentBar.disable_bar_selection = checked
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(Res.string.layout_editor_preview_option_show_bar_content))
        }

        SwitchButton(
            checked = player.hide_player,
            onCheckedChange = { checked ->
                player.hide_player = checked
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(Res.string.layout_editor_preview_option_hide_player))
        }

        SwitchButton(
            checked = form_factor == FormFactor.PORTRAIT,
            onCheckedChange = { checked ->
                FormFactor.setOverride(
                    if (checked) FormFactor.PORTRAIT
                    else FormFactor.LANDSCAPE
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(Res.string.layout_editor_preview_option_portrait_mode))
        }
    }
}

@Composable
private fun SwitchButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    text: @Composable () -> Unit
) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        text()

        Spacer(Modifier.width(20.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.platformClickable(
                onClick = { onCheckedChange(!checked) }
            )
        )
    }
}
