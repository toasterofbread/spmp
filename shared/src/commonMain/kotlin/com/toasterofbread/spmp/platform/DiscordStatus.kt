package com.toasterofbread.spmp.platform

import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider

expect class DiscordStatus(
    context: AppContext,
    account_token: String? = null
) {

    companion object {
        fun isSupported(): Boolean
        fun isAccountTokenRequired(): Boolean
    }

    enum class Status { ONLINE, IDLE, DO_NOT_DISTURB }
    enum class Type { PLAYING, STREAMING, LISTENING, WATCHING, COMPETING }

    fun close()

    suspend fun shouldUpdateStatus(): Boolean

    fun setActivity(
        name: String,
        type: Type,
        application_id: String? = null,
        status: Status = Status.ONLINE,
        state: String? = null,
        details: String? = null,
        timestamps: Pair<Long?, Long?>? = null,
        large_image: String? = null,
        small_image: String? = null,
        large_text: String? = null,
        small_text: String? = null,
        buttons: List<Pair<String, String>>? = null
    )

    suspend fun getCustomImages(image_items: List<MediaItem>, target_quality: MediaItemThumbnailProvider.Quality): Result<List<String?>>
}
