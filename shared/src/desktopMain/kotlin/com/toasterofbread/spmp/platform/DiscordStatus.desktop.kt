package com.toasterofbread.spmp.platform

import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider

// TODO

actual class DiscordStatus actual constructor(context: AppContext, account_token: String?) {
    actual companion object {
        actual fun isSupported(): Boolean = false // TODO

        actual fun isAccountTokenRequired(): Boolean {
            TODO("Not yet implemented")
        }
    }

    actual enum class Status { ONLINE, IDLE, DO_NOT_DISTURB }
    actual enum class Type { PLAYING, STREAMING, LISTENING, WATCHING, COMPETING }

    actual fun close() {
    }

    actual suspend fun shouldUpdateStatus(): Boolean {
        TODO("Not yet implemented")
    }

    actual fun setActivity(
        name: String,
        type: Type,
        application_id: String?,
        status: Status,
        state: String?,
        details: String?,
        timestamps: Pair<Long?, Long?>?,
        large_image: String?,
        small_image: String?,
        large_text: String?,
        small_text: String?,
        buttons: List<Pair<String, String>>?,
    ) {
    }

    actual suspend fun getCustomImages(
        image_items: List<MediaItem>,
        target_quality: MediaItemThumbnailProvider.Quality,
    ): Result<List<String?>> {
        TODO("Not yet implemented")
    }
}
