package com.toasterofbread.spmp.widget.configuration

import kotlinx.serialization.Serializable

@Serializable
sealed interface TypeConfigurationDefaultsMask<C: TypeWidgetConfig<*>> {
    val click_action: Boolean
    fun applyTo(config: C, default: C): C

    fun setClickAction(click_action: Boolean): TypeConfigurationDefaultsMask<C>
}
