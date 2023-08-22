package com.toasterofbread.spmp.ui.layout

import LocalPlayerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.resources.getStringArray
import com.toasterofbread.utils.launchSingle

@Composable
fun DiscordManualLogin(modifier: Modifier = Modifier, onFinished: (Result<String?>?) -> Unit) {
    val coroutine_scope = rememberCoroutineScope()
    val player = LocalPlayerState.current

    ManualLoginPage(
        steps = getStringArray("discord_manual_login_steps"),
        suffix = getString("discord_manual_login_suffix"),
        entry_label = getString("discord_manual_login_field"),
        modifier
    ) { entry ->
        if (entry == null) {
            onFinished(Result.success(null))
            return@ManualLoginPage null
        }

        coroutine_scope.launchSingle {
            onFinished(
                getDiscordAccountInfo(entry).fold(
                    { Result.success(entry) },
                    { error ->
                        val content = error.message
                        if (content != null) {
                            try {
                                val parsed = player.context.ytapi.klaxon.parseJsonObject(content.reader())
                                val message = parsed["message"] as String?
                                if (message != null) {
                                    return@fold Result.failure(RuntimeException(message))
                                }
                            }
                            catch (_: Throwable) {}
                        }
                        return@fold Result.failure(error)
                    }
                )
            )
        }

        return@ManualLoginPage null
    }
}