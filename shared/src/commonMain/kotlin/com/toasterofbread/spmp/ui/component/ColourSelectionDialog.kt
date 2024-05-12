package com.toasterofbread.spmp.ui.component

import androidx.compose.ui.Modifier
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import dev.toastbits.composekit.settings.ui.Theme
import com.toasterofbread.spmp.resources.getString
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.items
import LocalPlayerState
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.border
import androidx.compose.material3.AlertDialog
import androidx.compose.animation.Crossfade
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.material3.Button
import dev.toastbits.composekit.platform.composable.platformClickable
import dev.toastbits.composekit.utils.composable.ColourPicker
import androidx.compose.material.icons.filled.Done
import androidx.compose.ui.Alignment
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material3.IconButtonDefaults
import dev.toastbits.composekit.utils.common.getContrasted
import dev.toastbits.composekit.utils.composable.ShapedIconButton
import com.toasterofbread.spmp.ui.theme.appHover
import com.toasterofbread.spmp.ui.layout.nowplaying.getNPBackground
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.ColourSource
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.ThemeColourSource
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.PlayerBackgroundColourSource
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.CustomColourSource
import dev.toastbits.composekit.utils.modifier.bounceOnClick
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.animation.animateContentSize

@Composable
fun ColourSelectionDialog(
    onDismissed: () -> Unit,
    onColourSelected: (ColourSource) -> Unit,
    modifier: Modifier = Modifier
) {
    val player: PlayerState = LocalPlayerState.current
    var selecting_custom_colour: Boolean by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissed,
        title = {
            Text(getString("colour_selector_dialog_title"))
        },
        text = {
            Crossfade(selecting_custom_colour, Modifier.animateContentSize()) {
                if (it) {
                    CustomColourSelector(onColourSelected)
                }
                else {
                    ThemeColourSelectionList(onColourSelected)
                }
            }
        },
        confirmButton = {
            val button_colours: ButtonColors = ButtonDefaults.buttonColors(
                containerColor = player.theme.background,
                contentColor = player.theme.on_background
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Crossfade(selecting_custom_colour) { it ->
                    Button(
                        { selecting_custom_colour = !it },
                        colors = button_colours
                    ) {
                        Text(
                            if (it) getString("colour_selector_dialog_select_theme")
                            else getString("colour_selector_dialog_select_custom")
                        )
                    }
                }

                Button(
                    onDismissed,
                    colors = button_colours
                ) {
                    Text(getString("action_cancel"))
                }
            }
        }
    )
}

@Composable
private fun ThemeColourSelectionList(
    onSelected: (ColourSource) -> Unit,
    modifier: Modifier = Modifier
) {
    val player: PlayerState = LocalPlayerState.current

    LazyColumn(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(Theme.Colour.entries) { colour ->
            ColourCard(
                colour = colour.get(player.theme),
                name = colour.getReadable(),
                onSelected = {
                    onSelected(ThemeColourSource(colour))
                }
            )
        }

        item {
            ColourCard(
                colour = player.getNPBackground(),
                name = getString("colour_selector_dialog_player_background"),
                onSelected = {
                    onSelected(PlayerBackgroundColourSource())
                }
            )
        }

        item {
            ColourCard(
                colour = Color.Transparent,
                name = getString("colour_selector_dialog_transparent"),
                onSelected = {
                    onSelected(CustomColourSource(Color.Transparent))
                }
            )
        }
    }
}

@Composable
private fun ColourCard(colour: Color, name: String, onSelected: () -> Unit) {
    val shape: Shape = RoundedCornerShape(20.dp)

    Row(
        Modifier
            .bounceOnClick()
            .appHover(true)
            .platformClickable(
                onClick = { onSelected() }
            )
            .background(colour, shape)
            .border(2.dp, colour.getContrasted(), shape)
            .padding(15.dp)
            .fillMaxWidth()
    ) {
        CompositionLocalProvider(LocalContentColor provides colour.getContrasted()) {
            Text(name, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun CustomColourSelector(
    onSelected: (ColourSource) -> Unit,
    modifier: Modifier = Modifier
) {
    var current_colour: Color by remember { mutableStateOf(Color.Red) }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ColourPicker(
            current_colour,
            modifier,
            bottomRowExtraContent = {
                ShapedIconButton(
                    { onSelected(CustomColourSource(current_colour)) },
                    colours = IconButtonDefaults.iconButtonColors(
                        containerColor = current_colour,
                        contentColor = current_colour.getContrasted()
                    )
                ) {
                    Icon(Icons.Default.Done, null)
                }
            }
        ) {
            current_colour = it
        }
    }
}

fun Theme.Colour.getReadable(): String =
    when (this) {
        Theme.Colour.BACKGROUND -> getString("theme_colour_background")
        Theme.Colour.ACCENT -> getString("theme_colour_accent")
        Theme.Colour.VIBRANT_ACCENT -> getString("theme_colour_vibrant_accent")
        Theme.Colour.CARD -> getString("theme_colour_card")
    }
