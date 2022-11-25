package com.spectre7.spmp.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.spectre7.spmp.model.Playlist
import com.spectre7.utils.setAlpha

@Composable
fun PlaylistPreview(playlist: Playlist, large: Boolean, colour: Color, modifier: Modifier) {
    Column(
        modifier.padding(10.dp, 0.dp).clickable {
//            PlayerHost.service.playSong(song)
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(contentAlignment = Alignment.BottomStart) {
            Image(
                painter = rememberAsyncImagePainter(playlist.getThumbUrl(false)),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(100.dp).clip(RoundedCornerShape(10))
            )
            Box(Modifier.padding(2.dp)) {
                Box(Modifier.background(Color.Black.setAlpha(0.5), CircleShape).padding(2.dp)) {
                    Icon(Icons.Filled.PlaylistPlay, null)
                }
            }
        }

        Text(
            playlist.title,
            fontSize = 12.sp,
            color = colour,
            maxLines = 1,
            lineHeight = 14.sp,
            overflow = TextOverflow.Ellipsis
        )
    }
}