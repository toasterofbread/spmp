package com.toasterofbread.spmp.ui.layout.apppage.settingspage

import androidx.compose.ui.Modifier
import dev.toastbits.composekit.util.roundTo
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.appTextField
import dev.toastbits.composekit.settingsitem.domain.PlatformSettingsProperty
import dev.toastbits.composekit.settingsitem.presentation.ui.component.item.SliderSettingsItem
import dev.toastbits.composekit.util.CustomStringResource
import dev.toastbits.composekit.util.toCustomResource
import org.jetbrains.compose.resources.getString
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.settings_value_not_int
import spmp.shared.generated.resources.settings_value_not_float
import spmp.shared.generated.resources.`settings_value_out_of_$range`

fun AppSliderItem(
    state: PlatformSettingsProperty<out Number>,
    min_label: CustomStringResource? = null,
    max_label: CustomStringResource? = null,
    steps: Int = 0,
    range: ClosedFloatingPointRange<Float> = 0f .. 1f,
    getValueText: ((value: Number) -> String?)? = {
        if (it is Float) it.roundTo(2).toString()
        else it.toString()
    }
): SliderSettingsItem =
    SliderSettingsItem(
        state = state,
        minLabel = min_label,
        maxLabel = max_label,
        steps = steps,
        range = range,
        getValueText = getValueText,
        getFieldModifier = { Modifier.appTextField() }
    )
