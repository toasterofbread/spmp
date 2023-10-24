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
import com.toasterofbread.Database
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.playlist.LocalPlaylistData
import com.toasterofbread.spmp.model.mediaitem.playlist.LocalPlaylistRef
import com.toasterofbread.spmp.model.mediaitem.playlist.PlaylistFileConverter
import com.toasterofbread.spmp.platform.PlatformContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

private fun Database.getPinnedItems(): List<MediaItem> {
    return pinnedItemQueries.getAll().executeAsList().map { item ->
        MediaItemType.values()[item.type.toInt()].referenceFromId(item.id)
    }
}
private fun Database.isItemPinned(item: MediaItem): Boolean {
    return pinnedItemQueries.countByItem(item.id, item.getType().ordinal.toLong()).executeAsOne() > 0
}

@Composable
fun rememberAnyItemsArePinned(context: PlatformContext): Boolean {
    val db = context.database

    val any_are_pinned = remember { mutableStateOf(
        db.pinnedItemQueries.count().executeAsOne() > 0
    ) }

    DisposableEffect(Unit) {
        val listener = Query.Listener {
            any_are_pinned.value = db.pinnedItemQueries.count().executeAsOne() > 0
        }

        val query = db.pinnedItemQueries.count()
        query.addListener(listener)

        onDispose {
            query.removeListener(listener)
        }
    }

    return any_are_pinned.value
}

@Composable
fun rememberPinnedItems(context: PlatformContext): List<MediaItem> {
    val db = context.database

    var pinned_items: List<MediaItem> by remember { mutableStateOf(db.getPinnedItems()) }
    var loaded_pinned_items: List<MediaItem> by remember { mutableStateOf(emptyList()) }

    DisposableEffect(Unit) {
        val listener = Query.Listener {
            pinned_items = db.getPinnedItems()
        }

        db.pinnedItemQueries.getAll().addListener(listener)

        onDispose {
            db.pinnedItemQueries.getAll().removeListener(listener)
        }
    }

    LaunchedEffect(pinned_items) {
        val previous_loaded: List<MediaItem> = loaded_pinned_items
        loaded_pinned_items = emptyList()

        val items: MutableList<MediaItem?> = pinned_items.toMutableList()
        val jobs: MutableList<Job> = mutableListOf()

        for ((i, item) in items.withIndex()) {
            if (item is LocalPlaylistRef) {
                jobs.add(launch(Dispatchers.IO) {
                    val existing: MediaItem? = previous_loaded.firstOrNull { it is LocalPlaylistData && it.id == item.id }
                    if (existing != null) {
                        items[i] = existing
                    }
                    else {
                        val data: LocalPlaylistData? = PlaylistFileConverter.loadFromFile(item.getLocalPlaylistFile(context), context)
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
fun MediaItem.observePinnedToHome(): MutableState<Boolean> {
    val queries = LocalPlayerState.current.database.pinnedItemQueries
    val query = remember(this) {
        queries.countByItem(id, getType().ordinal.toLong())
    }

    return query.observeAsState(
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

fun MediaItem.setPinned(pinned: Boolean, context: PlatformContext) {
    val queries = context.database.pinnedItemQueries
    if (pinned) {
        queries.insert(id, getType().ordinal.toLong())
    }
    else {
        queries.remove(id, getType().ordinal.toLong())
    }
}
