package com.toasterofbread.spmp.widget.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.glance.GlanceModifier
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.height
import androidx.glance.layout.size
import androidx.glance.layout.width
import com.toasterofbread.spmp.widget.action.TypeWidgetClickAction
import com.toasterofbread.spmp.widget.action.WidgetClickAction
import com.toasterofbread.spmp.widget.component.styledcolumn.GLANCE_STYLED_COLUMN_DEFAULT_SPACING
import com.toasterofbread.spmp.widget.modifier.size
import dev.toastbits.composekit.theme.core.ui.LocalComposeKitTheme
import dev.toastbits.composekit.theme.core.vibrantAccent

enum class GlanceActionButtonGridMode {
    FILL,
    NO_FILL
}

private fun GlanceActionButtonGridMode.getButtonSize(size: DpSize, spacing: Dp): DpSize =
    when (this) {
        GlanceActionButtonGridMode.FILL ->
            DpSize(
                (size.width - spacing) / 2,
                (size.height - spacing) / 2
            )
        GlanceActionButtonGridMode.NO_FILL -> {
            val main_size: Dp = (minOf(size.width, size.height) - spacing) / 2
            DpSize(main_size, main_size)
        }
    }

@Composable
fun <T: TypeWidgetClickAction> GlanceActionButtonGrid(
    size: DpSize,
    mode: GlanceActionButtonGridMode,
    top_start_button_action: WidgetClickAction<T>,
    top_end_button_action: WidgetClickAction<T>,
    bottom_start_button_action: WidgetClickAction<T>,
    bottom_end_button_action: WidgetClickAction<T>,
    getTypeActionIcon: (T) -> Int?,
    modifier: GlanceModifier = GlanceModifier,
    button_modifier: GlanceModifier = GlanceModifier,
    spacing: Dp = GLANCE_STYLED_COLUMN_DEFAULT_SPACING,
    alignment: Alignment = Alignment.Center,
    button_background_colour: Color = LocalComposeKitTheme.current.vibrantAccent
) {
    val button_size: DpSize = mode.getButtonSize(size, spacing)
    val button_icon_size: Dp = minOf(button_size.width, button_size.height) * 0.37f

    Column(
        modifier.size(size),
        verticalAlignment = alignment.vertical
    ) {
        Row(
            GlanceModifier.height(button_size.height),
            horizontalAlignment = alignment.horizontal
        ) {
            CommonActionButton(
                top_start_button_action,
                button_modifier.size(button_size),
                getTypeActionIcon = getTypeActionIcon,
                icon_modifier = GlanceModifier.size(button_icon_size),
                background_colour = button_background_colour
            )

            Spacer(GlanceModifier.width(spacing))

            CommonActionButton(
                top_end_button_action,
                button_modifier.size(button_size),
                getTypeActionIcon = getTypeActionIcon,
                icon_modifier = GlanceModifier.size(button_icon_size),
                background_colour = button_background_colour
            )
        }

        Spacer(GlanceModifier.height(spacing))

        Row(
            GlanceModifier.height(button_size.height),
            horizontalAlignment = alignment.horizontal
        ) {
            CommonActionButton(
                bottom_start_button_action,
                button_modifier.size(button_size),
                getTypeActionIcon = getTypeActionIcon,
                icon_modifier = GlanceModifier.size(button_icon_size),
                background_colour = button_background_colour
            )

            Spacer(GlanceModifier.width(spacing))

            CommonActionButton(
                bottom_end_button_action,
                button_modifier.size(button_size),
                getTypeActionIcon = getTypeActionIcon,
                icon_modifier = GlanceModifier.size(button_icon_size),
                background_colour = button_background_colour
            )
        }
    }
}
