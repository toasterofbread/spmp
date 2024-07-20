package com.toasterofbread.spmp.ui.layout.apppage.settingspage

import androidx.compose.ui.Modifier
import dev.toastbits.composekit.settings.ui.item.SliderSettingsItem
import dev.toastbits.composekit.utils.common.roundTo
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.appTextField
import dev.toastbits.composekit.platform.PreferencesProperty
import dev.toastbits.composekit.utils.common.CustomStringResource
import dev.toastbits.composekit.utils.common.toCustomResource
import org.jetbrains.compose.resources.getString
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.settings_value_not_int
import spmp.shared.generated.resources.settings_value_not_float
import spmp.shared.generated.resources.`settings_value_out_of_$range`

fun AppSliderItem(
    state: PreferencesProperty<out Number>,
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
        getErrMsgValueOutOfRange = {
            getString(Res.string.`settings_value_out_of_$range`).replace("\$range", it.toString())
        },
        errmsg_value_not_int = Res.string.settings_value_not_int.toCustomResource(),
        errmsg_value_not_float = Res.string.settings_value_not_float.toCustomResource(),
        min_label = min_label,
        max_label = max_label,
        steps = steps,
        range = range,
        getValueText = getValueText,
        getFieldModifier = { Modifier.appTextField() }
    )
