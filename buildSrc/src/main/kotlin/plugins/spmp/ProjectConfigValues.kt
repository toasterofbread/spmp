package plugin.spmp

object ProjectConfigValues {
    val CONFIG_VALUES = mapOf(
        "PASTE_EE_TOKEN" to "String",
        "SUPABASE_URL" to "String",
        "SUPABASE_KEY" to "String",
        "DISCORD_APPLICATION_ID" to "String"
    )

    val DEBUG_CONFIG_VALUES = mapOf(
        "YTM_CHANNEL_ID" to "String",
        "YTM_COOKIE" to "String",
        "YTM_HEADERS" to "String",
        "DISCORD_ACCOUNT_TOKEN" to "String",
        "DISCORD_ERROR_REPORT_WEBHOOK" to "String",
        "DISCORD_STATUS_TEXT_NAME_OVERRIDE" to "String",
        "DISCORD_STATUS_TEXT_TEXT_A_OVERRIDE" to "String",
        "DISCORD_STATUS_TEXT_TEXT_B_OVERRIDE" to "String",
        "DISCORD_STATUS_TEXT_TEXT_C_OVERRIDE" to "String",
        "DISCORD_STATUS_TEXT_BUTTON_SONG_OVERRIDE" to "String",
        "DISCORD_STATUS_TEXT_BUTTON_PROJECT_OVERRIDE" to "String",
        "MUTE_PLAYER" to "Boolean",
        "DISABLE_PERSISTENT_QUEUE" to "Boolean",
        "STATUS_WEBHOOK_URL" to "String",
        "STATUS_WEBHOOK_PAYLOAD" to "String",
        "SERVER_PORT" to "Int"
    )
}
