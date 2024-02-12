package com.toasterofbread.spmp.ui.layout.nowplaying.maintab

import LocalPlayerState
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
import com.toasterofbread.composekit.platform.vibrateShort
import com.toasterofbread.composekit.utils.composable.PlatformClickableIconButton
import com.toasterofbread.composekit.utils.modifier.bounceOnClick
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.LikeDislikeButton
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.theme.appHover
import com.toasterofbread.spmp.youtubeapi.YoutubeApi

internal object NowPlayingMainTabActionButtons {
    @Composable
    fun LikeDislikeButton(song: Song?, modifier: Modifier = Modifier, colour: Color = LocalContentColor.current) {
        if (song == null) {
            return
        }
        
        val player: PlayerState = LocalPlayerState.current
        val auth_state: YoutubeApi.UserAuthState? = player.context.ytapi.user_auth_state

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
        val player: PlayerState = LocalPlayerState.current
        
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
        val player: PlayerState = LocalPlayerState.current

        if (!(player.context.canShare() || player.context.canOpenUrl())) {
            return
        }

        val clipboard: ClipboardManager = LocalClipboardManager.current
        
        PlatformClickableIconButton(
            onClick = {
                val url: String = song?.getURL(player.context) ?: return@PlatformClickableIconButton

                if (player.context.canShare()) {
                    player.context.shareText(url, song.getActiveTitle(player.database))
                }
                else if (player.context.canOpenUrl()) {
                    player.context.openUrl(url)
                }
            },
            onAltClick = {
                song?.getURL(player.context)?.also {
                    clipboard.setText(AnnotatedString((it)))
                    player.context.vibrateShort()
                    player.context.sendToast(getString("notif_copied_to_clipboard"))
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
        val player: PlayerState = LocalPlayerState.current
        
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
