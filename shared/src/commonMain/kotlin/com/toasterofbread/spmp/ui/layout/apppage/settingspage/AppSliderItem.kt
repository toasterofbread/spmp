package com.toasterofbread.spmp.ui.layout.apppage.settingspage

import com.toasterofbread.composekit.settings.ui.item.BasicSettingsValueState
import com.toasterofbread.composekit.settings.ui.item.SettingsSliderItem
import com.toasterofbread.composekit.utils.common.roundTo
import com.toasterofbread.spmp.resources.getString

fun AppSliderItem(
    state: BasicSettingsValueState<out Number>,
    title: String?,
    subtitle: String?,
    min_label: String? = null,
    max_label: String? = null,
    steps: Int = 0,
    range: ClosedFloatingPointRange<Float> = 0f .. 1f,
    getValueText: ((value: Number) -> String?)? = {
        if (it is Float) it.roundTo(2).toString()
        else it.toString()
    }
): SettingsSliderItem =
    SettingsSliderItem(
        state = state,
        title = title,
        subtitle = subtitle,
        getErrMsgValueOutOfRange = {
            getString("settings_value_out_of_\$range").replace("\$range", it.toString())
        },
        errmsg_value_not_int = getString("settings_value_not_int"),
        errmsg_value_not_float = getString("settings_value_not_float"),
        min_label = min_label,
        max_label = max_label,
        steps = steps,
        range = range,
        getValueText = getValueText
    )
