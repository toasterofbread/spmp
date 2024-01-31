@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package com.toasterofbread.spmp.ui.layout.apppage.mainpage

import LocalPlayerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.toasterofbread.composekit.utils.common.bitmapResource
import com.toasterofbread.composekit.utils.common.blockGestures
import com.toasterofbread.composekit.utils.common.thenIf
import com.toasterofbread.composekit.utils.common.toFloat
import com.toasterofbread.spmp.platform.splash.SplashExtraLoadingContent
import com.toasterofbread.spmp.resources.getString
import kotlinx.coroutines.delay

private const val MESSAGE_DELAY: Long = 2000L
enum class SplashMode {
    SPLASH, WARNING
}

@Composable
fun LoadingSplashView(
    splash_mode: SplashMode?,
    loading_message: String?,
    modifier: Modifier = Modifier,
    server_executable_path: String? = null
) {
    val player: PlayerState = LocalPlayerState.current

    var show_message: Boolean by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(MESSAGE_DELAY)
        show_message = true
    }

    Crossfade(splash_mode, modifier) { mode ->
        when (mode) {
            null -> {}
            SplashMode.SPLASH -> {
                val image: ImageBitmap = bitmapResource("assets/drawable/ic_splash.png")

                Column(
                    Modifier.fillMaxSize().background(player.theme.background).padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)
                ) {
                    var launched: Boolean by remember { mutableStateOf(false) }
                    val image_alpha: Float by animateFloatAsState(if (launched) 1f else 0f, tween(2000))

                    LaunchedEffect(Unit) {
                        launched = true
                    }

                    Box(
                        Modifier
                            .size(
                                with(LocalDensity.current) {
                                    minOf(image.width.toDp(), image.height.toDp(), 200.dp)
                                }
                            )
                            .weight(1f, false)
                            .alpha(image_alpha)
                            .drawWithContent {
                                drawIntoCanvas { canvas ->
                                    val first_filter: ColorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
                                    val second_filter: ColorFilter = ColorFilter.tint(player.theme.accent, BlendMode.Modulate)

                                    canvas.saveLayer(
                                        Rect(0f, 0f, size.width, size.height),
                                        Paint().apply {
                                            colorFilter = second_filter
                                        }
                                    )
                                    drawImage(
                                        image,
                                        dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                                        colorFilter = first_filter
                                    )
                                    canvas.restore()
                                }
                            }
                    )

                    AnimatedVisibility(show_message) {
                        Column(
                            Modifier.width(500.dp).animateContentSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (loading_message != null) {
                                Text(loading_message, Modifier.padding(horizontal = 20.dp), color = player.theme.on_background)
                            }
                            LinearProgressIndicator(Modifier.fillMaxWidth(), color = player.theme.accent)
                        }
                    }

                    val extra_content_alpha: Float by animateFloatAsState(show_message.toFloat())
                    SplashExtraLoadingContent(
                        Modifier
                            .thenIf(!show_message) {
                                blockGestures()
                            }
                            .graphicsLayer { alpha = extra_content_alpha },
                        server_executable_path = server_executable_path
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
