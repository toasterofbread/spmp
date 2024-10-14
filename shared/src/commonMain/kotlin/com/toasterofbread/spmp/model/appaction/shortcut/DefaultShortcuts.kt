package com.toasterofbread.spmp.model.appaction.shortcut

import androidx.compose.ui.input.key.Key
import com.toasterofbread.spmp.ui.component.shortcut.trigger.*
import com.toasterofbread.spmp.ui.layout.apppage.AppPage
import com.toasterofbread.spmp.model.appaction.*
import com.toasterofbread.spmp.model.appaction.action.playback.*
import com.toasterofbread.spmp.model.appaction.action.navigation.*

fun getDefaultShortcuts(): List<Shortcut> =
    listOf(
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

        // Navigate to feed
        Shortcut(
            KeyboardShortcutTrigger(Key.Home.keyCode),
            NavigationAppAction(AppPageNavigationAction(AppPage.Type.SONG_FEED))
        ),

        // Open settings
        Shortcut(
            KeyboardShortcutTrigger(Key.Comma.keyCode, listOf(KeyboardShortcutTrigger.KeyboardModifier.CTRL)),
            NavigationAppAction(AppPageNavigationAction(AppPage.Type.SETTINGS))
        ),

        // Open search
        Shortcut(
            KeyboardShortcutTrigger(Key.F.keyCode, listOf(KeyboardShortcutTrigger.KeyboardModifier.CTRL)),
            NavigationAppAction(AppPageNavigationAction(AppPage.Type.SEARCH))
        ),

        // Navigate back
        Shortcut(
            KeyboardShortcutTrigger(Key.Escape.keyCode),
            OtherAppAction(OtherAppAction.Action.NAVIGATE_BACK)
        )
    ) + getPlatformDefaultShortcuts()

internal expect fun getPlatformDefaultShortcuts(): List<Shortcut>
