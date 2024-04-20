package com.toasterofbread.spmp.ui.layout.apppage.settingspage

import androidx.compose.ui.Modifier
import dev.toastbits.composekit.settings.ui.item.SliderSettingsItem
import dev.toastbits.composekit.utils.common.roundTo
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.appTextField
import dev.toastbits.composekit.platform.PreferencesProperty

fun AppSliderItem(
    state: PreferencesProperty<out Number>,
    min_label: String? = null,
    max_label: String? = null,
    steps: Int = 0,
    range: ClosedFloatingPointRange<Float> = 0f .. 1f,
    getValueText: ((value: Number) -> String?)? = {
        if (it is Float) it.roundTo(2).toString()
        else it.toString()
    }
): SliderSettingsItem =
    SliderSettingsItem(
        state = state,
        getErrMsgValueOutOfRange = {
            getString("settings_value_out_of_\$range").replace("\$range", it.toString())
        },
        errmsg_value_not_int = getString("settings_value_not_int"),
        errmsg_value_not_float = getString("settings_value_not_float"),
        min_label = min_label,
        max_label = max_label,
        steps = steps,
        range = range,
        getValueText = getValueText,
        getFieldModifier = { Modifier.appTextField() }
    )
