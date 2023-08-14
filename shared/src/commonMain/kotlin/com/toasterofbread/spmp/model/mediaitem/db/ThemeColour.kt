package com.toasterofbread.spmp.model.mediaitem.db

import SpMp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.toasterofbread.Database
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.loader.MediaItemThumbnailLoader
import com.toasterofbread.utils.getThemeColour

@Composable
fun MediaItem.rememberThemeColour(db: Database = SpMp.context.database): Color? {
    val thumbnail_state = MediaItemThumbnailLoader.rememberItemState(this, db)
    val item_colour: Color? by ThemeColour.observe(db)

    val colour: Color? by remember(thumbnail_state, item_colour) { derivedStateOf {
        if (item_colour != null) {
            return@derivedStateOf item_colour
        }

        for (quality in MediaItemThumbnailProvider.Quality.values()) {
            val image = thumbnail_state.loaded_images[quality]?.get() ?: continue
            return@derivedStateOf image.getThemeColour()
        }
        return@derivedStateOf null
    } }

    return colour
}
