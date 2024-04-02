package com.toasterofbread.spmp.ui.layout.contentbar.element

import LocalPlayerState
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.Alignment
import com.toasterofbread.composekit.utils.common.*
import com.toasterofbread.composekit.utils.composable.RowOrColumnScope
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import kotlin.math.roundToInt
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
data class ContentBarElementSpacer(
    override val size_mode: ContentBarElement.SizeMode = DEFAULT_SIZE_MODE,
    override val size: Int = DEFAULT_SIZE,
): ContentBarElement() {
    override fun getType(): ContentBarElement.Type = ContentBarElement.Type.SPACER

    override fun copyWithSize(size_mode: ContentBarElement.SizeMode, size: Int): ContentBarElement =
        copy(size_mode = size_mode, size = size)

    @Composable
    override fun isDisplaying(): Boolean = false

    override fun blocksIndicatorAnimation(): Boolean = true

    @Composable
    override fun ElementContent(vertical: Boolean, onPreviewClick: (() -> Unit)?, modifier: Modifier) {
        Box(
            modifier
                .run {
                    if (vertical) height(IntrinsicSize.Min)
                    else width(IntrinsicSize.Min)
                }
                .thenWith(onPreviewClick) {
                    clickable(onClick = it)
                },
            contentAlignment = Alignment.Center
        ) {
            if (onPreviewClick != null) {
                if (vertical) {
                    VerticalDivider(Modifier.fillMaxHeight())
                }
                else {
                    HorizontalDivider(Modifier.fillMaxWidth())
                }
            }
        }
    }
}
