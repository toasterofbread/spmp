package com.toasterofbread.spmp.ui.layout.artistpage

import LocalPlayerState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import dev.toastbits.composekit.util.blendWith
import dev.toastbits.composekit.components.utils.composable.LinkifyText

@Composable
fun DescriptionCard(description_text: String) {
    val player: PlayerState = LocalPlayerState.current

    ElevatedCard(
        Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = player.theme.accent.blendWith(player.theme.background, 0.05f)
        )
    ) {
        LinkifyText(
            description_text,
            modifier = Modifier.padding(10.dp),
            highlight_colour = player.theme.onBackground,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = player.theme.onBackground.copy(alpha = 0.8f)
            )
        )
    }
}
