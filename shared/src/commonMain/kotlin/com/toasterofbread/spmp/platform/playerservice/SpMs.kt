package com.toasterofbread.spmp.platform.playerservice

import com.toasterofbread.spmp.resources.getString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.hostOs
import java.io.File
import java.lang.System.getenv

const val SERVER_EXPECT_REPLY_CHAR: Char = '!'

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
    val server_state: SpMsServerState
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

fun getSpMsMachineId(): String {
    val id_file: File =
        when (hostOs) {
            OS.Linux -> File("/tmp/")
            OS.Windows -> File("${getenv("USERPROFILE")!!}/AppData/Local/")
            else -> throw NotImplementedError(hostOs.name)
        }.resolve("spmp_machine_id.txt")

    if (id_file.exists()) {
        return id_file.readText()
    }

    if (!id_file.parentFile.exists()) {
        id_file.parentFile.mkdirs()
    }

    val id_length: Int = 8
    val allowed_chars: List<Char> = ('A'..'Z') + ('a'..'z') + ('0'..'9')

    val new_id: String = (1..id_length).map { allowed_chars.random() }.joinToString("")

    id_file.writeText(new_id)

    return new_id
}
