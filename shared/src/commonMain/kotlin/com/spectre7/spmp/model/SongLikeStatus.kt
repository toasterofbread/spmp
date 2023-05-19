package com.spectre7.spmp.model

import androidx.compose.runtime.mutableStateOf
import com.spectre7.spmp.api.getSongLiked
import com.spectre7.spmp.api.setSongLiked
import com.spectre7.utils.ValueListeners
import kotlin.concurrent.thread

class SongLikeStatus(private val id: String) {
    var status: Song.LikeStatus
        get() = status_state.value
        private set(value) { status_state.value = value }
    val listeners = ValueListeners<Song.LikeStatus>()

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
            if (status == Song.LikeStatus.LOADING || status == Song.LikeStatus.UNAVAILABLE) {
                return
            }
            status = Song.LikeStatus.LOADING
            listeners.call(status)
        }

        fun update() {
            val result = getSongLiked(id)
            synchronized(status_state) {
                result.fold(
                    { status ->
                        this.status = when (status) {
                            true -> Song.LikeStatus.LIKED
                            false -> Song.LikeStatus.DISLIKED
                            null -> Song.LikeStatus.NEUTRAL
                        }
                        listeners.call(this.status)
                    },
                    {
                        status = Song.LikeStatus.UNAVAILABLE
                        listeners.call(status)
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
