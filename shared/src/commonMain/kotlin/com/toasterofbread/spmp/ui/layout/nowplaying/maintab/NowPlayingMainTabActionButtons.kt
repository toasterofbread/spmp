package com.toasterofbread.spmp.ui.layout.nowplaying.maintab

import LocalPlayerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.toasterofbread.composekit.utils.modifier.bounceOnClick
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.ui.component.LikeDislikeButton
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState
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
        if (auth_state != null) {
            LikeDislikeButton(
                song,
                auth_state,
                modifier.minimumInteractiveComponentSize(),
                getColour = { colour }
            )
        }
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
        if (!player.context.canOpenUrl()) {
            return
        }
        
        IconButton(
            {
                if (song == null) {
                    return@IconButton
                }
                player.context.openUrl(song.getURL(player.context))
            },
            modifier.bounceOnClick().appHover(true)
        ) {
            Icon(Icons.Rounded.OpenInNew, null)
        }
    }
    
    @Composable
    fun DownloadButton(song: Song?, modifier: Modifier = Modifier) {
        val player: PlayerState = LocalPlayerState.current
        
        IconButton(
            {
                if (song == null) {
                    return@IconButton
                }
                player.onSongDownloadRequested(song, null)
            },
            modifier.bounceOnClick().appHover(true)
        ) {
            Icon(Icons.Rounded.Download, null)
        }
    }
}
