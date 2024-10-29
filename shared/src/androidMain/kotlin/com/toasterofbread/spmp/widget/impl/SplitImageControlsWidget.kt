package com.toasterofbread.spmp.widget.impl

import LocalPlayerState
import android.graphics.Bitmap
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentWidth
import androidx.glance.unit.ColorProvider
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.shared.R
import com.toasterofbread.spmp.widget.SpMpWidget
import com.toasterofbread.spmp.widget.action.SplitImageControlsWidgetClickAction
import com.toasterofbread.spmp.widget.component.GlanceActionButtonGrid
import com.toasterofbread.spmp.widget.component.GlanceActionButtonGridMode
import com.toasterofbread.spmp.widget.component.styledcolumn.GLANCE_STYLED_COLUMN_DEFAULT_SPACING
import com.toasterofbread.spmp.widget.configuration.enum.WidgetSectionTheme
import com.toasterofbread.spmp.widget.configuration.type.SplitImageControlsWidgetConfig
import com.toasterofbread.spmp.widget.modifier.systemCornerRadius
import dev.toastbits.composekit.platform.composable.theme.LocalApplicationTheme
import dev.toastbits.composekit.settings.ui.vibrant_accent
import dev.toastbits.composekit.utils.common.blendWith
import dev.toastbits.composekit.utils.common.getValue

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
    override fun Content(
        song: Song?,
        song_image: Bitmap?,
        modifier: GlanceModifier,
        content_padding: PaddingValues
    ) {
        val player: PlayerState = LocalPlayerState.current

        if (song == null) {
            WidgetText("No song", modifier)
            return
        }

        val top_bar_height: Dp = 100.dp

        StyledColumn(
            listOf(type_configuration.title_row_theme, type_configuration.content_row_theme),
            {
                Column(
                    GlanceModifier.height(top_bar_height - content_padding.calculateTopPadding() - content_padding.calculateBottomPadding()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val title: String? by song.observeActiveTitle()
                    title?.also {
                        val colour: Color =
                            when (type_configuration.title_row_theme.mode) {
                                WidgetSectionTheme.Mode.BACKGROUND -> with (LocalApplicationTheme.current) {
                                        accent.blendWith(on_background, 0.4f)
                                    }
                                WidgetSectionTheme.Mode.ACCENT,
                                WidgetSectionTheme.Mode.TRANSPARENT -> LocalContentColor.current
                            }

                        WidgetText(it, font_size = 18.sp, colour = colour)
                    }

                    val artist_title: String? by song.Artists.observe(player.database).value?.firstOrNull()?.observeActiveTitle()
                    artist_title?.also {
                        WidgetText(
                            it,
                            GlanceModifier.padding(top = 3.dp),
                            font_size = 14.sp,
                            alpha = 0.7f
                        )
                    }
                }
            },
            {
                Row(
                    GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    val height: Dp = LocalSize.current.height - top_bar_height - GLANCE_STYLED_COLUMN_DEFAULT_SPACING - content_padding.calculateTopPadding() - content_padding.calculateBottomPadding()
                    val max_button_grid_size: Dp = 150.dp
                    val button_grid_size: Dp = height.coerceAtMost(max_button_grid_size)

                    if (song_image != null) {
                        val image_available_width: Dp = (
                            LocalSize.current.width
                            - content_padding.calculateStartPadding(LocalLayoutDirection.current)
                            - content_padding.calculateEndPadding(LocalLayoutDirection.current)
                            - button_grid_size
                            - GLANCE_STYLED_COLUMN_DEFAULT_SPACING
                        )
                        val image_size: Dp = minOf(image_available_width, height)

                        Image(
                            ImageProvider(song_image),
                            null,
                            GlanceModifier
                                .size(image_size)
                                .systemCornerRadius(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(GlanceModifier.fillMaxWidth().defaultWeight())
                    Spacer(GlanceModifier.width(GLANCE_STYLED_COLUMN_DEFAULT_SPACING))

                    val show_icon = (button_grid_size - max_button_grid_size) >= 20.dp

                    Column(
                        GlanceModifier.wrapContentWidth().fillMaxHeight(),
                        horizontalAlignment = Alignment.End
                    ) {
                        GlanceActionButtonGrid(
                            DpSize(
                                button_grid_size,
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
                            alignment = Alignment.TopStart,
                            button_background_colour =
                                when (type_configuration.content_row_theme.mode) {
                                    WidgetSectionTheme.Mode.BACKGROUND,
                                    WidgetSectionTheme.Mode.TRANSPARENT -> LocalApplicationTheme.current.vibrant_accent
                                    WidgetSectionTheme.Mode.ACCENT -> widget_background_colour.copy(alpha = 1f)
                                },
                            modifier = GlanceModifier.defaultWeight()
                        )

                        Spacer(GlanceModifier.fillMaxHeight().defaultWeight())

                        if (show_icon) {
                            Image(
                                ImageProvider(R.drawable.ic_spmp),
                                null,
                                GlanceModifier.size(20.dp),
                                colorFilter = ColorFilter.tint(ColorProvider(LocalContentColor.current))
                            )
                        }
                    }
                }
            },
            modifier = GlanceModifier
                .fillMaxWidth()
                .systemCornerRadius(),
            content_padding = content_padding,
            vertical_alignment = Alignment.Bottom
        )
    }
}

fun SplitImageControlsWidgetClickAction.getIcon(): Int? =
    when (this) {
        else -> null
    }
