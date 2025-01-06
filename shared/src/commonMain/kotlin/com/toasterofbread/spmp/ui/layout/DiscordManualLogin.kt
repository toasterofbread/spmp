package com.toasterofbread.spmp.ui.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import dev.toastbits.composekit.util.platform.launchSingle
import com.toasterofbread.spmp.platform.getDiscordAccountInfo
import androidx.compose.foundation.layout.PaddingValues
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.discord_manual_login_suffix
import spmp.shared.generated.resources.discord_manual_login_field
import spmp.shared.generated.resources.discord_manual_login_steps

private data class DiscordErrorMessage(val message: String?)

@Composable
fun DiscordManualLogin(content_padding: PaddingValues, modifier: Modifier = Modifier, onFinished: (Result<String?>?) -> Unit) {
    val coroutine_scope = rememberCoroutineScope()

    ManualLoginPage(
        steps = stringArrayResource(Res.array.discord_manual_login_steps),
        suffix = stringResource(Res.string.discord_manual_login_suffix),
        entry_label = stringResource(Res.string.discord_manual_login_field),
        modifier,
        content_padding = content_padding
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
                                val parsed: DiscordErrorMessage = Json.decodeFromString(content)
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