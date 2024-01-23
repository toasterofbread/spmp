package com.toasterofbread.spmp.platform

import com.toasterofbread.spmp.ProjectBuildConfig
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import dev.cbyrne.kdiscordipc.KDiscordIPC
import dev.cbyrne.kdiscordipc.core.event.impl.ReadyEvent
import dev.cbyrne.kdiscordipc.data.activity.Activity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

actual class DiscordStatus actual constructor(
    private val context: AppContext,
    application_id: String,
    account_token: String?
) {
    actual companion object {
        actual fun isSupported(): Boolean = true
        actual fun isAccountTokenRequired(): Boolean = false
        actual fun getWarningText(): String? = null
    }

    private val ipc: KDiscordIPC = KDiscordIPC(application_id)
    private val coroutine_scope = CoroutineScope(Job())
    private var connected: Boolean = false

    actual fun close() {
        ipc.disconnect()
        coroutine_scope.cancel()
    }

    actual enum class Status { ONLINE, IDLE, DO_NOT_DISTURB }
    actual enum class Type { PLAYING, STREAMING, LISTENING, WATCHING, COMPETING }

    actual suspend fun shouldUpdateStatus(): Boolean {
        // TODO
        return true
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

        coroutine_scope.launch {
            if (connected) {
                setActivity()
            }
            else {
                ipc.on<ReadyEvent> {
                    setActivity()
                }

                connected = true
                ipc.connect()
            }
        }
    }

    actual suspend fun getCustomImages(
        image_items: List<MediaItem>,
        target_quality: MediaItemThumbnailProvider.Quality
    ): Result<List<String?>> =
        Result.success(image_items.map { item ->
            val thumbnail_provider: MediaItemThumbnailProvider? = item.ThumbnailProvider.get(context.database)
            thumbnail_provider?.getThumbnailUrl(target_quality)
        })
}

actual suspend fun getDiscordAccountInfo(account_token: String?): Result<DiscordMeResponse> {
    val ipc: KDiscordIPC = KDiscordIPC(ProjectBuildConfig.DISCORD_APPLICATION_ID)
    var result: Result<DiscordMeResponse>? = null

    ipc.on<ReadyEvent> {
        result = Result.success(
            with (data.user) {
                DiscordMeResponse(
                    id = id,
                    username = username,
                    avatar = avatar,
                    discriminator = discriminator,
                    banner_color = null,
                    bio = null
                )
            }
        )

        ipc.disconnect()
    }
    ipc.connect()

    return result ?: Result.failure(RuntimeException("Result not set"))
}
