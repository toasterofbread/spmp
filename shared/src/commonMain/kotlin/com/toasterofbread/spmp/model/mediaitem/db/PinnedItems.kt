package com.toasterofbread.spmp.model.mediaitem.db

import LocalPlayerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.cash.sqldelight.Query
import com.toasterofbread.spmp.db.Database
import com.toasterofbread.spmp.db.mediaitem.PinnedItemQueries
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.getType
import com.toasterofbread.spmp.model.mediaitem.playlist.LocalPlaylistData
import com.toasterofbread.spmp.model.mediaitem.playlist.LocalPlaylistRef
import com.toasterofbread.spmp.model.mediaitem.playlist.PlaylistFileConverter
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import dev.toastbits.ytmkt.model.external.mediaitem.YtmMediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import PlatformIO

fun Database.getPinnedItems(): List<MediaItem> {
    return pinnedItemQueries.getAll().executeAsList().map { item ->
        MediaItemType.entries[item.type.toInt()].referenceFromId(item.id)
    }
}

@Composable
fun rememberPinnedItems(): List<MediaItem>? {
    val player: PlayerState = LocalPlayerState.current

    var pinned_items: List<MediaItem> by remember { mutableStateOf(player.database.getPinnedItems()) }
    var loaded_pinned_items: List<MediaItem>? by remember { mutableStateOf(null) }

    DisposableEffect(Unit) {
        val listener: Query.Listener = Query.Listener {
            pinned_items = player.database.getPinnedItems()
        }

        player.database.pinnedItemQueries.getAll().addListener(listener)

        onDispose {
            player.database.pinnedItemQueries.getAll().removeListener(listener)
        }
    }

    LaunchedEffect(pinned_items) {
        val previous_loaded: List<MediaItem>? = loaded_pinned_items
        loaded_pinned_items = emptyList()

        val items: MutableList<MediaItem?> = pinned_items.toMutableList()
        val jobs: MutableList<Job> = mutableListOf()

        for ((i, item) in items.withIndex()) {
            if (item is LocalPlaylistRef) {
                jobs.add(launch(Dispatchers.PlatformIO) {
                    val existing: MediaItem? = previous_loaded?.firstOrNull { it is LocalPlaylistData && it.id == item.id }
                    if (existing != null) {
                        items[i] = existing
                    }
                    else {
                        val data: LocalPlaylistData? =
                            item.getLocalPlaylistFile(player.context)?.let { file ->
                                PlaylistFileConverter.loadFromFile(file, player.context)
                            }
                        items[i] = data
                    }
                })
            }
        }

        jobs.joinAll()
        loaded_pinned_items = items.filterNotNull()
    }

    return loaded_pinned_items
}

@Composable
fun YtmMediaItem.observePinnedToHome(): MutableState<Boolean> {
    val queries: PinnedItemQueries = LocalPlayerState.current.database.pinnedItemQueries
    val query: Query<Long> = remember(this) {
        queries.countByItem(id, getType().ordinal.toLong())
    }

    return query.observeAsState(
        Unit,
        {
            it.executeAsOne() > 0
        }
    ) { pin ->
        try {
            if (pin) {
                queries.insert(id, getType().ordinal.toLong())
            }
            else {
                queries.remove(id, getType().ordinal.toLong())
            }
        }
        catch (e: Throwable) {
            throw RuntimeException("Failed setting pinned status of $this to $pin", e)
        }
    }
}

fun YtmMediaItem.setPinned(pinned: Boolean, context: AppContext) {
    val queries: PinnedItemQueries = context.database.pinnedItemQueries
    if (pinned) {
        queries.insert(id, getType().ordinal.toLong())
    }
    else {
        queries.remove(id, getType().ordinal.toLong())
    }
}

fun MediaItem.togglePinned(context: AppContext) {
    val pinned: Boolean = context.database.getPinnedItems().any { it.id == id }
    val queries: PinnedItemQueries = context.database.pinnedItemQueries
    if (pinned) {
        queries.remove(id, getType().ordinal.toLong())
    }
    else {
        queries.insert(id, getType().ordinal.toLong())
    }
}
