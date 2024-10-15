package com.toasterofbread.spmp.platform

import androidx.compose.runtime.Composable
import com.toasterofbread.spmp.ProjectBuildConfig
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import dev.cbyrne.kdiscordipc.KDiscordIPC
import dev.cbyrne.kdiscordipc.core.event.impl.ReadyEvent
import dev.cbyrne.kdiscordipc.data.activity.Activity
import dev.toastbits.ytmkt.model.external.ThumbnailProvider

actual class DiscordStatus actual constructor(
    private val context: AppContext,
    application_id: String,
    account_token: String?
) {
    actual companion object {
        actual fun isSupported(): Boolean = true
        actual fun isAccountTokenRequired(): Boolean = false
        @Composable
        actual fun getWarningText(): String? = null
    }

    private val ipc: KDiscordIPC = KDiscordIPC(application_id)
    private var connected: Boolean = false

    actual fun close() {
        try {
            ipc.disconnect()
        }
        catch (_: Throwable) {}
    }

    actual enum class Status { ONLINE, IDLE, DO_NOT_DISTURB }
    actual enum class Type { PLAYING, STREAMING, LISTENING, WATCHING, COMPETING }

    actual suspend fun shouldUpdateStatus(): Boolean {
        // TODO
        return true
    }

    actual suspend fun setActivity(
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
        buttons: List<Pair<String, String>>?,
    ) {
        suspend fun setActivity() {
            ipc.activityManager.setActivity(Activity(
                details = details,
                state = state,
                timestamps = timestamps?.let { Activity.Timestamps(it.first ?: 0, it.second) },
                assets = Activity.Assets(
                    largeImage = large_image,
                    largeText = large_text,
                    smallImage = small_image,
                    smallText = small_text
                ),
                buttons = buttons?.map { button ->
                    Activity.Button(label = button.first, url = button.second)
                }?.toMutableList()
            ))
        }

        if (connected) {
            setActivity()
            return
        }

        ipc.on<ReadyEvent> {
            setActivity()
        }

        try {
            connected = true
            ipc.connect()
        }
        catch (e: Throwable) {
            e.printStackTrace()
            connected = false
        }
    }

    actual suspend fun getCustomImages(
        image_items: List<MediaItem>,
        target_quality: ThumbnailProvider.Quality
    ): Result<List<String?>> =
        Result.success(image_items.map { item ->
            val thumbnail_provider: ThumbnailProvider? = item.ThumbnailProvider.get(context.database)
            thumbnail_provider?.getThumbnailUrl(target_quality)
        })
}

actual suspend fun getDiscordAccountInfo(account_token: String?): Result<DiscordMeResponse> = runCatching {
    val ipc: KDiscordIPC = KDiscordIPC(ProjectBuildConfig.DISCORD_APPLICATION_ID)
    var result: DiscordMeResponse? = null

    ipc.on<ReadyEvent> {
        result = with (data.user) {
            DiscordMeResponse(
                id = id,
                username = username,
                avatar = avatar,
                discriminator = discriminator,
                banner_color = null,
                bio = null
            )
        }

        ipc.disconnect()
    }
    ipc.connect()

    return@runCatching result ?: throw NullPointerException("Result not set")
}
