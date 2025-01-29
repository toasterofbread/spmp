package com.toasterofbread.spmp.ui.component

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.ColourSource
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.CustomColourSource
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.PlayerBackgroundColourSource
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.ThemeColourSource
import com.toasterofbread.spmp.ui.layout.nowplaying.getNPBackground
import com.toasterofbread.spmp.ui.theme.appHover
import dev.toastbits.composekit.components.platform.composable.platformClickable
import dev.toastbits.composekit.components.utils.composable.ColourPicker
import dev.toastbits.composekit.components.utils.composable.ShapedIconButton
import dev.toastbits.composekit.components.utils.modifier.bounceOnClick
import dev.toastbits.composekit.theme.core.ThemeValues
import dev.toastbits.composekit.theme.core.get
import dev.toastbits.composekit.theme.core.readableName
import dev.toastbits.composekit.util.getContrasted
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.action_cancel
import spmp.shared.generated.resources.colour_selector_dialog_player_background
import spmp.shared.generated.resources.colour_selector_dialog_select_custom
import spmp.shared.generated.resources.colour_selector_dialog_select_theme
import spmp.shared.generated.resources.colour_selector_dialog_title
import spmp.shared.generated.resources.colour_selector_dialog_transparent

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
            Text(stringResource(Res.string.colour_selector_dialog_title))
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
                contentColor = player.theme.onBackground
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Crossfade(selecting_custom_colour) { it ->
                    Button(
                        { selecting_custom_colour = !it },
                        colors = button_colours
                    ) {
                        Text(
                            if (it) stringResource(Res.string.colour_selector_dialog_select_theme)
                            else stringResource(Res.string.colour_selector_dialog_select_custom)
                        )
                    }
                }

                Button(
                    onDismissed,
                    colors = button_colours
                ) {
                    Text(stringResource(Res.string.action_cancel))
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
        items(ThemeValues.Slot.entries) { slot ->
            ColourCard(
                colour = player.theme[slot],
                name = slot.readableName,
                onSelected = {
                    onSelected(ThemeColourSource(slot))
                }
            )
        }

        item {
            ColourCard(
                colour = player.getNPBackground(),
                name = stringResource(Res.string.colour_selector_dialog_player_background),
                onSelected = {
                    onSelected(PlayerBackgroundColourSource())
                }
            )
        }

        item {
            ColourCard(
                colour = Color.Transparent,
                name = stringResource(Res.string.colour_selector_dialog_transparent),
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
            Unit,
            { current_colour },
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
