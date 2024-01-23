package com.toasterofbread.spmp.ui.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.google.gson.Gson
import com.toasterofbread.composekit.utils.common.launchSingle
import com.toasterofbread.spmp.platform.getDiscordAccountInfo
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.resources.getStringArray
import com.toasterofbread.spmp.youtubeapi.fromJson

private data class DiscordErrorMessage(val message: String?)

@Composable
fun DiscordManualLogin(modifier: Modifier = Modifier, onFinished: (Result<String?>?) -> Unit) {
    val coroutine_scope = rememberCoroutineScope()

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
                                val parsed: DiscordErrorMessage = Gson().fromJson(content)
                                if (parsed.message != null) {
                                    return@fold Result.failure(RuntimeException(parsed.message))
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