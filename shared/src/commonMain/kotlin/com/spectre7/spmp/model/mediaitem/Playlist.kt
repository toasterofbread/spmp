package com.spectre7.spmp.model.mediaitem

import androidx.compose.runtime.*
import com.spectre7.spmp.api.cast
import com.spectre7.spmp.model.Settings
import com.spectre7.spmp.model.mediaitem.data.PlaylistItemData
import com.spectre7.spmp.model.mediaitem.enums.PlaylistType
import com.spectre7.spmp.ui.component.MediaItemLayout
import com.spectre7.spmp.ui.component.PlaylistPreviewLong
import com.spectre7.spmp.ui.component.PlaylistPreviewSquare
import com.spectre7.utils.addUnique

class PlaylistDataRegistryEntry: MediaItemDataRegistry.Entry() {
    var playlist_page_thumb_width: Float? by mutableStateOf(null)
    var image_item_uid: String? by mutableStateOf(null)

    override fun clear() {
        super.clear()
        playlist_page_thumb_width = null
        image_item_uid = null
    }
}

abstract class Playlist protected constructor (id: String): MediaItem(id), MediaItemWithLayouts {
    abstract override val data: PlaylistItemData
    val playlist_reg_entry: PlaylistDataRegistryEntry = registry_entry as PlaylistDataRegistryEntry

    abstract val is_editable: Boolean?
    abstract val playlist_type: PlaylistType?
    abstract val total_duration: Long?
    abstract val item_count: Int?

    override val feed_layouts: List<MediaItemLayout>?
        @Composable
        get() = getLayout()?.let { listOf(it) }

    override suspend fun getFeedLayouts(): Result<List<MediaItemLayout>> {
        data.items?.also { return Result.success(listOf(MediaItemLayout(null, null, MediaItemLayout.Type.LIST, it))) }
        val result = loadGeneralData()
        if (result.isFailure) {
            return result.cast()
        }
        return data.items?.let {
            Result.success(listOf(MediaItemLayout(null, null, MediaItemLayout.Type.LIST, it)))
        } ?: Result.failure(RuntimeException("Items not loaded"))
    }

    @Composable
    fun getLayout(): MediaItemLayout? {
        checkNotDeleted()
        return remember(data.items) {
            data.items?.let {
                MediaItemLayout(null, null, MediaItemLayout.Type.LIST, it)
            }
        }
    }

    open val items: MutableList<MediaItem>? get() = checkNotDeleted(data.items)
    val year: Int? get() = checkNotDeleted(data.year)

    override fun getHolder(): PlaylistHolder = PlaylistHolder(this)

    var is_deleted: Boolean = false
        private set
    fun checkNotDeleted() {
        check(!is_deleted) { toString() }
    }
    fun <T> checkNotDeleted(result: T): T {
        check(!is_deleted)
        return result
    }

    open fun addItem(item: MediaItem) {
        checkNotDeleted()
        check(is_editable == true)
        data.items!!.add(item)
    }

    open fun removeItem(index: Int) {
        checkNotDeleted()
        check(is_editable == true)
        data.items!!.removeAt(index)
    }

    open fun moveItem(from: Int, to: Int) {
        checkNotDeleted()
        check(is_editable == true)
        data.items!!.add(to, data.items!!.removeAt(from))
    }

    abstract suspend fun deletePlaylist(): Result<Unit>
    abstract suspend fun saveItems(): Result<Unit>

    @Composable
    override fun getThumbnailHolder(): MediaItem {
        checkNotDeleted()
        var item: MediaItem by remember { mutableStateOf(this) }
        LaunchedEffect(playlist_reg_entry.image_item_uid) {
            val uid = playlist_reg_entry.image_item_uid
            item = if (uid != null) fromUid(uid)
            else this@Playlist
        }
        return item
    }

    @Composable
    override fun PreviewSquare(params: MediaItemPreviewParams) {
        checkNotDeleted()
        PlaylistPreviewSquare(this, params)
    }

    @Composable
    override fun PreviewLong(params: MediaItemPreviewParams) {
        checkNotDeleted()
        PlaylistPreviewLong(this, params)
    }

    interface Listener {
        fun onReplaced(with: Playlist)
        fun onDeleted()
    }
    private val listeners: MutableList<Listener> = mutableListOf()
    fun addListener(listener: Listener) {
        checkNotDeleted()
        listeners.addUnique(listener)
    }
    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    override fun getDefaultRegistryEntry(): PlaylistDataRegistryEntry = PlaylistDataRegistryEntry()

    protected fun onDeleted() {
        checkNotDeleted()

        if (pinned_to_home) {
            setPinnedToHome(false)
        }

        is_deleted = true
        listeners.forEach { it.onDeleted() }
        listeners.clear()
    }

    protected fun onReplaced(with: Playlist) {
        checkNotDeleted()

        if (pinned_to_home) {
            var found = false
            val new_pinned = Settings.INTERNAL_PINNED_ITEMS.get<Set<String>>().map {
                if (it == uid) {
                    found = true
                    with.uid
                }
                else it
            }

            check(found)
            Settings.INTERNAL_PINNED_ITEMS.set(new_pinned.toSet())
        }

        is_deleted = true

        for (listener in listeners) {
            listener.onReplaced(with)
        }
        listeners.clear()
    }
}
