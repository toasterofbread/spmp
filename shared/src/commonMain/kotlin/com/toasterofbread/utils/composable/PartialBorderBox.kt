package com.toasterofbread.utils.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.toasterofbread.spmp.ui.theme.Theme

@Composable
fun PartialBorderBox(
    labelContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    border_shape: Shape = RoundedCornerShape(16.dp),
    border_width: Dp = 1.dp,
    border_colour: Color = LocalContentColor.current,
    background_colour: Color = Theme.background,
    content: @Composable BoxScope.() -> Unit
) {
    val density = LocalDensity.current

    var label_height: Dp by remember { mutableStateOf(0.dp) }
    var main_size: DpSize by remember { mutableStateOf(DpSize.Zero) }
    val content_padding = (label_height / 2) + 5.dp

    Box(
        modifier
            .padding(top = label_height / 2)
            .wrapContentHeight()
    ) {
        Box(
            Modifier
                .width(main_size.width + (content_padding * 2))
                .height(main_size.height + (content_padding * 2))
                .border(
                    border_width,
                    border_colour,
                    border_shape
                )
        )

        Box(
            Modifier
                .offset(
                    x = 10.dp,
                    y = -label_height / 2
                )
                .background(background_colour)
                .padding(horizontal = 10.dp)
                .zIndex(1f)
                .onSizeChanged {
                    label_height = with(density) {
                        it.height.toDp()
                    }
                }
        ) {
            labelContent()
        }

        Box(
            Modifier.fillMaxSize().padding(content_padding),
        ) {
            Box(
                Modifier
                    .onSizeChanged {
                        main_size = with(density) {
                            DpSize(it.width.toDp(), it.height.toDp())
                        }
                    }
            ) {
                content()
            }
        }
    }
}