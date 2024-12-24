package com.toasterofbread.spmp.widget.component.spmp

import LocalPlayerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.padding
import androidx.glance.layout.size
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.widget.SpMpWidget
import dev.toastbits.composekit.util.composable.getValue
import dev.toastbits.ytmkt.model.external.ThumbnailProvider

@Composable
internal fun SpMpWidget<*, *>.GlanceSongPreview(
    song: Song,
    modifier: GlanceModifier = GlanceModifier
) {
    val player: PlayerState = LocalPlayerState.current

    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val title: String? by song.observeActiveTitle()
        val artist: Artist? = song.Artists.observe(player.database).value?.firstOrNull()
        val artist_title: String? by artist?.observeActiveTitle()

        val image_size: Dp = 50.dp
        val spacing: Dp = 10.dp

        GlanceSongThumbnail(
            song,
            title,
            ThumbnailProvider.Quality.LOW,
            GlanceModifier
                .size(image_size)
                .padding(end = spacing),
            scale_to_size = 100
        )

        Column {
            title?.also { text ->
                WidgetText(
                    text,
                    font_size = 17.sp
                )
            }

            artist_title?.also { text ->
                WidgetText(
                    text,
                    font_size = 12.sp,
                    alpha = 0.75f
                )
            }
        }
    }
}
