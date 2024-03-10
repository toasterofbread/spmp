package com.toasterofbread.spmp.ui.layout.contentbar

import LocalPlayerState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.platform.*
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.contentbar.ContentBar
import com.toasterofbread.composekit.platform.composable.platformClickable

@Composable
fun LayoutSlotEditorPreviewOptionsList(item_modifier: Modifier = Modifier) {
    val player: PlayerState = LocalPlayerState.current

    DisposableEffect(Unit) {
        onDispose {
            FormFactor.form_factor_override = null
        }
    }

    SwitchButton(
        checked = ContentBar.disable_bar_selection,
        onCheckedChange = { checked ->
            ContentBar.disable_bar_selection = checked
        },
        modifier = item_modifier
    ) {
        Text(getString("layout_editor_preview_option_show_bar_content"))
    }

    SwitchButton(
        checked = player.hide_player,
        onCheckedChange = { checked ->
            player.hide_player = checked
        },
        modifier = item_modifier
    ) {
        Text(getString("layout_editor_preview_option_hide_player"))
    }

    SwitchButton(
        checked = player.form_factor == FormFactor.PORTRAIT,
        onCheckedChange = { checked ->
            FormFactor.form_factor_override =
                if (checked) FormFactor.PORTRAIT
                else FormFactor.LANDSCAPE
        },
        modifier = item_modifier
    ) {
        Text(getString("layout_editor_preview_option_portrait_mode"))
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
