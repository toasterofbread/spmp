package com.toasterofbread.spmp.ui.shortcut

import androidx.compose.foundation.shape.CircleShape
import kotlinx.serialization.Serializable
import com.toasterofbread.spmp.ui.shortcut.action.ShortcutAction
import com.toasterofbread.spmp.ui.shortcut.trigger.ShortcutTrigger

val SHORTCUT_INDICATOR_SHAPE get() = CircleShape
const val SHORTCUT_INDICATOR_GROUP_ANIM_DURATION_MS: Long = 30
const val SHORTCUT_INDICATOR_SHOW_DELAY_MS: Long = 200

@Serializable
data class Shortcut(val trigger: ShortcutTrigger, val action: ShortcutAction)

