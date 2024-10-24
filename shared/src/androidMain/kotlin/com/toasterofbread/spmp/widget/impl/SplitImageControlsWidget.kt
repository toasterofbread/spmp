package com.toasterofbread.spmp.widget.impl

import LocalPlayerState
import android.graphics.Bitmap
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.component.Thumbnail
import com.toasterofbread.spmp.widget.SpMpWidget
import com.toasterofbread.spmp.widget.action.SplitImageControlsWidgetClickAction
import com.toasterofbread.spmp.widget.component.GlanceActionButtonGrid
import com.toasterofbread.spmp.widget.component.GlanceActionButtonGridMode
import com.toasterofbread.spmp.widget.configuration.type.SplitImageControlsWidgetConfig
import com.toasterofbread.spmp.widget.modifier.padding
import dev.toastbits.composekit.platform.composable.theme.LocalApplicationTheme
import dev.toastbits.composekit.settings.ui.ThemeValues
import dev.toastbits.composekit.settings.ui.ThemeValuesData
import dev.toastbits.composekit.utils.common.getThemeColour
import dev.toastbits.composekit.utils.common.getValue
import dev.toastbits.ytmkt.model.external.ThumbnailProvider

val DEFAULT_WIDGET_AREA_SPACING: Dp = 12.dp

@Composable
fun WithCurrentSongImage(
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

internal class SplitImageControlsWidget: SpMpWidget<SplitImageControlsWidgetClickAction, SplitImageControlsWidgetConfig>(
    custom_background = true,
    exact_size = true
) {
    override fun executeTypeAction(action: SplitImageControlsWidgetClickAction) =
        when (action) {
            else -> throw IllegalStateException(action.toString())
        }

    @Composable
    override fun hasContent(): Boolean =
        LocalPlayerState.current.status.m_song != null

    @Composable
    override fun Content(modifier: GlanceModifier, content_padding: PaddingValues) {
        val player: PlayerState = LocalPlayerState.current

        WithCurrentSongImage { song, image ->
            if (song == null) {
                WidgetText("No song", modifier)
                return@WithCurrentSongImage
            }

            Column(
                modifier.fillMaxSize(),
                verticalAlignment = Alignment.Bottom
            ) {
                val top_bar_height: Dp = 100.dp

                Column(
                    GlanceModifier
                        .fillMaxWidth()
                        .height(top_bar_height)
                        .padding(content_padding)
                        .background(LocalApplicationTheme.current.background)
                        .systemCornerRadius(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val title: String? by song.observeActiveTitle()
                    title?.also {
                        WidgetText(it, font_size = 15.sp)
                    }

                    val artist_title: String? by song.Artists.observe(player.database).value?.firstOrNull()?.observeActiveTitle()
                    artist_title?.also {
                        WidgetText(
                            it,
                            GlanceModifier.padding(top = 3.dp),
                            font_size = 13.sp,
                            alpha = 0.7f
                        )
                    }
                }

                Spacer(GlanceModifier.height(DEFAULT_WIDGET_AREA_SPACING))

                Row(
                    GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    if (image != null) {
                        Image(
                            ImageProvider(image),
                            null,
                            GlanceModifier
                                .fillMaxSize()
                                .defaultWeight()
                                .systemCornerRadius(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(GlanceModifier.width(DEFAULT_WIDGET_AREA_SPACING))

                    val button_grid_size: Dp = LocalSize.current.height - top_bar_height - DEFAULT_WIDGET_AREA_SPACING

                    GlanceActionButtonGrid(
                        DpSize(
                            button_grid_size.coerceAtMost(150.dp),
                            button_grid_size
                        ),
                        GlanceActionButtonGridMode.NO_FILL,
                        type_configuration.top_start_button_action,
                        type_configuration.top_end_button_action,
                        type_configuration.bottom_start_button_action,
                        type_configuration.bottom_end_button_action,
                        { it.getIcon() },
                        button_modifier = GlanceModifier.systemCornerRadius(),
                        spacing = 7.dp,
                        alignment = Alignment.TopStart
                    )
                }

            }
        }
    }
}

fun SplitImageControlsWidgetClickAction.getIcon(): Int? =
    when (this) {
        else -> null
    }
