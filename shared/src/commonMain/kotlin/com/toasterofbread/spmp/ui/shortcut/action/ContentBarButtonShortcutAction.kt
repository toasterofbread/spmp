package com.toasterofbread.spmp.ui.shortcut.action

import com.toasterofbread.spmp.ui.layout.contentbar.element.ContentBarElementButton
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import kotlinx.serialization.Serializable

@Serializable
data class ContentBarButtonShortcutAction(
    val button_type: ContentBarElementButton.Type
): ShortcutAction {
    override fun execute(player: PlayerState) {
        button_type.executeAction(player)
    }
}
