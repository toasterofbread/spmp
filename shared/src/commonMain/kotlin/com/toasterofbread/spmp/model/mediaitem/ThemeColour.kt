package com.toasterofbread.spmp.model.mediaitem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.toasterofbread.Database
import com.toasterofbread.spmp.model.mediaitem.loader.MediaItemThumbnailLoader
import com.toasterofbread.utils.getThemeColour

@Composable
fun MediaItem.rememberThemeColour(db: Database = SpMp.context.database): Color? {
    val thumbnail_state = MediaItemThumbnailLoader.rememberItemState(this)
    val item_colour by db.mediaItemQueries
        .themeColourById(id)
        .observeAsState(
            { it.executeAsOne().theme_colour?.let { Color(it) } },
            null
        )

    val colour: Color? by remember(thumbnail_state, item_colour) { derivedStateOf {
        if (item_colour != null) {
            return@derivedStateOf item_colour
        }

        for (quality in MediaItemThumbnailProvider.Quality.values()) {
            val image = thumbnail_state.loaded_images[quality] ?: continue
            return@derivedStateOf image.getThemeColour()
        }
        return@derivedStateOf null
    } }

    return colour
}
