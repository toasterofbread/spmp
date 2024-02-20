package com.toasterofbread.spmp.platform.playerservice

import com.toasterofbread.spmp.resources.getString
import spms.socketapi.shared.SpMsClientType

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

expect fun getSpMsMachineId(): String

expect fun getServerExecutableFilename(): String?
