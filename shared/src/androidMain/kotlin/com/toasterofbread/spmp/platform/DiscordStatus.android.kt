package com.toasterofbread.spmp.platform

import android.net.Uri
import com.google.gson.Gson
import com.my.kizzyrpc.KizzyRPC
import com.my.kizzyrpc.model.Activity
import com.my.kizzyrpc.model.Assets
import com.my.kizzyrpc.model.Metadata
import com.my.kizzyrpc.model.Timestamps
import com.toasterofbread.spmp.ProjectBuildConfig
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.model.settings.category.DiscordSettings
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.youtubeapi.executeResult
import com.toasterofbread.spmp.youtubeapi.fromJson
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.cast
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.plugins.standaloneSupabaseModule
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.protobuf.ProtoBuf
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.io.Reader
import java.util.Base64

actual class DiscordStatus actual constructor(
    private val context: AppContext,
    private val application_id: String,
    private val account_token: String?
) {
    actual companion object {
        actual fun isSupported(): Boolean = true
        actual fun isAccountTokenRequired(): Boolean = true
        actual fun getWarningText(): String? = getString("warning_discord_kizzy")
    }

    private val rpc: KizzyRPC

    init {
        if (account_token.isNullOrBlank()) {
            throw IllegalArgumentException("Account token is required")
        }
        rpc = KizzyRPC(account_token, loggingEnabled = false)
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

    private data class ProtoSettingsResponse(
        val settings: String
    )

    actual suspend fun shouldUpdateStatus(): Boolean = withContext(Dispatchers.IO) {
        if (account_token == null) {
            return@withContext true
        }

        val request: Request = Request.Builder()
            .url("https://discord.com/api/v9/users/@me/settings-proto/1")
            .addHeader("authorization", account_token)
            .build()

        val response: Response = OkHttpClient().executeResult(request).getOrElse {
            return@withContext true
        }

        val result: ProtoSettingsResponse =
            try {
                response.use {
                    Gson().fromJson(it.body!!.charStream())
                }
            }
            catch (e: Throwable) {
                return@withContext true
            }

        val settings: PreloadedUserSettings =
            try {
                val bytes = Base64.getDecoder().decode(result.settings)
                ProtoBuf.decodeFromByteArray(bytes)
            }
            catch (e: Throwable) {
                return@withContext true
            }

        val disable: Boolean = when (settings.getStatus()) {
            "invisible" -> DiscordSettings.Key.STATUS_DISABLE_WHEN_INVISIBLE.get(context)
            "dnd" -> DiscordSettings.Key.STATUS_DISABLE_WHEN_DND.get(context)
            "idle" -> DiscordSettings.Key.STATUS_DISABLE_WHEN_IDLE.get(context)
            "offline" -> DiscordSettings.Key.STATUS_DISABLE_WHEN_OFFLINE.get(context)
            "online" -> DiscordSettings.Key.STATUS_DISABLE_WHEN_ONLINE.get(context)
            else -> throw NotImplementedError(settings.getStatus())
        }

        return@withContext !disable
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
        rpc.setActivity(
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

    private fun getSupabaseFunctions(): Functions =
        standaloneSupabaseModule(
            Functions,
            ProjectBuildConfig.SUPABASE_URL + "/functions/v1/",
            ProjectBuildConfig.SUPABASE_KEY
        )

    actual suspend fun getCustomImages(
        image_items: List<MediaItem>,
        target_quality: MediaItemThumbnailProvider.Quality
    ): Result<List<String?>> {
        val supabase_functions: Functions = getSupabaseFunctions()

        // The source code for this function is available at https://github.com/toasterofbread/discordimageindex
        val response: HttpResponse = supabase_functions.invoke(
            "get-images",
            body = buildJsonObject {
                putJsonArray("images") {
                    for (item in image_items) {
                        add(
                            buildJsonObject {
                                put("id", item.id)

                                val thumbnail_provider: MediaItemThumbnailProvider? = item.ThumbnailProvider.get(context.database)
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

actual suspend fun getDiscordAccountInfo(account_token: String?): Result<DiscordMeResponse> = withContext(Dispatchers.IO) {
    if (account_token == null) {
        return@withContext Result.failure(NullPointerException("account_token is null"))
    }

    val request: Request = Request.Builder()
        .url("https://discord.com/api/v9/users/@me")
        .addHeader("authorization", account_token)
        .build()

    val result: Result<Response> = OkHttpClient().executeResult(request)
    val response: Response = result.getOrNull() ?: return@withContext result.cast()

    val stream: Reader = response.body!!.charStream()
    val me: DiscordMeResponse = try {
        Gson().fromJson(stream)
    }
    catch (e: Throwable) {
        return@withContext Result.failure(e)
    }
    finally {
        stream.close()
    }
    me.token = account_token

    return@withContext Result.success(me)
}
