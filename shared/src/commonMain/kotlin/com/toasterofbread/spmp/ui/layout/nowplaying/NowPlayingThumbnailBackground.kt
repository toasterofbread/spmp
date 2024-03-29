package com.toasterofbread.spmp.ui.layout.nowplaying

import LocalNowPlayingExpansion
import LocalPlayerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import dev.toastbits.ytmkt.model.external.ThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.settings.category.ThemeSettings
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.component.Thumbnail

@Composable
fun NowPlayingThumbnailBackground(modifier: Modifier = Modifier) {
    val player: PlayerState = LocalPlayerState.current
    val expansion: NowPlayingExpansionState = LocalNowPlayingExpansion.current
    
    val default_background_image_opacity: Float by ThemeSettings.Key.NOWPLAYING_DEFAULT_BACKGROUND_IMAGE_OPACITY.rememberMutableState()
    val current_song: Song? by player.status.song_state

    current_song?.also { song ->
        val background_image_opacity: Float? by song.BackgroundImageOpacity.observe(player.database)
        val opacity: Float = background_image_opacity ?: default_background_image_opacity
        if (opacity <= 0f) {
            return@also
        }

        song.Thumbnail(
            ThumbnailProvider.Quality.HIGH,
            modifier
                .blur(5.dp)
                .alpha(opacity)
                .graphicsLayer {
                    alpha = expansion.get().coerceIn(0f, 1f)
                }
        )
    }    
}
