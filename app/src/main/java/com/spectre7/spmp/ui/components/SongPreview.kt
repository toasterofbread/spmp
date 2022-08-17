package com.spectre7.spmp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import com.spectre7.spmp.model.Song

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter

@Composable
fun SongPreview (song: Song, showArtist: Boolean = true) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {

        Image(
            painter = rememberAsyncImagePainter(song.getThumbUrl()),
            contentDescription = null,
            modifier = Modifier.size(64.dp)
        )

        Column {
            Text(song.nativeData.title)

            if (showArtist)
                Text(song.artist.nativeData.name)
        }

    }
}