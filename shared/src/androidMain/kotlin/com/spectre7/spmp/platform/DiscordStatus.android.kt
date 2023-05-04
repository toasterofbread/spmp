package com.spectre7.spmp.platform

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import com.my.kizzyrpc.KizzyRPC
import com.my.kizzyrpc.model.Activity
import com.my.kizzyrpc.model.Assets
import com.my.kizzyrpc.model.Metadata
import com.my.kizzyrpc.model.Timestamps
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.rest.NamedFile
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.service.ChannelService
import io.ktor.client.request.forms.*
import io.ktor.utils.io.jvm.javaio.*
import java.io.ByteArrayOutputStream

actual class DiscordStatus actual constructor(
    private val bot_token: String?,
    private val custom_images_channel_id: Long?,
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

    actual suspend fun getCustomImage(unique_id: String, imageProvider: () -> ImageBitmap?): String? {
        check(bot_token != null)
        check(custom_images_channel_id != null)

        val kord = Kord(bot_token)
        val result = with(kord.rest.channel) {
            val channel = Snowflake(custom_images_channel_id)

            val messages = getMessages(channel)
            for (message in messages) {
                if (message.author.id != kord.selfId) {
                    continue
                }

                if (message.content == unique_id) {
                    return@with getProxyUrlAttachment(message.attachments.first().proxyUrl)
                }
            }

            val image = imageProvider() ?: return null

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
        return result
    }
}
