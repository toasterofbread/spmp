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
import dev.toastbits.composekit.util.*
import dev.toastbits.composekit.components.utils.composable.RowOrColumnScope
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.LayoutSlot
import kotlin.math.roundToInt
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
data class ContentBarElementSpacer(
    override val config: ContentBarElementConfig = ContentBarElementConfig()
): ContentBarElement() {
    override fun getType(): ContentBarElement.Type = ContentBarElement.Type.SPACER

    override fun copyWithConfig(config: ContentBarElementConfig): ContentBarElement =
        copy(config = config)

    @Composable
    override fun isDisplaying(): Boolean = false

    override fun blocksIndicatorAnimation(): Boolean = true

    @Composable
    override fun ElementContent(vertical: Boolean, slot: LayoutSlot?, bar_size: DpSize, onPreviewClick: (() -> Unit)?, modifier: Modifier) {
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
