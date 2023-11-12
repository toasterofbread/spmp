@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package com.toasterofbread.spmp.ui.layout.apppage.mainpage

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.composekit.settings.ui.Theme
import com.toasterofbread.composekit.utils.common.bitmapResource
import kotlinx.coroutines.delay

private const val WARNING_DELAY: Long = 5000L
private enum class SplashMode {
    SPLASH, WARNING
}

@Composable
fun LoadingSplashView(modifier: Modifier = Modifier) {
    var splash_mode: SplashMode? by remember { mutableStateOf(null) }
    val player = LocalPlayerState.current

    Crossfade(splash_mode, modifier) { mode ->
        when (mode) {
            null -> {}
            SplashMode.SPLASH -> {
                val image: ImageBitmap = bitmapResource("drawable/ic_splash.png")
                val background_colour: Color = remember(image) {
                    val first_pixel = IntArray(1)
                    image.readPixels(first_pixel, 0, 0, 1, 1)

                    Color(first_pixel[0])
                }

                Column(
                    Modifier.fillMaxSize().background(background_colour),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        image,
                        null,
                        Modifier
                            .size(with(LocalDensity.current) { 450.toDp() })
                            .clip(CircleShape),
                    )
                }
            }
            SplashMode.WARNING -> {
                Column(
                    Modifier.fillMaxSize().background(player.theme.background),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)
                ) {
                    CompositionLocalProvider(LocalContentColor provides player.theme.on_background) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Warning, null, tint = player.theme.on_background)
                            Text(getString("error_player_service_not_connected"), color = player.theme.on_background)
                        }

                        if (player.context.canOpenUrl()) {
                            Button(
                                {
                                    player.context.openUrl(getString("report_issue_url"))
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = player.theme.accent,
                                    contentColor = player.theme.on_accent
                                )
                            ) {
                                Text(getString("report_error"))
                            }
                        }
                    }
                }
            }
        }
    }
}
