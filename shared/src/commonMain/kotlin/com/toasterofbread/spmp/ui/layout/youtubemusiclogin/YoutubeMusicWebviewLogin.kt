package com.toasterofbread.spmp.ui.layout.youtubemusiclogin

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.toastbits.ytmkt.impl.youtubei.YoutubeiApi
import io.ktor.http.Headers

@Composable
internal expect fun YoutubeMusicWebviewLogin(
    api: YoutubeiApi,
    login_url: String,
    modifier: Modifier = Modifier,
    onFinished: (Result<Headers>?) -> Unit
)
