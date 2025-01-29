package com.toasterofbread.spmp.ui.layout.nowplaying.maintab

import LocalPlayerState
import dev.toastbits.ytmkt.model.ApiAuthenticationState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.toasterofbread.spmp.model.mediaitem.observeUrl
import dev.toastbits.composekit.context.vibrateShort
import dev.toastbits.composekit.components.utils.composable.PlatformClickableIconButton
import dev.toastbits.composekit.components.utils.modifier.bounceOnClick
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.ui.component.LikeDislikeButton
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.theme.appHover
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.notif_copied_to_clipboard

internal object NowPlayingMainTabActionButtons {
    @Composable
    fun LikeDislikeButton(song: Song?, modifier: Modifier = Modifier, colour: Color = LocalContentColor.current) {
        if (song == null) {
            return
        }

        val player: PlayerState = LocalPlayerState.current
        val auth_state: ApiAuthenticationState? = player.context.ytapi.user_auth_state

        LikeDislikeButton(
            song,
            auth_state,
            modifier,
            getColour = { colour }
        )
    }

    @Composable
    fun RadioButton(song: Song?, modifier: Modifier = Modifier, colour: Color = LocalContentColor.current) {
        val player: PlayerState = LocalPlayerState.current

        IconButton(
            {
                if (song != null) {
                    player.withPlayer {
                        undoableAction {
                            startRadioAtIndex(current_item_index + 1, song, current_item_index, skip_first = true)
                        }
                    }
                    player.expansion.scrollTo(2.coerceIn(player.expansion.getPageRange()))
                }
            },
            modifier.bounceOnClick().appHover(true)
        ) {
            Icon(Icons.Rounded.Radio, null, tint = colour)
        }
    }

    @Composable
    fun ShuffleButton(modifier: Modifier = Modifier, colour: Color = LocalContentColor.current) {
        val player: PlayerState = LocalPlayerState.current

        IconButton(
            {
                player.withPlayer {
                    undoableAction {
                        shuffleQueue(start = current_item_index + 1)
                    }
                }
            },
            modifier.bounceOnClick().appHover(true)
        ) {
            Icon(Icons.Rounded.Shuffle, null, tint = colour)
        }
    }
}
