package com.toasterofbread.spmp.model.radio

import dev.toastbits.ytmkt.radio.BuiltInRadioContinuation
import dev.toastbits.ytmkt.radio.RadioContinuation
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json

internal object RadioContinuationSerializer: KSerializer<RadioContinuation> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor(
            "RadioContinuation",
            PrimitiveKind.STRING
        )

    override fun serialize(encoder: Encoder, value: RadioContinuation) {
        encoder.encodeString(
            when (value) {
                is BuiltInRadioContinuation ->
                    Json.encodeToString(value)
                is PlaylistItemsRadioContinuation ->
                    PLAYLIST_ITEMS_RADIO_CONTINUATION_PREFIX + Json.encodeToString(value)
                else -> throw NotImplementedError(value::class.toString())
            }
        )
    }

    override fun deserialize(decoder: Decoder): RadioContinuation {
        val string: String = decoder.decodeString()
        return when (string.firstOrNull()) {
            null -> throw IllegalStateException()
            PLAYLIST_ITEMS_RADIO_CONTINUATION_PREFIX ->
                Json.decodeFromString<PlaylistItemsRadioContinuation>(string.drop(1))
            else ->
                Json.decodeFromString<BuiltInRadioContinuation>(string)
        }
    }

    private const val PLAYLIST_ITEMS_RADIO_CONTINUATION_PREFIX: Char = '!'
}
