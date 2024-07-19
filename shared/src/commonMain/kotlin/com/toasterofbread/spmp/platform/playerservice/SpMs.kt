package com.toasterofbread.spmp.platform.playerservice

import com.toasterofbread.spmp.resources.getString
import dev.toastbits.spms.socketapi.shared.SpMsClientType
import dev.toastbits.composekit.platform.PlatformFile
import com.toasterofbread.spmp.platform.AppContext
import okio.buffer
import okio.use

fun SpMsClientType.getName(): String =
    when (this) {
        SpMsClientType.SPMP_PLAYER -> getString("spms_client_type_spmp_player")
        SpMsClientType.SPMP_STANDALONE -> getString("spms_client_type_spmp_standalone")
        SpMsClientType.PLAYER -> getString("spms_client_type_player")
        SpMsClientType.COMMAND_LINE,
        SpMsClientType.COMMAND_LINE_ACTION -> getString("spms_client_type_command_line")
        SpMsClientType.SERVER -> getString("spms_client_type_server")
    }

fun SpMsClientType.getInfoText(): String =
    when (this) {
        SpMsClientType.SPMP_PLAYER -> getString("spms_client_type_info_spmp_player")
        SpMsClientType.SPMP_STANDALONE -> getString("spms_client_type_info_spmp_standalone")
        SpMsClientType.PLAYER -> getString("spms_client_type_info_player")
        SpMsClientType.COMMAND_LINE, SpMsClientType.COMMAND_LINE_ACTION -> getString("spms_client_type_info_command_line")
        SpMsClientType.SERVER -> getString("spms_client_type_info_server")
    }

fun SpMsClientType.getInfoUrl(): String =
    when (this) {
        SpMsClientType.SPMP_PLAYER -> getString("spms_client_type_info_url_spmp_player")
        SpMsClientType.SPMP_STANDALONE -> getString("spms_client_type_info_url_spmp_standalone")
        SpMsClientType.PLAYER -> getString("spms_client_type_info_url_player")
        SpMsClientType.COMMAND_LINE,
        SpMsClientType.COMMAND_LINE_ACTION -> getString("spms_client_type_info_url_command_line")
        SpMsClientType.SERVER -> getString("spms_client_type_info_url_server")
    }

expect fun getSpMsMachineId(context: AppContext): String

internal fun getSpMsMachineIdFromFile(id_file: PlatformFile): String {
    if (id_file.exists) {
        return id_file.inputStream().buffer().use { stream ->
            stream.readUtf8()
        }
    }

    if (!id_file.parent_file.exists) {
        check(id_file.parent_file.mkdirs()) { id_file }
    }

    val id_length: Int = 8
    val allowed_chars: List<Char> = ('A'..'Z') + ('a'..'z') + ('0'..'9')

    val new_id: String = (1..id_length).map { allowed_chars.random() }.joinToString("")

    id_file.outputStream().use { stream->
        stream.buffer().writeUtf8(new_id)
    }

    return new_id
}
