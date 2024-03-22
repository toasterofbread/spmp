package com.toasterofbread.spmp.model.settings.category

import androidx.compose.material.icons.outlined.Adjust
import androidx.compose.material.icons.Icons
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.pointer.PointerButton
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getShortcutCategoryItems
import com.toasterofbread.spmp.ui.component.shortcut.trigger.*
import com.toasterofbread.spmp.model.appaction.*
import com.toasterofbread.spmp.model.appaction.action.playback.*
import com.toasterofbread.spmp.model.appaction.action.navigation.*
import com.toasterofbread.spmp.model.appaction.shortcut.Shortcut
import com.toasterofbread.spmp.model.settings.SettingsKey
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.composekit.platform.Platform
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

data object ShortcutSettings: SettingsCategory("shortcut") {
    override val keys: List<SettingsKey> = Key.entries.toList()

    override fun getPage(): CategoryPage? =
        SimplePage(
            getString("s_cat_shortcut"),
            getString("s_cat_desc_shortcut"),
            { getShortcutCategoryItems() },
            { Icons.Outlined.Adjust }
        )

    enum class Key: SettingsKey {
        // List<Shortcut>
        CONFIGURED_SHORTCUTS,
        NAVIGATE_SONG_WITH_NUMBERS;

        override val category: SettingsCategory get() = ShortcutSettings

        @Suppress("UNCHECKED_CAST")
        override fun <T> getDefaultValue(): T =
            when (this) {
                CONFIGURED_SHORTCUTS -> Json.encodeToString(getDefaultShortcuts())
                NAVIGATE_SONG_WITH_NUMBERS -> true
            } as T
    }
}

private fun getDefaultShortcuts(): List<Shortcut> =
    listOfNotNull(
        // Play / pause
        Shortcut(
            KeyboardShortcutTrigger(Key.Spacebar.keyCode),
            PlaybackAppAction(TogglePlayPlaybackAppAction())
        ),

        // Time seek
        Shortcut(
            KeyboardShortcutTrigger(Key.DirectionRight.keyCode),
            PlaybackAppAction(SeekByTimePlaybackAppAction(5000))
        ),
        Shortcut(
            KeyboardShortcutTrigger(Key.DirectionLeft.keyCode),
            PlaybackAppAction(SeekByTimePlaybackAppAction(-5000))
        ),

        // Song seek
        Shortcut(
            KeyboardShortcutTrigger(Key.DirectionRight.keyCode, listOf(KeyboardShortcutTrigger.KeyboardModifier.CTRL)),
            PlaybackAppAction(SeekNextPlaybackAppAction())
        ),
        Shortcut(
            KeyboardShortcutTrigger(Key.DirectionLeft.keyCode, listOf(KeyboardShortcutTrigger.KeyboardModifier.CTRL)),
            PlaybackAppAction(SeekPreviousPlaybackAppAction())
        ),

        // Remove current song
        Shortcut(
            KeyboardShortcutTrigger(Key.Delete.keyCode),
            SongAppAction(SongAppAction.Action.REMOVE_FROM_QUEUE)
        ),

        // Toggle player
        Shortcut(
            KeyboardShortcutTrigger(Key.Tab.keyCode),
            NavigationAppAction(TogglePlayerNavigationAction())
        ),

        // UI scale
        Shortcut(
            KeyboardShortcutTrigger(Key.Equals.keyCode, listOf(KeyboardShortcutTrigger.KeyboardModifier.CTRL)),
            OtherAppAction(OtherAppAction.Action.INCREASE_UI_SCALE)
        ),
        Shortcut(
            KeyboardShortcutTrigger(Key.Minus.keyCode, listOf(KeyboardShortcutTrigger.KeyboardModifier.CTRL)),
            OtherAppAction(OtherAppAction.Action.DECREASE_UI_SCALE)
        ),

        // Navigate back
        Shortcut(
            KeyboardShortcutTrigger(Key.Escape.keyCode),
            OtherAppAction(OtherAppAction.Action.NAVIGATE_BACK)
        ),
        Shortcut(
            MouseButtonShortcutTrigger(PointerButton.Back.index),
            OtherAppAction(OtherAppAction.Action.NAVIGATE_BACK)
        ),
        Shortcut(
            MouseButtonShortcutTrigger(5),
            OtherAppAction(OtherAppAction.Action.NAVIGATE_BACK)
        ),

        // Toggle fullscreen
        if (Platform.DESKTOP.isCurrent())
            Shortcut(
                KeyboardShortcutTrigger(Key.F11.keyCode),
                OtherAppAction(OtherAppAction.Action.TOGGLE_FULLSCREEN)
            )
        else null
    )
