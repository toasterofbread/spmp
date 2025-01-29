package com.toasterofbread.spmp.model.mediaitem.db

import PlatformIO
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
import dev.toastbits.composekit.context.PlatformFile
import dev.toastbits.ytmkt.model.external.mediaitem.YtmMediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration

suspend fun MediaItem.incrementPlayCount(context: AppContext, by: Int = 1): Result<Unit> = withContext(Dispatchers.PlatformIO) {
    require(by >= 1)

    try {
        if (this@incrementPlayCount is LocalPlaylist) {
            val data: LocalPlaylistData = loadData(context).fold(
                { it },
                { return@withContext Result.failure(it) }
            )
            data.play_count += by

            val file: PlatformFile =
                MediaItemLibrary.getLocalPlaylistFile(this@incrementPlayCount, context)
                ?: return@withContext Result.success(Unit)

            return@withContext data.saveToFile(file, context)
        }

        context.database.mediaItemPlayCountQueries.transaction {
            val day: Int = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toEpochDays()
            context.database.mediaItemPlayCountQueries.insertOrIgnore(day.toLong(), id)
            context.database.mediaItemPlayCountQueries.increment(by.toLong(), id, day.toLong())
        }

        return@withContext Result.success(Unit)
    }
    catch (e: Throwable) {
        val item: MediaItem = this@incrementPlayCount
        throw RuntimeException("Incrementing play count of $item by $by failed", e)
    }
}

fun YtmMediaItem.getPlayCount(db: Database, range_days: Long? = null): Int {
    if (this is LocalPlaylistData) {
        return play_count
    }

    val entries = if (range_days != null) {
        val time = Clock.System.now()
        val since_day: Int = with (Duration) { time - range_days.days }.date.toEpochDays()
        db.mediaItemPlayCountQueries.byItemIdSince(
            id, since_day.toLong(),
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
        withContext(Dispatchers.PlatformIO) {
            play_count_state = getPlayCount(db, range_days)
        }
    }

    DisposableEffect(id, range_days) {
        val query =
            if (range_days != null)
                db.mediaItemPlayCountQueries.byItemIdSince(
                    id,
                    (Clock.System.now() - with (Duration) { range_days.days }).date.toEpochDays().toLong(),
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

private val Instant.date: LocalDate
    get() = toLocalDateTime(TimeZone.currentSystemDefault()).date
