package com.toasterofbread.spmp.platform

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import com.my.kizzyrpc.KizzyRPC
import com.my.kizzyrpc.model.Activity
import com.my.kizzyrpc.model.Assets
import com.my.kizzyrpc.model.Metadata
import com.my.kizzyrpc.model.Timestamps
import com.toasterofbread.utils.indexOfFirstOrNull
import com.toasterofbread.utils.indexOfOrNull
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
import io.ktor.client.network.sockets.*
import io.ktor.client.request.forms.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import java.io.ByteArrayOutputStream
import java.io.IOException

private const val RATE_LIMIT_ADDITIONAL_WAIT_MS: Long = 1000

actual class DiscordStatus actual constructor(
    private val bot_token: String?,
    private val guild_id: Long?,
    private val custom_images_channel_category_id: Long?,
    private val custom_images_channel_name_prefix: String,
    account_token: String?
) {
    actual companion object {
        actual fun isSupported(): Boolean = true
        actual fun isAccountTokenRequired(): Boolean = true
    }

    private val rpc: KizzyRPC

    init {
        if (account_token == null || account_token.isBlank()) {
            throw IllegalArgumentException("Account token is required")
        }
        if (guild_id == null && custom_images_channel_category_id == null) {
            throw IllegalArgumentException("At least one of guild_id and custom_images_channel_category_id required")
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
            for (message in messages) {
                if (predicate(message)) {
                    return message
                }
            }
            position = Position.After(messages.last().id)
        }
    }

    private fun getChannelNameFor(first_char: Char): String {
        return custom_images_channel_name_prefix + when (first_char) {
            '-' -> "hyphen"
            else -> first_char
        }
    }

    @Suppress("UNUSED_VALUE")
    actual suspend fun getCustomImage(unique_id: String, imageProvider: suspend () -> ImageBitmap?): Result<String?> {
        check(bot_token != null)

        var kord: Kord
        try {
            kord = Kord(bot_token)
        }
        // Handle Discord rate limit
        catch (e: KordInitializationException) {
            val message = e.message ?: throw e

            val start = (message.indexOfOrNull("retry_after") ?: throw e) + 14
            val end = message.indexOfFirstOrNull(start) { !it.isDigit() && it != '.' } ?: throw NotImplementedError(message)

            val retry_after = message.substring(start, end).toFloatOrNull() ?: throw NotImplementedError(message)
            delay((retry_after * 1000L).toLong() + RATE_LIMIT_ADDITIONAL_WAIT_MS)

            kord = Kord(bot_token)
        }
        catch (e: ConnectTimeoutException) {
            return Result.failure(IOException(e))
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
