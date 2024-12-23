package com.toasterofbread.spmp.ui.component

import LocalPlayerState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.toastbits.composekit.util.*
import dev.toastbits.composekit.components.utils.composable.*
import dev.toastbits.composekit.components.utils.modifier.horizontal
import dev.toastbits.composekit.components.utils.modifier.vertical
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import dev.toastbits.composekit.theme.core.vibrantAccent
import dev.toastbits.composekit.util.composable.WidthShrinkText

@Composable
fun LargeFilterList(
    item_count: Int,
    getItemText: @Composable (Int) -> String,
    getItemIcon: @Composable (Int) -> ImageVector?,
    isItemSelected: @Composable (Int) -> Boolean,
    modifier: Modifier = Modifier,
    content_padding: PaddingValues = PaddingValues(),
    vertical: Boolean = true,
    lazy: Boolean = false,
    onSelected: (Int) -> Unit
) {
    val player: PlayerState = LocalPlayerState.current
    val horizontal_padding: PaddingValues = content_padding.horizontal

    @Composable
    fun Item(index: Int) {
        val is_selected: Boolean = isItemSelected(index)

        Card(
            { onSelected(index) },
            Modifier.padding(horizontal_padding).aspectRatio(1f),
            colors =
                if (is_selected) CardDefaults.cardColors(
                    containerColor = player.theme.vibrantAccent,
                    contentColor = player.theme.vibrantAccent.getContrasted()
                )
                else CardDefaults.cardColors(
                    containerColor = player.theme.accent.blendWith(player.theme.background, 0.05f),
                    contentColor = player.theme.onBackground
                ),
            shape = RoundedCornerShape(25.dp)
        ) {
            Column(Modifier.fillMaxSize().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val icon: ImageVector? = getItemIcon(index)
                if (icon != null) {
                    Icon(
                        icon,
                        null,
                        Modifier.aspectRatio(1f).fillMaxHeight().weight(1f).padding(10.dp),
                        tint =
                            if (is_selected) LocalContentColor.current
                            else player.theme.vibrantAccent
                    )
                }

                WidthShrinkText(
                    getItemText(index),
                    style = MaterialTheme.typography.labelLarge,
                    alignment = TextAlign.Center
                )
            }
        }
    }

    ScrollableRowOrColumn(
        !vertical,
        lazy,
        item_count,
        modifier,
        content_padding = content_padding.vertical,
        arrangement = Arrangement.spacedBy(15.dp),
        reverse_scroll_bar_layout = vertical,
        scroll_bar_colour = LocalContentColor.current.copy(alpha = 0.6f)
    ) { i ->
        Item(i)
    }
}
