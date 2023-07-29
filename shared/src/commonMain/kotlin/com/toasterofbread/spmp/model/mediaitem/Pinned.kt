package com.toasterofbread.spmp.model.mediaitem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import com.toasterofbread.Database

fun MediaItem.setPinnedToHome(pinned: Boolean, db: Database = SpMp.context.database) {
    db.mediaItemQueries.updatePinnedToHomeById(pinned.toSQLBoolean(), id)
}

@Composable
fun MediaItem.observePinnedToHome(db: Database = SpMp.context.database): MutableState<Boolean> =
    db.mediaItemQueries
        .pinnedToHomeById(id)
        .observeAsState(
            { it.executeAsOne().pinned_to_home.fromSQLBoolean() },
            { db.mediaItemQueries.updatePinnedToHomeById(it.toSQLBoolean(), id) }
        )
