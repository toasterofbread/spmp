package com.toasterofbread.spmp.widget.impl

import LocalPlayerState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.wrapContentHeight
import androidx.glance.layout.wrapContentSize
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.widget.SpMpWidget
import com.toasterofbread.spmp.widget.action.SongImageWidgetClickAction
import com.toasterofbread.spmp.widget.component.GlanceLargePlayPauseButton
import com.toasterofbread.spmp.widget.component.GlanceSongThumbnail
import com.toasterofbread.spmp.widget.configuration.type.SongImageWidgetConfig
import com.toasterofbread.spmp.widget.modifier.padding
import dev.toastbits.composekit.platform.composable.theme.LocalApplicationTheme
import dev.toastbits.composekit.settings.ui.ThemeValues
import dev.toastbits.composekit.settings.ui.ThemeValuesData
import dev.toastbits.composekit.utils.common.getThemeColour
import dev.toastbits.composekit.utils.common.getValue
import dev.toastbits.ytmkt.model.external.ThumbnailProvider

internal class SongImageWidget: SpMpWidget<SongImageWidgetClickAction, SongImageWidgetConfig>(custom_background = true) {
    override fun executeTypeAction(action: SongImageWidgetClickAction) =
        when (action) {
            else -> throw IllegalStateException(action.toString())
        }

    @Composable
    override fun hasContent(): Boolean =
        LocalPlayerState.current.status.m_song != null

    @Composable
    override fun Content(modifier: GlanceModifier, content_padding: PaddingValues) {
        val player: PlayerState = LocalPlayerState.current
        val theme: ThemeValues = LocalApplicationTheme.current
        val song: Song? = player.status.m_song

        if (song != null) {
            val song_theme: Color? by song.ThemeColour.observe(player.database)

            Column(
                modifier.fillMaxSize(),
                verticalAlignment = Alignment.Bottom
            ) {
                var image_accent: Color? by remember { mutableStateOf(null) }
                val image_showing: Boolean =
                    GlanceSongThumbnail(
                        song,
                        null,
                        ThumbnailProvider.Quality.HIGH,
                        GlanceModifier
                            .fillMaxSize()
                            .defaultWeight()
                            .systemCornerRadius(),
                        content_scale = ContentScale.Crop
                    ) { image ->
                        image_accent = image.getThemeColour()
                    }

                if (image_showing) {
                    Spacer(GlanceModifier.height(15.dp))
                }

                Column(
                    GlanceModifier
                        .background(widget_background_colour)
                        .systemCornerRadius()
                        .padding(content_padding)
                ) {
                    val current_accent: Color = song_theme ?: image_accent ?: theme.accent
                    CompositionLocalProvider(
                        LocalApplicationTheme provides ThemeValuesData.of(theme).copy(accent = current_accent)
                    ) {
                        InfoContent(song, GlanceModifier.fillMaxWidth())
                    }
                }
            }
        }
        else {
            WidgetText("No song", modifier)
        }
    }

    @Composable
    private fun InfoContent(song: Song, modifier: GlanceModifier = GlanceModifier) {
        val player: PlayerState = LocalPlayerState.current

        Row(modifier) {
            Column(
                GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight()
                    .padding(end = 5.dp)
            ) {
                val title: String? by song.observeActiveTitle()
                title?.also {
                    WidgetText(it, font_size = 15.sp)
                }

                val artist_title: String? by song.Artists.observe(player.database).value?.firstOrNull()?.observeActiveTitle()
                artist_title?.also {
                    WidgetText(it, font_size = 10.sp)
                }
            }

            GlanceLargePlayPauseButton(
                !player.status.m_playing,
                GlanceModifier.size(50.dp)
            )
        }
    }
}
