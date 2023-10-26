package com.toasterofbread.spmp.ui.layout.radiobuilder

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.toastercomposetools.settings.ui.Theme
import com.toasterofbread.spmp.youtubeapi.RadioBuilderModifier
import com.toasterofbread.toastercomposetools.utils.composable.NoRipple
import kotlinx.coroutines.launch
import kotlin.math.ceil

@Composable
internal fun SelectionTypeRow(state: MutableState<RadioBuilderModifier.SelectionType>) {
    val player = LocalPlayerState.current

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.Center) {
            for (i in 0 until 5) {
                val size = remember { Animatable(0f) }
                val arc_angle = remember { Animatable(0f) }
                val offset = remember { Animatable(0f) }

                LaunchedEffect(state.value) {
                    val values = getRecordArcValues(state.value, i)

                    launch {
                        size.animateTo(values.first.value)
                    }
                    launch {
                        arc_angle.animateTo(values.second)
                    }
                    launch {
                        offset.animateTo(values.third, SpringSpec(stiffness = Spring.StiffnessVeryLow))
                    }
                }

                RecordArc(size.value.dp, arc_angle.value, offset.value, player.theme.vibrant_accent)
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(getString("radio_builder_modifier_selection_type"))

            MultiSelectRow(
                amount = RadioBuilderModifier.SelectionType.values().size,
                isSelected = { it == state.value.ordinal },
                onSelected = { state.value =  RadioBuilderModifier.SelectionType.values()[it!!] },
                getText = {
                    RadioBuilderModifier.SelectionType.values()[it].getReadable()
                },
                button_padding = PaddingValues(0.dp)
            )
        }

//            for (type in RadioBuilderModifier.SelectionType.values()) {
//                var animate by remember { mutableStateOf(false) }
//                Column(
//                    Modifier
//                        .fillMaxWidth()
//                        .weight(1f)
//                        .cliclableNoIndication {
//                            if (type != state.value) {
//                                state.value = type
//                                animate = !animate
//                            }
//                        },
//                    horizontalAlignment = Alignment.CenterHorizontally,
//                    verticalArrangement = Arrangement.spacedBy(15.dp)
//                ) {
//                    RadioSelectionTypeAnimation(type, animate, colour = if (state.value == type) player.theme.vibrant_accent else player.theme.on_background)
//                    Text(text)
//                }
//            }
    }
}

@Composable
internal fun ArtistVarietyRow(state: MutableState<RadioBuilderModifier.Variety>) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(getString("radio_builder_modifier_variety"))
        MultiSelectRow(
            RadioBuilderModifier.Variety.values().size,
            isSelected = { state.value.ordinal == it },
            onSelected = { state.value = RadioBuilderModifier.Variety.values()[it!!] },
            getText = {
                RadioBuilderModifier.Variety.values()[it].getReadable()
            }
        )
    }
}

@Composable
internal fun FilterARow(state: MutableState<RadioBuilderModifier.FilterA?>) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(getString("radio_builder_modifier_filter_a"))
        MultiSelectRow(
            RadioBuilderModifier.FilterA.values().size,
            isSelected = { state.value?.ordinal == it },
            onSelected = { state.value = it?.let { RadioBuilderModifier.FilterA.values()[it] } },
            getText = {
                RadioBuilderModifier.FilterA.values()[it].getReadable()
            },
            nullable = true
        )
    }
}

@Composable
internal fun FilterBRow(state: MutableState<RadioBuilderModifier.FilterB?>) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(getString("radio_builder_modifier_filter_b"))
        MultiSelectRow(
            RadioBuilderModifier.FilterB.values().size,
            isSelected = { state.value?.ordinal == it },
            onSelected = { state.value = it?.let { RadioBuilderModifier.FilterB.values()[it] } },
            getText = {
                RadioBuilderModifier.FilterB.values()[it].getReadable()
            },
            nullable = true,
            button_padding = PaddingValues(0.dp),
            columns = 3
        )
    }
}

@Composable
internal fun MultiSelectRow(
    amount: Int,
    arrangement: Arrangement.Horizontal = Arrangement.spacedBy(10.dp),
    isSelected: (Int) -> Boolean,
    onSelected: (Int?) -> Unit,
    getText: (Int) -> String,
    nullable: Boolean = false,
    button_padding: PaddingValues = ButtonDefaults.ContentPadding,
    columns: Int = amount,
    shape: Shape = RoundedCornerShape(12.dp)
) {
    val player = LocalPlayerState.current

    val rows = if (columns <= 0) 1 else ceil(amount / columns.toDouble()).toInt()
    for (row in 0 until rows) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = arrangement,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in row * columns until (row + 1) * columns) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (i < amount) {
                        NoRipple {
                            Crossfade(isSelected(i)) { selected ->
                                if (selected) {
                                    Button(
                                        { if (nullable) onSelected(null) },
                                        Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = player.theme.accent,
                                            contentColor = player.theme.on_accent
                                        ),
                                        contentPadding = button_padding,
                                        shape = shape
                                    ) {
                                        Text(getText(i))
                                    }
                                }
                                else {
                                    OutlinedButton(
                                        { onSelected(i) },
                                        Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = player.theme.on_background
                                        ),
                                        contentPadding = button_padding,
                                        shape = shape
                                    ) {
                                        Text(getText(i))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
