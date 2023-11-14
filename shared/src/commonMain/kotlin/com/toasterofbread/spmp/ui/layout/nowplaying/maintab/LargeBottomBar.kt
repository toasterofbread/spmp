package com.toasterofbread.spmp.ui.layout.nowplaying.maintab

import LocalPlayerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState

@Composable
internal fun LargeBottomBar(modifier: Modifier = Modifier) {
    val player: PlayerState = LocalPlayerState.current

    Row(
        modifier,
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton({ player.expansion.close() }) {
            Icon(Icons.Default.KeyboardArrowDown, null, tint = LocalContentColor.current)
        }
    }
}
