package com.toasterofbread.spmp.ui.component.longpressmenu.song

import LocalPlayerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.toastbits.composekit.util.composable.WidthShrinkText
import com.toasterofbread.spmp.model.mediaitem.song.Song
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.lpm_action_radio_at_song_pos
import spmp.shared.generated.resources.lpm_action_radio_after_x_songs
import spmp.shared.generated.resources.lpm_info_queue_index

@Composable
fun ColumnScope.SongLongPressMenuInfo(song: Song, queue_index: Int?, getAccentColour: () -> Color) {
    @Composable
    fun Item(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
        Row(
            modifier,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = getAccentColour())
            WidthShrinkText(text, fontSize = 15.sp)
        }
    }

    if (queue_index != null) {
        Item(Icons.Default.Radio, stringResource(Res.string.lpm_action_radio_at_song_pos))
    }

    val player = LocalPlayerState.current
    if ((player.controller?.service_player?.active_queue_index ?: 0) < player.status.m_song_count) {
        Item(Icons.Default.SubdirectoryArrowRight, stringResource(Res.string.lpm_action_radio_after_x_songs))
    }

    Spacer(Modifier.fillMaxHeight().weight(1f))

    if (queue_index != null) {
        Text(stringResource(Res.string.lpm_info_queue_index).replace("\$index", queue_index.toString()))
    }
}
