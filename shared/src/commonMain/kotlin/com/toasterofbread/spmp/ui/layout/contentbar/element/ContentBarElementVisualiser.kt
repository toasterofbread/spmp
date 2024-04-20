package com.toasterofbread.spmp.ui.layout.contentbar.element

import kotlinx.serialization.json.*
import kotlinx.serialization.Serializable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Waves
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.LayoutSlot
import LocalPlayerState

@Serializable
data class ContentBarElementVisualiser(
    override val config: ContentBarElementConfig = ContentBarElementConfig()
): ContentBarElement() {
    override fun getType(): ContentBarElement.Type = ContentBarElement.Type.VISUALISER

    override fun copyWithConfig(config: ContentBarElementConfig): ContentBarElement =
        copy(config = config)

    @Composable
    override fun isDisplaying(): Boolean =
        LocalPlayerState.current.status.m_song != null

    @Composable
    override fun ElementContent(vertical: Boolean, slot: LayoutSlot?, onPreviewClick: (() -> Unit)?, modifier: Modifier) {
        val player: PlayerState = LocalPlayerState.current

        if (onPreviewClick != null) {
            IconButton(onPreviewClick) {
                Icon(Icons.Default.Waves, null)
            }
        }
        else {
            player.controller?.Visualiser(
                LocalContentColor.current,
                modifier,
                0.5f
            )
        }
    }
}
