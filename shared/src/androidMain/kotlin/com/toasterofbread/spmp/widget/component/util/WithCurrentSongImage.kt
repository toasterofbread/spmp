package com.toasterofbread.spmp.widget.component.util

import LocalPlayerState
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.component.Thumbnail
import com.toasterofbread.spmp.widget.SpMpWidget
import dev.toastbits.composekit.platform.composable.theme.LocalApplicationTheme
import dev.toastbits.composekit.settings.ui.ThemeValues
import dev.toastbits.composekit.settings.ui.ThemeValuesData
import dev.toastbits.composekit.utils.common.getThemeColour
import dev.toastbits.ytmkt.model.external.ThumbnailProvider

@Composable
fun SpMpWidget<*, *>.WithCurrentSongImage(
    content: @Composable (Song?, Bitmap?) -> Unit
) {
    val player: PlayerState = LocalPlayerState.current
    val song: Song? = player.status.m_song

    if (song != null) {
        song.Thumbnail(
            ThumbnailProvider.Quality.HIGH,
            contentOverride = {
                val theme: ThemeValues = LocalApplicationTheme.current
                val song_theme: Color? by song.ThemeColour.observe(player.database)
                val image_accent: Color? = it?.getThemeColour()
                val current_accent: Color = song_theme ?: image_accent ?: theme.accent

                CompositionLocalProvider(
                    LocalApplicationTheme provides ThemeValuesData.of(theme).copy(accent = current_accent)
                ) {
                    content(song, it?.asAndroidBitmap())
                }
            }
        )
    }
    else {
        content(null, null)
    }
}
