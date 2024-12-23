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
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.OpenInNew
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
import dev.toastbits.composekit.util.getContrasted
import dev.toastbits.composekit.components.platform.composable.ScrollabilityIndicatorRow
import dev.toastbits.composekit.components.utils.composable.ShapedIconButton
import dev.toastbits.composekit.components.utils.modifier.vertical
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.observeUrl
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import dev.toastbits.composekit.theme.core.vibrantAccent
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.artist_chip_shuffle
import spmp.shared.generated.resources.action_share
import spmp.shared.generated.resources.artist_chip_open
import spmp.shared.generated.resources.artist_chip_details

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

    val shuffle_playlist_id: String? by artist.ShufflePlaylistId.observe(player.database)

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
                        Icon(icon, null, tint = accent_colour ?: player.theme.vibrantAccent)
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = player.theme.background,
                        labelColor = player.theme.onBackground,
                        leadingIconContentColor = accent_colour ?: player.theme.vibrantAccent
                    )
                )
            }

            if (play_button_size == null && shuffle_playlist_id != null) {
                Chip(stringResource(Res.string.artist_chip_shuffle), Icons.Outlined.Shuffle) { player.playMediaItem(artist, true) }
            }

            val artist_url: String = artist.observeUrl()

            if (player.context.canShare()) {
                Chip(
                    stringResource(Res.string.action_share),
                    Icons.Outlined.Share
                ) {
                    player.context.shareText(
                        artist_url,
                        artist.getActiveTitle(player.database) ?: ""
                    )
                }
            }
            if (player.context.canOpenUrl()) {
                Chip(
                    stringResource(Res.string.artist_chip_open),
                    Icons.AutoMirrored.Outlined.OpenInNew
                ) {
                    player.context.openUrl(artist_url)
                }
            }

            Chip(
                stringResource(Res.string.artist_chip_details),
                Icons.Outlined.Info
            ) {
                show_info = !show_info
            }

            Spacer(Modifier.width(content_padding.calculateEndPadding(LocalLayoutDirection.current)))
        }

        if (play_button_size != null && shuffle_playlist_id != null) {
            Box(Modifier.requiredHeight(height)) {
                ShapedIconButton(
                    { player.playMediaItem(artist, true) },
                    IconButtonDefaults.iconButtonColors(
                        containerColor = accent_colour ?: LocalContentColor.current,
                        contentColor = (accent_colour ?: LocalContentColor.current).getContrasted()
                    ),
                    Modifier.requiredSize(play_button_size)
                ) {
                    Icon(Icons.Default.Shuffle, null)
                }
            }
        }
    }
}
