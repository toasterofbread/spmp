package com.toasterofbread.spmp.platform

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import com.google.gson.Gson
import com.my.kizzyrpc.KizzyRPC
import com.my.kizzyrpc.model.Activity
import com.my.kizzyrpc.model.Assets
import com.my.kizzyrpc.model.Metadata
import com.my.kizzyrpc.model.Timestamps
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.youtubeapi.executeResult
import com.toasterofbread.spmp.youtubeapi.fromJson
import com.toasterofbread.utils.common.indexOfFirstOrNull
import com.toasterofbread.utils.common.indexOfOrNull
import dev.kord.common.entity.DiscordMessage
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createTextChannel
import dev.kord.core.cache.data.toData
import dev.kord.core.entity.channel.Category
import dev.kord.core.exception.KordInitializationException
import dev.kord.rest.NamedFile
import dev.kord.rest.builder.channel.TextChannelCreateBuilder
import dev.kord.rest.route.Position
import dev.kord.rest.service.ChannelService
import dev.kord.rest.service.createTextChannel
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.request.forms.ChannelProvider
import io.ktor.utils.io.jvm.javaio.toByteReadChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.Base64

private const val RATE_LIMIT_ADDITIONAL_WAIT_MS: Long = 1000

actual class DiscordStatus actual constructor(
    private val bot_token: String?,
    private val guild_id: Long?,
    private val custom_images_channel_category_id: Long?,
    private val custom_images_channel_name_prefix: String,
    private val account_token: String?
) {
    actual companion object {
        actual fun isSupported(): Boolean = true
        actual fun isAccountTokenRequired(): Boolean = true
    }

    private val rpc: KizzyRPC

    init {
        if (account_token.isNullOrBlank()) {
            throw IllegalArgumentException("Account token is required")
        }
        if (guild_id == null && custom_images_channel_category_id == null) {
            throw IllegalArgumentException("At least one of guild_id and custom_images_channel_category_id required")
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

    actual suspend fun shouldUpdateStatus(context: PlatformContext): Boolean = withContext(Dispatchers.IO) {
        if (account_token == null) {
            return@withContext true
        }

        val request = Request.Builder()
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
            "invisible" -> Settings.KEY_DISCORD_STATUS_DISABLE_WHEN_INVISIBLE.get(context)
            "dnd" -> Settings.KEY_DISCORD_STATUS_DISABLE_WHEN_DND.get(context)
            "idle" -> Settings.KEY_DISCORD_STATUS_DISABLE_WHEN_IDLE.get(context)
            "offline" -> Settings.KEY_DISCORD_STATUS_DISABLE_WHEN_OFFLINE.get(context)
            "online" -> Settings.KEY_DISCORD_STATUS_DISABLE_WHEN_ONLINE.get(context)
            else -> throw NotImplementedError(settings.getStatus())
        }

        return@withContext !disable
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

    private fun getProxyUrlAttachment(proxy_url: String): String = "mp:" + Uri.parse(proxy_url).path!!.removePrefix("/")

    private suspend fun ChannelService.firstMessageOrNull(channel_id: Snowflake, predicate: (DiscordMessage) -> Boolean): DiscordMessage? {
        var position: Position? = null
        while (true) {
            val messages = getMessages(channel_id, position)
            if (messages.isEmpty()) {
                return null
            }

            var last_message: DiscordMessage = messages.first()
            for (message in messages) {
                if (predicate(message)) {
                    return message
                }

                if (last_message.timestamp < message.timestamp) {
                    last_message = message
                }
            }

            if (position?.value == last_message.id) {
                return null
            }
            position = Position.After(last_message.id)
        }
    }

    private fun getChannelNameFor(first_char: Char): String {
        return custom_images_channel_name_prefix + when (first_char) {
            '-' -> "hyphen"
            else -> first_char
        }
    }

    private suspend fun getKord(token: String): Result<Kord> {
        var kord: Kord
        try {
            kord = Kord(token)
        }
        // Handle Discord rate limit
        catch (e: KordInitializationException) {
            val message = e.message ?: throw e

            val start = (message.indexOfOrNull("retry_after") ?: throw e) + 14
            val end = message.indexOfFirstOrNull(start) { !it.isDigit() && it != '.' } ?: throw NotImplementedError(message)

            val retry_after = message.substring(start, end).toFloatOrNull() ?: throw NotImplementedError(message)
            delay((retry_after * 1000L).toLong() + RATE_LIMIT_ADDITIONAL_WAIT_MS)

            try {
                kord = Kord(token)
            }
            catch (e: Throwable) {
                return Result.failure(e)
            }
        }
        catch (e: ConnectTimeoutException) {
            return Result.failure(IOException(e))
        }

        return Result.success(kord)
    }

    actual suspend fun getCustomImage(unique_id: String, imageProvider: suspend () -> ImageBitmap?): Result<String?> {
        check(bot_token != null)

        val kord: Kord = getKord(bot_token).getOrElse {
            return Result.failure(it)
        }

        val result = with(kord.rest.channel) {
            val channel_name = getChannelNameFor(unique_id.first())

            var channel: Snowflake?
            val category: Category?

            // Get existing channel from category or guild
            if (custom_images_channel_category_id != null) {
                category = Category(getChannel(Snowflake(custom_images_channel_category_id)).toData(), kord)
                channel = category.channels.firstOrNull {
                    it.name.equals(channel_name, true)
                }?.id
            }
            else {
                check(guild_id != null)
                category = null
                channel = kord.defaultSupplier.getGuildChannels(Snowflake(guild_id)).firstOrNull { it.name == channel_name }?.id
            }

            // Find matching image from existing channel
            if (channel != null) {
                val message = firstMessageOrNull(channel) { message ->
                    message.author.id == kord.selfId && message.content == unique_id
                }
                if (message != null) {
                    return@with getProxyUrlAttachment(message.attachments.first().proxyUrl)
                }
            }

            // Get image from caller
            val image = imageProvider() ?: return@with null

            // Create new channel if needed
            if (channel == null) {
                val channel_builder: TextChannelCreateBuilder.() -> Unit = {}
                channel =
                    if (category != null) category.createTextChannel(channel_name, channel_builder).id
                    else kord.rest.guild.createTextChannel(Snowflake(guild_id!!), channel_name, channel_builder).id
            }

            // Upload image data to channel
            val message = createMessage(channel) {
                content = unique_id

                val stream = ByteArrayOutputStream()
                image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, stream)

                val bytes = stream.toByteArray()
                files.add(NamedFile("$unique_id.png", ChannelProvider(bytes.size.toLong(), { bytes.inputStream().toByteReadChannel() })))
            }

            return@with getProxyUrlAttachment(message.attachments.first().proxyUrl)
        }

        kord.shutdown()
        return Result.success(result)
    }
}
