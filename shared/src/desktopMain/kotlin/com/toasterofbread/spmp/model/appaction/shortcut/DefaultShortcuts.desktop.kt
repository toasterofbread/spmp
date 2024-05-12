package com.toasterofbread.spmp.model.appaction.shortcut

import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.key.Key
import com.toasterofbread.spmp.ui.component.shortcut.trigger.*
import com.toasterofbread.spmp.model.appaction.*

actual fun getPlatformDefaultShortcuts(): List<Shortcut> =
    listOf(
        Shortcut(
            MouseButtonShortcutTrigger(PointerButton.Back.index),
            OtherAppAction(OtherAppAction.Action.NAVIGATE_BACK)
        ),
        Shortcut(
            MouseButtonShortcutTrigger(5),
            OtherAppAction(OtherAppAction.Action.NAVIGATE_BACK)
        ),
        Shortcut(
            KeyboardShortcutTrigger(Key.F11.keyCode),
            OtherAppAction(OtherAppAction.Action.TOGGLE_FULLSCREEN)
        )
    )
