package com.toasterofbread.spmp.ui.component.longpressmenu.playlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.toastbits.composekit.util.composable.WidthShrinkText
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.lpm_action_play_shuffled_after_x_songs

@Composable
fun ColumnScope.PlaylistLongPressMenuInfo(playlist: Playlist, getAccentColour: () -> Color) {
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

    Item(Icons.Default.SubdirectoryArrowRight, stringResource(Res.string.lpm_action_play_shuffled_after_x_songs))

    Spacer(
        Modifier
            .fillMaxHeight()
            .weight(1f)
    )
}
