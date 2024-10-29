package com.toasterofbread.spmp.widget.component

import LocalPlayerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.unit.ColorProvider
import com.toasterofbread.spmp.shared.R
import com.toasterofbread.spmp.widget.WidgetActionCallback
import com.toasterofbread.spmp.widget.action.TypeWidgetClickAction
import com.toasterofbread.spmp.widget.action.WidgetClickAction
import com.toasterofbread.spmp.widget.action.WidgetClickAction.CommonWidgetClickAction.NONE
import com.toasterofbread.spmp.widget.action.WidgetClickAction.CommonWidgetClickAction.OPEN_SPMP
import com.toasterofbread.spmp.widget.action.WidgetClickAction.CommonWidgetClickAction.OPEN_WIDGET_CONFIG
import com.toasterofbread.spmp.widget.action.WidgetClickAction.CommonWidgetClickAction.PLAY_PAUSE
import com.toasterofbread.spmp.widget.action.WidgetClickAction.CommonWidgetClickAction.SEEK_NEXT
import com.toasterofbread.spmp.widget.action.WidgetClickAction.CommonWidgetClickAction.SEEK_PREVIOUS
import com.toasterofbread.spmp.widget.action.WidgetClickAction.CommonWidgetClickAction.TOGGLE_VISIBILITY
import dev.toastbits.composekit.platform.composable.theme.LocalApplicationTheme
import dev.toastbits.composekit.settings.ui.ThemeValues
import dev.toastbits.composekit.settings.ui.on_accent
import dev.toastbits.composekit.settings.ui.vibrant_accent
import dev.toastbits.composekit.utils.common.getContrasted

@Composable
internal fun <T: TypeWidgetClickAction> CommonActionButton(
    action: WidgetClickAction<T>,
    modifier: GlanceModifier = GlanceModifier,
    icon_modifier: GlanceModifier = GlanceModifier,
    background_colour: Color = LocalApplicationTheme.current.vibrant_accent,
    getTypeActionIcon: (T) -> Int?
) {
    Box(
        modifier
            .background(background_colour)
            .clickable(
                WidgetActionCallback(action)
            ),
        contentAlignment = Alignment.Center
    ) {
        val icon: Int? =
            when (action) {
                is WidgetClickAction.CommonWidgetClickAction -> action.getIcon()
                is WidgetClickAction.Type -> getTypeActionIcon(action.actionEnum)
            }

        if (icon != null) {
            Image(
                ImageProvider(icon),
                null,
                icon_modifier,
                colorFilter = ColorFilter.tint(ColorProvider(background_colour.getContrasted().copy(alpha = 0.85f)))
            )
        }
    }
}

@Composable
private fun WidgetClickAction.CommonWidgetClickAction.getIcon(): Int? =
    when (this) {
        NONE -> null
        OPEN_SPMP -> R.drawable.ic_spmp
        OPEN_WIDGET_CONFIG -> R.drawable.ic_settings
        TOGGLE_VISIBILITY -> R.drawable.ic_visibility
        PLAY_PAUSE ->
            if (LocalPlayerState.current.status.m_playing) R.drawable.ic_pause
            else R.drawable.ic_play
        SEEK_NEXT -> R.drawable.ic_skip_next
        SEEK_PREVIOUS -> R.drawable.ic_skip_previous
    }
