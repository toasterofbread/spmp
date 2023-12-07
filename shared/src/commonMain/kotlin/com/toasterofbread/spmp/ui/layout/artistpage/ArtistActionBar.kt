package com.toasterofbread.spmp.ui.layout.artistpage

import LocalPlayerState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.toasterofbread.composekit.utils.common.getContrasted
import com.toasterofbread.composekit.platform.composable.ScrollabilityIndicatorRow
import com.toasterofbread.composekit.utils.composable.ShapedIconButton
import com.toasterofbread.composekit.utils.modifier.vertical
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState

@Composable
fun ArtistActionBar(
    artist: Artist,
    modifier: Modifier = Modifier,
    content_padding: PaddingValues = PaddingValues(),
    height: Dp = 32.dp,
    play_button_size: Dp? = null,
    accent_colour: Color? = null
) {
    val player: PlayerState = LocalPlayerState.current

    var show_info by remember { mutableStateOf(false) }
    if (show_info) {
        ArtistInfoDialog(artist) { show_info = false }
    }

    Box(
        modifier,
        contentAlignment = Alignment.CenterEnd
    ) {
        val scroll_state: ScrollState = rememberScrollState()
        ScrollabilityIndicatorRow(
            scroll_state,
            Modifier
                .fillMaxWidth()
                .padding(content_padding.vertical)
                .padding(end = (play_button_size ?: 0.dp) / 2),
            horizontal_arrangement = Arrangement.spacedBy(10.dp),
            accent_colour = accent_colour ?: LocalContentColor.current
        ) {
            Spacer(Modifier.width(content_padding.calculateStartPadding(LocalLayoutDirection.current)))

            @Composable
            fun Chip(text: String, icon: ImageVector, onClick: () -> Unit) {
                ElevatedAssistChip(
                    onClick,
                    { Text(text, style = MaterialTheme.typography.labelLarge) },
                    Modifier.height(height),
                    leadingIcon = {
                        Icon(icon, null, tint = accent_colour ?: player.theme.vibrant_accent)
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = player.theme.background,
                        labelColor = player.theme.on_background,
                        leadingIconContentColor = accent_colour ?: player.theme.vibrant_accent
                    )
                )
            }

            if (play_button_size == null) {
                Chip(getString("artist_chip_play"), Icons.Outlined.PlayArrow) { player.playMediaItem(artist) }
            }

            Chip(getString("artist_chip_shuffle"), Icons.Outlined.Shuffle) { player.playMediaItem(artist, true) }

            if (player.context.canShare()) {
                Chip(
                    getString("action_share"),
                    Icons.Outlined.Share
                ) {
                    player.context.shareText(
                        artist.getURL(player.context),
                        artist.getActiveTitle(player.database) ?: ""
                    )
                }
            }
            if (player.context.canOpenUrl()) {
                Chip(
                    getString("artist_chip_open"),
                    Icons.Outlined.OpenInNew
                ) {
                    player.context.openUrl(
                        artist.getURL(player.context)
                    )
                }
            }

            Chip(
                getString("artist_chip_details"),
                Icons.Outlined.Info
            ) {
                show_info = !show_info
            }

            Spacer(Modifier.width(content_padding.calculateEndPadding(LocalLayoutDirection.current)))
        }

        if (play_button_size != null) {
            Box(Modifier.requiredHeight(height)) {
                ShapedIconButton(
                    { player.playMediaItem(artist) },
                    IconButtonDefaults.iconButtonColors(
                        containerColor = accent_colour ?: LocalContentColor.current,
                        contentColor = (accent_colour ?: LocalContentColor.current).getContrasted()
                    ),
                    Modifier.requiredSize(play_button_size)
                ) {
                    Icon(Icons.Default.PlayArrow, null)
                }
            }
        }
    }
}
