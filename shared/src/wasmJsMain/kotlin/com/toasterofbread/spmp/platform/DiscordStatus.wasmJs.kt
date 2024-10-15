package com.toasterofbread.spmp.platform

import com.toasterofbread.spmp.model.mediaitem.MediaItem
import dev.toastbits.ytmkt.model.external.ThumbnailProvider

// TODO
actual class DiscordStatus actual constructor(
    context: AppContext,
    application_id: String,
    account_token: String?
) {
    actual companion object {
        actual fun isSupported(): Boolean = false
        actual fun isAccountTokenRequired(): Boolean = false
        actual fun getWarningText(): String? = null
    }

    actual enum class Status { ONLINE, IDLE, DO_NOT_DISTURB }
    actual enum class Type { PLAYING, STREAMING, LISTENING, WATCHING, COMPETING }

    actual fun close() {
        throw IllegalStateException()
    }

    actual suspend fun shouldUpdateStatus(): Boolean {
        throw IllegalStateException()
    }

    actual fun setActivity(
        name: String,
        type: Type,
        status: Status,
        state: String?,
        details: String?,
        timestamps: Pair<Long?, Long?>?,
        large_image: String?,
        small_image: String?,
        large_text: String?,
        small_text: String?,
        buttons: List<Pair<String, String>>?
    ) {
        throw IllegalStateException()
    }

    actual suspend fun getCustomImages(
        image_items: List<MediaItem>,
        target_quality: ThumbnailProvider.Quality
    ): Result<List<String?>> {
        throw IllegalStateException()
    }
}

actual suspend fun getDiscordAccountInfo(account_token: String?): Result<DiscordMeResponse> =
    Result.failure(IllegalStateException())
