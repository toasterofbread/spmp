package com.toasterofbread.spmp.model.mediaitem.db

import LocalDataase
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.toasterofbread.spmp.db.Database
import dev.toastbits.composekit.utils.common.getThemeColour
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.loader.MediaItemThumbnailLoader
import dev.toastbits.ytmkt.model.external.ThumbnailProvider as YtmThumbnailProvider

@Composable
fun MediaItem.rememberThemeColour(): Color? {
    val database: Database = LocalDataase.current
    val thumbnail_state = MediaItemThumbnailLoader.rememberItemState(this)
    val item_colour: Color? by ThemeColour.observe(database)

    val colour: Color? by remember(thumbnail_state, item_colour) { derivedStateOf {
        if (item_colour != null) {
            return@derivedStateOf item_colour
        }

        for (quality in YtmThumbnailProvider.Quality.entries) {
            val image = thumbnail_state.loaded_images[quality] ?: continue
            return@derivedStateOf image.getThemeColour()
        }
        return@derivedStateOf null
    } }

    return colour
}
