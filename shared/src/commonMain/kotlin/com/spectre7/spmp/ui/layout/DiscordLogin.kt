package com.spectre7.spmp.ui.layout

private const val DISCORD_URL = "https://discord.com/"
private const val DISCORD_LOGIN_URL = "https://discord.com/login"

@Composable
fun DiscordLogin(modifier: Modifier = Modifier) {
    var showing_warning: Boolean? = remember { mutableStateOf(!Settings.CHOICE_ACCEPT_DISCORD_LOGIN_WARNING.get<Boolean>()) }
    when (showing_warning) {
        true -> {
            PlatformAlertDialog(
                { 
                    show_reset_confirmation = false
                    Settings.CHOICE_ACCEPT_DISCORD_LOGIN_WARNING.set(true)
                },
                confirmButton = {
                    FilledTonalButton({ showing_warning = false }) {
                        Text(getString("action_confirm_action"))
                    }
                },
                dismissButton = { TextButton({ showing_warning = null }) { Text(getString("action_deny_action")) } },
                title = { Text(getString("prompt_confirm_action")) },
                text = {
                    Text(getString("warning_discord_login"))
                }
            )
        }
        false -> {
            if (isWebViewLoginSupported()) {
                WebViewLogin(DISCORD_URL, modifier, { !it.startsWith(DISCORD_URL) }) { request, openUrl ->
                
                }
            }
            else {
                // TODO
                SpMp.context.openUrl(DISCORD_LOGIN_URL)
            }
        }
        null -> {

        }
    }
}
