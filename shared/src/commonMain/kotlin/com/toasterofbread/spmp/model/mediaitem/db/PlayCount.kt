package com.toasterofbread.spmp.model.mediaitem.db

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.cash.sqldelight.Query
import com.toasterofbread.spmp.db.Database
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.library.MediaItemLibrary
import com.toasterofbread.spmp.model.mediaitem.playlist.LocalPlaylist
import com.toasterofbread.spmp.model.mediaitem.playlist.LocalPlaylistData
import com.toasterofbread.spmp.model.mediaitem.playlist.PlaylistFileConverter.saveToFile
import com.toasterofbread.spmp.platform.AppContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

suspend fun MediaItem.incrementPlayCount(context: AppContext, by: Int = 1): Result<Unit> = withContext(Dispatchers.IO) {
    require(by >= 1)

    try {
        if (this@incrementPlayCount is LocalPlaylist) {
            val data: LocalPlaylistData = loadData(context).fold(
                { it },
                { return@withContext Result.failure(it) }
            )
            data.play_count += by

            val file = MediaItemLibrary.getLocalPlaylistFile(this@incrementPlayCount, context)
            return@withContext data.saveToFile(file, context)
        }

        context.database.mediaItemPlayCountQueries.transaction {
            val day = LocalDate.now().toEpochDay()
            context.database.mediaItemPlayCountQueries.insertOrIgnore(day, id)
            context.database.mediaItemPlayCountQueries.increment(by.toLong(), id, day)
        }

        return@withContext Result.success(Unit)
    }
    catch (e: Throwable) {
        throw RuntimeException("Incrementing play count of ${this@incrementPlayCount} by $by failed", e)
    }
}

fun MediaItem.getPlayCount(db: Database, range_days: Long? = null): Int {
    if (this is LocalPlaylistData) {
        return play_count
    }

    val entries = if (range_days != null) {
        val since_day = LocalDate.now().minusDays(range_days).toEpochDay()
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
fun MediaItem.observePlayCount(context: AppContext, range_days: Long? = null): Int? {
    val db = context.database
    var play_count_state: Int? by remember { mutableStateOf(null) }

    LaunchedEffect(id, range_days) {
        play_count_state = null
        withContext(Dispatchers.IO) {
            play_count_state = getPlayCount(db, range_days)
        }
    }

    DisposableEffect(id, range_days) {
        val query =
            if (range_days != null)
                db.mediaItemPlayCountQueries.byItemIdSince(
                    id,
                    LocalDate.now().minusDays(range_days).toEpochDay(),
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
