package com.toasterofbread.spmp.ui.shortcut

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf

val SHORTCUT_INDICATOR_SHAPE get() = CircleShape
const val SHORTCUT_INDICATOR_GROUP_ANIM_DURATION_MS: Long = 30
const val SHORTCUT_INDICATOR_SHOW_DELAY_MS: Long = 200

enum class ShortcutModifier {
    CTRL;
}

enum class ShortcutGroup {
    SIDEBAR_NAVIGATION;

    val modifiers: List<ShortcutModifier>
        get() = when (this) {
            SIDEBAR_NAVIGATION -> listOf(ShortcutModifier.CTRL)
        }
}

data class PressedShortcutModifiers(
    val modifiers: List<ShortcutModifier>
)

val LocalPressedShortcutModifiers: ProvidableCompositionLocal<PressedShortcutModifiers> = compositionLocalOf { PressedShortcutModifiers(emptyList()) }
