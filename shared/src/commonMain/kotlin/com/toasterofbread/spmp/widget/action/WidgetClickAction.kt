package com.toasterofbread.spmp.widget.action

import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.widget_click_action_common_none
import spmp.shared.generated.resources.widget_click_action_common_open_spmp
import spmp.shared.generated.resources.widget_click_action_common_open_widget_config
import spmp.shared.generated.resources.widget_click_action_common_play_pause
import spmp.shared.generated.resources.widget_click_action_common_seek_next
import spmp.shared.generated.resources.widget_click_action_common_seek_previous
import spmp.shared.generated.resources.widget_click_action_common_toggle_like
import spmp.shared.generated.resources.widget_click_action_common_toggle_visibility

@Serializable
sealed interface WidgetClickAction<in A: TypeWidgetClickAction> {
    @Serializable
    enum class CommonWidgetClickAction(val nameResource: StringResource): WidgetClickAction<TypeWidgetClickAction> {
        NONE(Res.string.widget_click_action_common_none),
        OPEN_SPMP(Res.string.widget_click_action_common_open_spmp),
        OPEN_WIDGET_CONFIG(Res.string.widget_click_action_common_open_widget_config),
        TOGGLE_VISIBILITY(Res.string.widget_click_action_common_toggle_visibility),
        PLAY_PAUSE(Res.string.widget_click_action_common_play_pause),
        SEEK_NEXT(Res.string.widget_click_action_common_seek_next),
        SEEK_PREVIOUS(Res.string.widget_click_action_common_seek_previous),
        TOGGLE_LIKE(Res.string.widget_click_action_common_toggle_like);
    }

    @Serializable
    data class Type<A: TypeWidgetClickAction>(val actionEnum: A): WidgetClickAction<A>

    companion object {
        val DEFAULT: CommonWidgetClickAction = CommonWidgetClickAction.OPEN_SPMP
    }
}
