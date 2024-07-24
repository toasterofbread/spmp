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
import dev.toastbits.composekit.platform.vibrateShort
import dev.toastbits.composekit.utils.composable.PlatformClickableIconButton
import dev.toastbits.composekit.utils.modifier.bounceOnClick
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.ui.component.LikeDislikeButton
import com.toasterofbread.spmp.model.state.OldPlayerStateImpl
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

        val player: OldPlayerStateImpl = LocalPlayerState.current
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
        val player: OldPlayerStateImpl = LocalPlayerState.current

        IconButton(
            {
                if (song != null) {
                    player.withPlayer {
                        undoableAction {
                            startRadioAtIndex(current_song_index + 1, song, current_song_index, skip_first = true)
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
        val player: OldPlayerStateImpl = LocalPlayerState.current

        IconButton(
            {
                player.withPlayer {
                    undoableAction {
                        shuffleQueue(start = current_song_index + 1)
                    }
                }
            },
            modifier.bounceOnClick().appHover(true)
        ) {
            Icon(Icons.Rounded.Shuffle, null, tint = colour)
        }
    }

    @Composable
    fun OpenExternalButton(song: Song?, modifier: Modifier = Modifier) {
        val player: OldPlayerStateImpl = LocalPlayerState.current
        if (!(player.context.canShare() || player.context.canOpenUrl())) {
            return
        }

        val clipboard: ClipboardManager = LocalClipboardManager.current
        val notif_copied_to_clipboard: String = stringResource(Res.string.notif_copied_to_clipboard)
        val song_url: String? = song?.observeUrl()

        PlatformClickableIconButton(
            onClick = {
                val url: String = song_url ?: return@PlatformClickableIconButton

                if (player.context.canShare()) {
                    player.context.shareText(url, song.getActiveTitle(player.database))
                }
                else if (player.context.canOpenUrl()) {
                    player.context.openUrl(url)
                }
            },
            onAltClick = {
                song_url?.also {
                    clipboard.setText(AnnotatedString((it)))
                    player.context.vibrateShort()
                    player.context.sendToast(notif_copied_to_clipboard)
                }
            },
            modifier = modifier.bounceOnClick().appHover(true)
        ) {
            Icon(
                if (player.context.canShare()) Icons.Rounded.Share
                else Icons.Rounded.OpenInNew,
                null
            )
        }
    }

    @Composable
    fun DownloadButton(song: Song?, modifier: Modifier = Modifier) {
        val player: OldPlayerStateImpl = LocalPlayerState.current

        PlatformClickableIconButton(
            onClick = {
                song?.also {
                    player.onSongDownloadRequested(it)
                }
            },
            onAltClick = {
                song?.also {
                    player.onSongDownloadRequested(it, always_show_options = true)
                    player.context.vibrateShort()
                }
            },
            modifier = modifier.bounceOnClick().appHover(true)
        ) {
            Icon(Icons.Rounded.Download, null)
        }
    }
}
