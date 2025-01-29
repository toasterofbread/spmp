package com.toasterofbread.spmp.platform.playerservice

import androidx.compose.runtime.Composable
import dev.toastbits.spms.socketapi.shared.SpMsClientType
import dev.toastbits.composekit.context.PlatformFile
import com.toasterofbread.spmp.platform.AppContext
import okio.buffer
import okio.use
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.spms_client_type_spmp_player
import spmp.shared.generated.resources.spms_client_type_spmp_standalone
import spmp.shared.generated.resources.spms_client_type_player
import spmp.shared.generated.resources.spms_client_type_command_line
import spmp.shared.generated.resources.spms_client_type_server
import spmp.shared.generated.resources.spms_client_type_info_spmp_player
import spmp.shared.generated.resources.spms_client_type_info_spmp_standalone
import spmp.shared.generated.resources.spms_client_type_info_player
import spmp.shared.generated.resources.spms_client_type_info_command_line
import spmp.shared.generated.resources.spms_client_type_info_server
import spmp.shared.generated.resources.spms_client_type_info_url_spmp_player
import spmp.shared.generated.resources.spms_client_type_info_url_spmp_standalone
import spmp.shared.generated.resources.spms_client_type_info_url_player
import spmp.shared.generated.resources.spms_client_type_info_url_command_line
import spmp.shared.generated.resources.spms_client_type_info_url_server

@Composable
fun SpMsClientType.getName(): String =
    when (this) {
        SpMsClientType.SPMP_PLAYER -> stringResource(Res.string.spms_client_type_spmp_player)
        SpMsClientType.SPMP_STANDALONE -> stringResource(Res.string.spms_client_type_spmp_standalone)
        SpMsClientType.PLAYER -> stringResource(Res.string.spms_client_type_player)
        SpMsClientType.COMMAND_LINE,
        SpMsClientType.COMMAND_LINE_ACTION -> stringResource(Res.string.spms_client_type_command_line)
        SpMsClientType.SERVER -> stringResource(Res.string.spms_client_type_server)
    }

@Composable
fun SpMsClientType.getInfoText(): String =
    when (this) {
        SpMsClientType.SPMP_PLAYER -> stringResource(Res.string.spms_client_type_info_spmp_player)
        SpMsClientType.SPMP_STANDALONE -> stringResource(Res.string.spms_client_type_info_spmp_standalone)
        SpMsClientType.PLAYER -> stringResource(Res.string.spms_client_type_info_player)
        SpMsClientType.COMMAND_LINE, SpMsClientType.COMMAND_LINE_ACTION -> stringResource(Res.string.spms_client_type_info_command_line)
        SpMsClientType.SERVER -> stringResource(Res.string.spms_client_type_info_server)
    }

@Composable
fun SpMsClientType.getInfoUrl(): String =
    when (this) {
        SpMsClientType.SPMP_PLAYER -> stringResource(Res.string.spms_client_type_info_url_spmp_player)
        SpMsClientType.SPMP_STANDALONE -> stringResource(Res.string.spms_client_type_info_url_spmp_standalone)
        SpMsClientType.PLAYER -> stringResource(Res.string.spms_client_type_info_url_player)
        SpMsClientType.COMMAND_LINE,
        SpMsClientType.COMMAND_LINE_ACTION -> stringResource(Res.string.spms_client_type_info_url_command_line)
        SpMsClientType.SERVER -> stringResource(Res.string.spms_client_type_info_url_server)
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

    val new_id: String = generateNewSpMsMachineId()
    id_file.outputStream().use { stream->
        stream.buffer().writeUtf8(new_id)
    }

    return new_id
}

internal fun generateNewSpMsMachineId(): String {
    val id_length: Int = 8
    val allowed_chars: List<Char> = ('A'..'Z') + ('a'..'z') + ('0'..'9')
    return (1..id_length).map { allowed_chars.random() }.joinToString("")
}
