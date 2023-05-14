package com.spectre7.spmp.platform

import androidx.compose.ui.graphics.ImageBitmap

expect class DiscordStatus(
    bot_token: String? = null,
    guild_id: Long? = null,
    custom_images_channel_category_id: Long? = null,
    custom_images_channel_name_prefix: String = "",
    account_token: String? = null
) {

    companion object {
        fun isSupported(): Boolean
        fun isAccountTokenRequired(): Boolean
    }

    enum class Status { ONLINE, IDLE, DO_NOT_DISTURB }
    enum class Type { PLAYING, STREAMING, LISTENING, WATCHING, COMPETING }

    fun close()

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

    suspend fun getCustomImage(unique_id: String, imageProvider: () -> ImageBitmap?): Result<String?>
}
