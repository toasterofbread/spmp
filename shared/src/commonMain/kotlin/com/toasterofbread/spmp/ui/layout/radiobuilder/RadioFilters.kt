package com.toasterofbread.spmp.ui.layout.radiobuilder

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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.api.RadioModifier
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.utils.composable.NoRipple
import kotlinx.coroutines.launch
import kotlin.math.ceil

@Composable
internal fun SelectionTypeRow(state: MutableState<RadioModifier.SelectionType>) {
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

                RecordArc(size.value.dp, arc_angle.value, offset.value, Theme.vibrant_accent)
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
                amount = RadioModifier.SelectionType.values().size,
                isSelected = { it == state.value.ordinal },
                onSelected = { state.value =  RadioModifier.SelectionType.values()[it!!] },
                getText = {
                    RadioModifier.SelectionType.values()[it].getReadable()
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
//                    RadioSelectionTypeAnimation(type, animate, colour = if (state.value == type) Theme.vibrant_accent else Theme.on_background)
//                    Text(text)
//                }
//            }
    }
}

@Composable
internal fun ArtistVarietyRow(state: MutableState<RadioModifier.Variety>) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(getString("radio_builder_modifier_variety"))
        MultiSelectRow(
            RadioModifier.Variety.values().size,
            isSelected = { state.value.ordinal == it },
            onSelected = { state.value = RadioModifier.Variety.values()[it!!] },
            getText = {
                RadioModifier.Variety.values()[it].getReadable()
            }
        )
    }
}

@Composable
internal fun FilterARow(state: MutableState<RadioModifier.FilterA?>) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(getString("radio_builder_modifier_filter_a"))
        MultiSelectRow(
            RadioModifier.FilterA.values().size,
            isSelected = { state.value?.ordinal == it },
            onSelected = { state.value = it?.let { RadioModifier.FilterA.values()[it] } },
            getText = {
                RadioModifier.FilterA.values()[it].getReadable()
            },
            nullable = true
        )
    }
}

@Composable
internal fun FilterBRow(state: MutableState<RadioModifier.FilterB?>) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(getString("radio_builder_modifier_filter_b"))
        MultiSelectRow(
            RadioModifier.FilterB.values().size,
            isSelected = { state.value?.ordinal == it },
            onSelected = { state.value = it?.let { RadioModifier.FilterB.values()[it] } },
            getText = {
                RadioModifier.FilterB.values()[it].getReadable()
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
                                            containerColor = Theme.accent,
                                            contentColor = Theme.on_accent
                                        ),
                                        contentPadding = button_padding,
                                        shape = shape
                                    ) {
                                        Text(getText(i))
                                    }
                                } else {
                                    OutlinedButton(
                                        { onSelected(i) },
                                        Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Theme.on_background
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
