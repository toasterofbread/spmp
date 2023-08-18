package com.toasterofbread.spmp.model.mediaitem.db

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.cash.sqldelight.Query
import com.toasterofbread.Database
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDate

fun MediaItem.incrementPlayCount(db: Database, by: Int = 1) {
    require(by >= 1)
    db.mediaItemPlayCountQueries.transaction {
        val day = LocalDate.now().toEpochDay()
        db.mediaItemPlayCountQueries.insertOrIgnore(day, id)
        db.mediaItemPlayCountQueries.increment(by.toLong(), id, day)
    }
}

fun MediaItem.getPlayCount(db: Database, range: Duration? = null): Int {
    val entries = if (range != null) {
        val since_day = LocalDate.now().minusDays(range.toDays()).toEpochDay()
        db.mediaItemPlayCountQueries.byItemIdSince(
            id, since_day,
            { _, play_count -> play_count }
        ).executeAsList()
    }
    else {
        db.mediaItemPlayCountQueries.byItemId(
            id,
            { _, play_count -> play_count }
        ).executeAsList()
    }

    return entries.sumOf { it }.toInt()
}

@Composable
fun MediaItem.observePlayCount(db: Database, range: Duration? = null): Int {
    var play_count_state: Int by remember { mutableStateOf(0) }

    LaunchedEffect(id, range) {
        play_count_state = 0
        withContext(Dispatchers.IO) {
            play_count_state = getPlayCount(db, range)
        }
    }

    DisposableEffect(id, range) {
        val query =
            if (range != null)
                db.mediaItemPlayCountQueries.byItemIdSince(
                    id,
                    LocalDate.now().minusDays(range.toDays()).toEpochDay(),
                    { _, play_count -> play_count }
                )
            else
                db.mediaItemPlayCountQueries.byItemId(
                    id,
                    { _, play_count -> play_count }
                )

        val listener = Query.Listener {
            play_count_state = query.executeAsList().sumOf { it }.toInt()
        }

        query.addListener(listener)
        onDispose {
            query.removeListener(listener)
        }
    }

    return play_count_state
}
