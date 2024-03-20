package com.toasterofbread.spmp.ui.shortcut.action

import com.toasterofbread.spmp.service.playercontroller.PlayerState
import kotlinx.serialization.Serializable

@Serializable
sealed interface ShortcutAction {
    fun execute(player: PlayerState)
}
