package com.toasterofbread.spmp.platform.playerservice

import com.toasterofbread.spmp.resources.getString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

const val SERVER_EXPECT_REPLY_CHAR: Char = '!'
const val SERVER_MESSAGE_MAX_SIZE: Int = 300

@Serializable
data class SpMsActionReply(val success: Boolean, val error: String? = null, val error_cause: String? = null, val result: JsonElement? = null)

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
    val machine_id: String,
    val language: String? = null,
    val player_port: Int? = null
)

@Serializable
internal data class SpMsServerHandshake(
    val name: String,
    val device_name: String,
    val spms_commit_hash: String,
    val server_state: SpMsServerState,
    val machine_id: String
)

@Serializable
data class SpMsClientInfo(
    val name: String,
    val type: SpMsClientType,
    val machine_id: String,
    val language: String,
    val is_caller: Boolean = false,
    val player_port: Int? = null
)

enum class SpMsClientType {
    SPMP_PLAYER, SPMP_STANDALONE, PLAYER, COMMAND_LINE, SERVER;
    
    fun getName(): String =
        when (this) {
            SPMP_PLAYER -> getString("spms_client_type_spmp_player")
            SPMP_STANDALONE -> getString("spms_client_type_spmp_standalone")
            PLAYER -> getString("spms_client_type_player")
            COMMAND_LINE -> getString("spms_client_type_command_line")
            SERVER -> getString("spms_client_type_server")
        }
    
    fun getInfoText(): String =
        when (this) {
            SPMP_PLAYER -> getString("spms_client_type_info_spmp_player")
            SPMP_STANDALONE -> getString("spms_client_type_info_spmp_standalone")
            PLAYER -> getString("spms_client_type_info_player")
            COMMAND_LINE -> getString("spms_client_type_info_command_line")
            SERVER -> getString("spms_client_type_info_server")
        }
    
    fun getInfoUrl(): String =
        when (this) {
            SPMP_PLAYER -> getString("spms_client_type_info_url_spmp_player")
            SPMP_STANDALONE -> getString("spms_client_type_info_url_spmp_standalone")
            PLAYER -> getString("spms_client_type_info_url_player")
            COMMAND_LINE -> getString("spms_client_type_info_url_command_line")
            SERVER -> getString("spms_client_type_info_url_server")
        }
}

expect fun getSpMsMachineId(): String

expect fun getServerExecutableFilename(): String?
