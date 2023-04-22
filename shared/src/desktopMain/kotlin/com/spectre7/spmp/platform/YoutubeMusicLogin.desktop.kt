package com.spectre7.spmp.platform

import SpMp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.spectre7.spmp.model.YoutubeMusicAuthInfo

@Composable
actual fun YoutubeMusicLogin(modifier: Modifier, onFinished: (Result<YoutubeMusicAuthInfo>) -> Unit) {
    // TODO
    SpMp.context.openUrl(YOUTUBE_MUSIC_LOGIN_URL)
}