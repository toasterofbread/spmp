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
import com.toasterofbread.composekit.utils.common.*
import com.toasterofbread.composekit.utils.composable.*
import com.toasterofbread.spmp.service.playercontroller.PlayerState

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

    @Composable
    fun Item(index: Int) {
        val is_selected: Boolean = isItemSelected(index)

        Card(
            { onSelected(index) },
            Modifier.aspectRatio(1f),
            colors =
                if (is_selected) CardDefaults.cardColors(
                    containerColor = player.theme.vibrant_accent,
                    contentColor = player.theme.vibrant_accent.getContrasted()
                )
                else CardDefaults.cardColors(
                    containerColor = player.theme.accent.blendWith(player.theme.background, 0.05f),
                    contentColor = player.theme.on_background
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
                            else player.theme.vibrant_accent
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

    if (lazy) {
        ScrollBarLazyRowOrColumn(
            !vertical,
            modifier,
            contentPadding = content_padding,
            arrangement = Arrangement.spacedBy(15.dp),
            reverseScrollBarLayout = vertical,
            scrollBarColour = LocalContentColor.current.copy(alpha = 0.6f)
        ) {
            items(item_count) { index ->
                Item(index)
            }
        }
    }
    else {
        RowOrColumn(
            !vertical,
            modifier.padding(content_padding).run {
                if (vertical) verticalScroll(rememberScrollState())
                else horizontalScroll(rememberScrollState())
            },
            arrangement = Arrangement.spacedBy(15.dp)
        ) {
            for (i in 0 until item_count) {
                Item(i)
            }
        }
    }
}
