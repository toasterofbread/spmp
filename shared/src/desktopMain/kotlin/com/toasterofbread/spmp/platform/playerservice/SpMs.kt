package com.toasterofbread.spmp.platform.playerservice

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

const val SERVER_EXPECT_REPLY_CHAR: Char = '!'

@Serializable
internal data class SpMsPlayerEvent(val type: Type, val properties: Map<String, JsonPrimitive> = emptyMap()) {
    enum class Type {
        ITEM_TRANSITION,
        PROPERTY_CHANGED,
        SEEKED,
        ITEM_ADDED,
        ITEM_REMOVED,
        ITEM_MOVED,
        CLEARED,
        READY_TO_PLAY
    }
}

@Serializable
internal data class SpMsServerState(
    val queue: List<String>,
    val state: MediaPlayerState,
    val is_playing: Boolean,
    val current_item_index: Int,
    val current_position_ms: Int,
    val duration_ms: Int,
    val repeat_mode: MediaPlayerRepeatMode,
    val volume: Float
)

@Serializable
internal data class SpMsClientHandshake(
    val name: String,
    val type: SpMsClientType,
    val language: String
)

@Serializable
internal data class SpMsClientInfo(
    val name: String,
    val type: SpMsClientType,
    val language: String
)

internal enum class SpMsClientType {
    PLAYER, HEADLESS_PLAYER
}
