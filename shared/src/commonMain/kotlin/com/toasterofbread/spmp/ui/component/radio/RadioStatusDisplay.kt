package com.toasterofbread.spmp.ui.component.radio

import LocalPlayerState
import SpMp.isDebugBuild
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.toastbits.composekit.util.getContrasted
import dev.toastbits.composekit.util.composable.AlignableCrossfade
import dev.toastbits.composekit.components.utils.composable.ShapedIconButton
import dev.toastbits.composekit.components.utils.composable.SubtleLoadingIndicator
import dev.toastbits.composekit.components.utils.modifier.bounceOnClick
import com.toasterofbread.spmp.model.radio.RadioInstance
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.component.ErrorInfoDisplay
import com.toasterofbread.spmp.ui.theme.appHover

@Composable
fun RadioInstance.StatusDisplay(
    modifier: Modifier,
    expanded_modifier: Modifier = Modifier,
    disable_parent_scroll: Boolean = false
) {
    val player: PlayerState = LocalPlayerState.current

    AlignableCrossfade(
        Triple(is_loading, load_error, isContinuationAvailable()),
        modifier,
        contentAlignment = Alignment.Center
    ) {
        val (loading: Boolean, error: Throwable?, continuation_available: Boolean) = it

        if (loading) {
            SubtleLoadingIndicator()
        }
        else if (error != null) {
            ErrorInfoDisplay(
                error,
                isDebugBuild(),
                expanded_content_modifier = expanded_modifier,
                disable_parent_scroll = disable_parent_scroll,
                onRetry = {
                    player.controller?.service_player?.radio?.instance?.loadContinuation()
                },
                onDismiss = {
                    player.controller?.service_player?.radio?.instance?.dismissLoadError()
                }
            )
        }
        else if (continuation_available) {
            ShapedIconButton(
                {
                    player.controller?.service_player?.radio?.instance?.loadContinuation()
                },
                modifier = Modifier
                    .width(80.dp)
                    .bounceOnClick()
                    .appHover(true),
                colours = IconButtonDefaults.iconButtonColors(
                    containerColor = LocalContentColor.current.copy(alpha = 0.3f),
                    contentColor = LocalContentColor.current.getContrasted()
                ),
                indication = null
            ) {
                Icon(Icons.Default.ArrowDownward, null)
            }
        }
    }
}
