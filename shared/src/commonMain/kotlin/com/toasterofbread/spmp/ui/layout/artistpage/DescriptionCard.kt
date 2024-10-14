package com.toasterofbread.spmp.ui.layout.artistpage

import LocalPlayerState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.toastbits.composekit.utils.common.blendWith
import dev.toastbits.composekit.utils.common.thenIf
import dev.toastbits.composekit.utils.composable.LinkifyText
import dev.toastbits.composekit.utils.composable.NoRipple
import com.toasterofbread.spmp.service.playercontroller.PlayerState

@Composable
fun DescriptionCard(description_text: String, expanding: Boolean = true, height: Dp = 200.dp) {
    val player: PlayerState = LocalPlayerState.current

    var expanded: Boolean by remember { mutableStateOf(false) }
    var can_expand: Boolean by remember { mutableStateOf(false) }
    val small_text_height: Dp = 200.dp
    val small_text_height_px: Int = with ( LocalDensity.current ) { small_text_height.toPx().toInt() }
    val padding: Dp = 10.dp

    ElevatedCard(
        Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = player.theme.accent.blendWith(player.theme.background, 0.05f)
        )
    ) {
        Column(
            Modifier
                .padding(horizontal = padding)
                .thenIf(!expanded) {
                    heightIn(max = height)
                }
                .thenIf(!expanding) {
                    verticalScroll(rememberScrollState())
                },
        ) {
            Spacer(Modifier.height(padding))

            if (expanding && can_expand) {
                NoRipple {
                    IconButton(
                        { expanded = !expanded },
                        Modifier
                            .align(Alignment.End)
                            .padding(bottom = 5.dp)
                    ) {
                        Icon(
                            if (expanded) Icons.Filled.ArrowDropUp
                            else Icons.Filled.ArrowDropDown,
                            null
                        )
                    }
                }
            }

            LinkifyText(
                description_text,
                modifier = Modifier
                    .onSizeChanged { size ->
                        if (size.height == small_text_height_px) {
                            can_expand = true
                        }
                    }
                    .animateContentSize(),
                highlight_colour = player.theme.on_background,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = player.theme.on_background.copy(alpha = 0.8f)
                )
            )

            Spacer(Modifier.height(padding))
        }
    }
}
