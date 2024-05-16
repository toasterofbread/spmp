@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package com.toasterofbread.spmp.ui.layout.loadingsplash

import LocalPlayerState
import SpMp.isDebugBuild
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.toasterofbread.spmp.platform.playerservice.PlayerServiceLoadState
import com.toasterofbread.spmp.platform.playerservice.PlayerServiceCompanion
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.ErrorInfoDisplay
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.*
import spmp.shared.generated.resources.*
import dev.toastbits.composekit.utils.common.toFloat
import dev.toastbits.composekit.utils.common.thenIf
import dev.toastbits.composekit.utils.common.blockGestures
import dev.toastbits.composekit.utils.composable.NullableValueAnimatedVisibility
import dev.toastbits.composekit.utils.composable.wave.OverlappingWaves
import dev.toastbits.composekit.utils.composable.wave.getDefaultOverlappingWavesLayers
import dev.toastbits.composekit.utils.composable.wave.WaveLayer

private const val MESSAGE_DISPLAY_DELAY: Long = 1000L
enum class SplashMode {
    SPLASH, WARNING
}

@Composable
fun LoadingSplash(
    splash_mode: SplashMode?,
    load_state: PlayerServiceLoadState?,
    requestServiceChange: (PlayerServiceCompanion) -> Unit,
    modifier: Modifier = Modifier,
    content_padding: PaddingValues = PaddingValues(),
) {
    val player: PlayerState = LocalPlayerState.current

    var show_message: Boolean by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(MESSAGE_DISPLAY_DELAY)
        show_message = true
    }

    BoxWithConstraints(modifier.background(player.theme.background)) {
        val wave_layers: List<WaveLayer> = remember {
            getDefaultOverlappingWavesLayers(7, 0.35f)
        }
        val waves_height: Dp = (maxHeight * 0.3f).coerceAtLeast(100.dp)

        OverlappingWaves(
            { player.theme.accent.copy(alpha = 0.2f) },
            BlendMode.Screen,
            modifier
                .fillMaxWidth(1f)
                .requiredHeight(waves_height)
                .offset(y = (maxHeight - waves_height) / 2)
                .align(Alignment.BottomCenter),
            layers = wave_layers,
            speed = 0.3f
        )

        Crossfade(splash_mode, Modifier.padding(content_padding)) { mode ->
            when (mode) {
                null -> {}
                SplashMode.SPLASH -> {
                    val image: ImageBitmap = imageResource(Res.drawable.ic_splash)

                    FlowRow(
                        Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)
                    ) {
                        var launched: Boolean by remember { mutableStateOf(false) }
                        val image_alpha: Float by animateFloatAsState(if (launched) 1f else 0f, tween(2000))

                        LaunchedEffect(Unit) {
                            launched = true
                        }

                        Box(
                            Modifier
                                .align(Alignment.CenterVertically)
                                .size(250.dp)
                                .aspectRatio(1f)
                                // .weight(1f, false)
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

                        AnimatedVisibility(
                            show_message,
                            Modifier.align(Alignment.CenterVertically)
                        ) {
                            Column(
                                Modifier.width(IntrinsicSize.Max),
                                verticalArrangement = Arrangement.spacedBy(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Column(
                                    Modifier.animateContentSize(),
                                    verticalArrangement = Arrangement.spacedBy(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    if (load_state?.error != null) {
                                        ErrorInfoDisplay(
                                            load_state.error,
                                            isDebugBuild(),
                                            onDismiss = null,
                                            expanded_content_modifier = Modifier.height(300.dp)
                                        )
                                    }
                                    else {
                                        if (load_state?.loading_message != null) {
                                            Text(
                                                load_state.loading_message,
                                                color = player.theme.on_background,
                                                modifier = Modifier.wrapContentWidth()
                                            )
                                        }

                                        LinearProgressIndicator(
                                            Modifier.fillMaxWidth().height(2.dp),
                                            color = player.theme.accent
                                        )
                                    }
                                }

                                val extra_content_alpha: Float by animateFloatAsState(show_message.toFloat())
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
                                ) {
                                    player.controller?.LoadScreenExtraContent(
                                        Modifier.align(Alignment.CenterVertically),
                                        requestServiceChange = requestServiceChange
                                    )

                                    SplashExtraLoadingContent(
                                        Modifier
                                            .align(Alignment.CenterVertically)
                                            .thenIf(!show_message) {
                                                blockGestures()
                                            }
                                            .graphicsLayer { alpha = extra_content_alpha }
                                    )
                                }

                                NullableValueAnimatedVisibility(load_state?.error) { error ->
                                    ErrorInfoDisplay(error, isDebugBuild(), onDismiss = null)
                                }
                            }
                        }
                    }
                }
                SplashMode.WARNING -> {
                    Column(
                        Modifier.fillMaxSize(),
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
}
