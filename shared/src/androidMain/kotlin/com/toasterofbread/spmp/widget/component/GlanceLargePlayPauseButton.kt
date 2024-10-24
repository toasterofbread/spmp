package com.toasterofbread.spmp.widget.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import com.toasterofbread.spmp.shared.R
import com.toasterofbread.spmp.widget.action.PlayPauseAction
import dev.toastbits.composekit.platform.composable.theme.LocalApplicationTheme
import dev.toastbits.composekit.settings.ui.ThemeValues
import dev.toastbits.composekit.settings.ui.vibrant_accent

@Composable
internal fun GlanceLargePlayPauseButton(
    play: Boolean,
    modifier: GlanceModifier = GlanceModifier
) {
    val theme: ThemeValues = LocalApplicationTheme.current
    Box(
        modifier
            .background(theme.vibrant_accent)
            .cornerRadius(10.dp)
            .clickable(
                PlayPauseAction(play)
            ),
        contentAlignment = Alignment.Center
    ) {
        val icon: Int =
            if (play) R.drawable.ic_play
            else R.drawable.ic_pause

        Image(ImageProvider(icon), null)
    }
}
