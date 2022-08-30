package com.spectre7.spmp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.spectre7.spmp.model.Song
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil.compose.rememberAsyncImagePainter
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.sendToast

@Composable
fun SongPreview (song: Song, showArtist: Boolean = true) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(10.dp, 0.dp)
    ) {

        Image(
            painter = rememberAsyncImagePainter(song.getThumbUrl()),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(20))
        )

        Column(Modifier.padding(10.dp).fillMaxWidth(0.9f)) {
            Text(
                song.nativeData?.title ?: "Unknown",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (showArtist) {
                Text(
                    song.artist.nativeData.name,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha=0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        IconButton(onClick = {
             MainActivity.instance!!.player.interact {
                 it.addToQueue(song) {
                     it.play()
                 }
             }
        }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.PlayArrow, null, Modifier, MaterialTheme.colorScheme.onBackground)
        }
    }
}