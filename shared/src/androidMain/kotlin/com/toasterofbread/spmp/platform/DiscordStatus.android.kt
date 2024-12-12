package com.toasterofbread.spmp.platform

import android.net.Uri
import androidx.compose.runtime.Composable
import com.my.kizzyrpc.KizzyRPC
import com.my.kizzyrpc.entities.presence.Activity
import com.my.kizzyrpc.entities.presence.Assets
import com.my.kizzyrpc.entities.presence.Metadata
import com.my.kizzyrpc.entities.presence.Timestamps
import com.toasterofbread.spmp.ProjectBuildConfig
import com.toasterofbread.spmp.model.JsonHttpClient
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import dev.toastbits.ytmkt.model.external.ThumbnailProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.protobuf.ProtoBuf
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.warning_discord_kizzy
import java.io.IOException
import java.util.Base64

actual class DiscordStatus actual constructor(
    private val context: AppContext,
    private val application_id: String,
    private val account_token: String?
) {
    actual companion object {
        actual fun isSupported(): Boolean = true
        actual fun isAccountTokenRequired(): Boolean = true

        @Composable
        actual fun getWarningText(): String? = stringResource(Res.string.warning_discord_kizzy)
    }

    private val rpc: KizzyRPC

    init {
        if (account_token.isNullOrBlank()) {
            throw IllegalArgumentException("Account token is required")
        }
        rpc = KizzyRPC(account_token)
    }

    actual fun close() {
        rpc.closeRPC()
    }

    actual enum class Status {
        ONLINE, IDLE, DO_NOT_DISTURB;

        val kizzy_status: String get() = when (this) {
            ONLINE -> "online"
            IDLE -> "idle"
            DO_NOT_DISTURB -> "dnd"
        }
    }
    actual enum class Type { PLAYING, STREAMING, LISTENING, WATCHING, COMPETING }

    @Serializable
    data class PreloadedUserSettings(
        val versions: Unit,
        val inbox: Unit,
        val guilds: Unit,
        val user_content: Unit,
        val voice_and_video: Unit,
        val text_and_images: Unit,
        val notifications: Unit,
        val privacy: Unit,
        val debug: Unit,
        val game_library: Unit,
        val status: StatusSettings,
//        val localization: Unit,
//        val appearance: Unit
    ) {
        @Serializable
        data class StatusSettings(
            val status: UserStatus
        )

        @Serializable
        data class UserStatus(
            val status: String
        )

        fun getStatus(): String = status.status.status
    }

    @Serializable
    private data class ProtoSettingsResponse(
        val settings: String
    )

    actual suspend fun shouldUpdateStatus(): Boolean {
        if (account_token == null) {
            return true
        }

        val disabled_statuses: List<String> =
            listOfNotNull(
                "invisible".takeIf { context.settings.Discord.STATUS_DISABLE_WHEN_INVISIBLE.get() },
                "dnd".takeIf { context.settings.Discord.STATUS_DISABLE_WHEN_DND.get() },
                "idle".takeIf { context.settings.Discord.STATUS_DISABLE_WHEN_IDLE.get() },
                "offline".takeIf { context.settings.Discord.STATUS_DISABLE_WHEN_OFFLINE.get() },
                "online".takeIf { context.settings.Discord.STATUS_DISABLE_WHEN_ONLINE.get() }
            )

        if (disabled_statuses.isEmpty()) {
            return true
        }

        val response: HttpResponse =
            JsonHttpClient.get("https://discord.com/api/v9/users/@me/settings-proto/1") {
                headers {
                    append("authorization", account_token)
                }
            }

        if (response.status.value !in 200 .. 299) {
            return true
        }

        val result: ProtoSettingsResponse = response.body()

        val settings: PreloadedUserSettings =
            try {
                val bytes = Base64.getDecoder().decode(result.settings)
                ProtoBuf.decodeFromByteArray(bytes)
            }
            catch (e: Throwable) {
                return true
            }

        return !disabled_statuses.contains(settings.getStatus())
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
        buttons: List<Pair<String, String>>?
    ) {
        rpc.updateRPC(
            Activity(
                name = name,
                state = state,
                details = details,
                type = type.ordinal,
                timestamps = timestamps?.run { Timestamps(start = first, end = second) },
                assets = Assets(
                    largeImage = large_image,
                    smallImage = small_image,
                    largeText = large_text,
                    smallText = small_text,
                ),
                buttons = buttons?.map { it.first },
                metadata = buttons?.run { Metadata(map { it.second }) },
                applicationId = application_id,
            ),
            status = status.kizzy_status
        )
    }

    private fun getProxyUrlAttachment(proxy_url: String): String {
        val uri: Uri = Uri.parse(proxy_url)
        return "mp:" + uri.path!!.removePrefix("/") + "?" + uri.query
    }

    @Serializable
    private data class SupabaseGetImagesResponse(val attachment_urls: List<String?>)

    private val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = ProjectBuildConfig.SUPABASE_URL,
            supabaseKey = ProjectBuildConfig.SUPABASE_KEY,
            builder = {
                install(Functions)
            }
        )
    }

    actual suspend fun getCustomImages(
        image_items: List<MediaItem>,
        target_quality: ThumbnailProvider.Quality
    ): Result<List<String?>> {
        // The source code for this function is available at https://github.com/toasterofbread/discordimageindex
        val response: HttpResponse = client.functions.invoke(
            "get-images",
            body = buildJsonObject {
                putJsonArray("images") {
                    for (item in image_items) {
                        add(
                            buildJsonObject {
                                put("id", item.id)

                                val thumbnail_provider: ThumbnailProvider? = item.ThumbnailProvider.get(context.database)
                                put("image_url", thumbnail_provider?.getThumbnailUrl(target_quality))
                            }
                        )
                    }
                }
            },
            headers = Headers.build {
                append(HttpHeaders.ContentType, "application/json")
            }
        )

        if (!response.status.isSuccess()) {
            return Result.failure(IOException(response.status.toString()))
        }

        val parsed: SupabaseGetImagesResponse = response.body()
        return Result.success(
            parsed.attachment_urls.map { attachment ->
                attachment?.let { getProxyUrlAttachment(it) }
            }
        )
    }
}

actual suspend fun getDiscordAccountInfo(
    account_token: String?
): Result<DiscordMeResponse> = runCatching {
    if (account_token == null) {
        throw NullPointerException("account_token is null")
    }

    val response: HttpResponse =
        JsonHttpClient.get("https://discord.com/api/v9/users/@me") {
            headers {
                append("authorization", account_token)
            }
        }

    if (response.status.value !in 200 .. 299) {
        throw IOException(response.status.value.toString())
    }

    val me: DiscordMeResponse = response.body()
    me.token = account_token

    return@runCatching me
}
