package com.toasterofbread.spmp.model.mediaitem

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.toasterofbread.spmp.api.getSongLiked
import com.toasterofbread.spmp.api.setSongLiked
import com.toasterofbread.utils.ValueListeners
import kotlin.concurrent.thread

class SongLikeStatus(private val id: String) {
    enum class Status {
        UNKNOWN, UNAVAILABLE, NEUTRAL, LIKED, DISLIKED;

        val is_available: Boolean get() = when(this) {
            LIKED, DISLIKED, NEUTRAL -> true
            else -> false
        }
    }
    
    var status: Status
        get() = status_state.value
        private set(value) { status_state.value = value }
    val listeners = ValueListeners<SongLikeStatus>()
    var loading: Boolean by mutableStateOf(false)
        private set

    private val status_state = mutableStateOf(Status.UNKNOWN)
    private var set_liked_thread: Thread? = null

    @Synchronized
    fun setLiked(liked: Boolean?) {
        if (status == Status.UNAVAILABLE) {
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
            if (loading || status == Status.UNAVAILABLE) {
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
                            true -> Status.LIKED
                            false -> Status.DISLIKED
                            null -> Status.NEUTRAL
                        }
                        listeners.call(this)
                    },
                    {
                        status = Status.UNAVAILABLE
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
