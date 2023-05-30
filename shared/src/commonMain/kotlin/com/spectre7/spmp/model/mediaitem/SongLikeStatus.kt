package com.spectre7.spmp.model.mediaitem

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.spectre7.spmp.api.getSongLiked
import com.spectre7.spmp.api.setSongLiked
import com.spectre7.utils.ValueListeners
import kotlin.concurrent.thread

class SongLikeStatus(private val id: String) {
    var status: Song.LikeStatus
        get() = status_state.value
        private set(value) { status_state.value = value }
    val listeners = ValueListeners<SongLikeStatus>()
    var loading: Boolean by mutableStateOf(false)
        private set

    private val status_state = mutableStateOf(Song.LikeStatus.UNKNOWN)
    private var set_liked_thread: Thread? = null

    @Synchronized
    fun setLiked(liked: Boolean?) {
        if (status == Song.LikeStatus.UNAVAILABLE) {
            return
        }

        set_liked_thread?.interrupt()
        set_liked_thread = thread {
            setSongLiked(id, liked)
            updateStatus(false)
        }
    }

    fun updateStatus(in_thread: Boolean = true) {
        synchronized(status_state) {
            if (loading || status == Song.LikeStatus.UNAVAILABLE) {
                return
            }
            loading = true
            listeners.call(this)
        }

        fun update() {
            val result = getSongLiked(id)
            synchronized(status_state) {
                loading = false
                result.fold(
                    { status ->
                        this.status = when (status) {
                            true -> Song.LikeStatus.LIKED
                            false -> Song.LikeStatus.DISLIKED
                            null -> Song.LikeStatus.NEUTRAL
                        }
                        listeners.call(this)
                    },
                    {
                        status = Song.LikeStatus.UNAVAILABLE
                        listeners.call(this)
                    }
                )
            }
        }

        if (in_thread) {
            thread { update() }
        }
        else {
            update()
        }
    }
}
