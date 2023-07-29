package com.toasterofbread.spmp.model.mediaitem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import com.toasterofbread.Database

fun MediaItem.setHidden(hidden: Boolean, db: Database = SpMp.context.database) {
    db.mediaItemQueries.updateIsHiddenById(hidden.toSQLBoolean(), id)
}

@Composable
fun MediaItem.observeHidden(db: Database = SpMp.context.database): MutableState<Boolean> =
    db.mediaItemQueries
        .isHiddenById(id)
        .observeAsState(
            { it.executeAsOne().hidden.fromSQLBoolean() },
            { db.mediaItemQueries.updatePinnedToHomeById(it.toSQLBoolean(), id) }
        )
