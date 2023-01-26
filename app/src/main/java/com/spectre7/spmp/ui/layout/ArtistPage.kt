package com.spectre7.spmp.ui.layout

import com.spectre7.spmp.model.Artist

@Composable
fun ArtistPage(artist: Artist) {

    Image(
        painter = rememberAsyncImagePainter(artist.getThumbUrl(true)),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(icon_size)
            .clip(CircleShape)
    )

}