package com.toasterofbread.spmp.platform.playerservice

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.util.removeLastBuiltIn

interface UndoRedoAction {
    fun undo(service: PlayerService) {}
    fun redo(service: PlayerService) {}
}

internal class UndoHandler(val player: PlayerServicePlayer, val service: PlayerService) {
    private var current_action: MutableList<UndoRedoAction>? = null
    private var current_action_is_further: Boolean = false
    private val action_list: MutableList<List<UndoRedoAction>> = mutableListOf()
    private var action_head: Int by mutableIntStateOf(0)

    val undo_count: Int get() = action_head
    val redo_count: Int get() = action_list.size - undo_count

    internal data class AddAction(val song: Song, val index: Int): UndoRedoAction {
        init {
            assert(index >= 0) { index.toString() }
        }

        override fun redo(service: PlayerService) {
            super.redo(service)
            service.addSong(song, index)
            service.service_player.onUndoStateChanged()
        }
        override fun undo(service: PlayerService) {
            service.removeItem(index)
            service.service_player.onUndoStateChanged()
        }
    }
    internal data class MoveAction(val from: Int, val to: Int): UndoRedoAction {
        init {
            assert(from >= 0)
            assert(to >= 0)
        }

        override fun redo(service: PlayerService) {
            super.redo(service)
            service.moveItem(from, to)
            service.service_player.onUndoStateChanged()
        }
        override fun undo(service: PlayerService) {
            service.moveItem(to, from)
            service.service_player.onUndoStateChanged()
        }
    }
    internal data class RemoveAction(val index: Int): UndoRedoAction {
        init {
            assert(index >= 0)
        }

        private lateinit var song: Song
        override fun redo(service: PlayerService) {
            super.redo(service)
            song = service.getSong(index)!!
            service.removeItem(index)
            service.service_player.onUndoStateChanged()
        }

        override fun undo(service: PlayerService) {
            service.addSong(song, index)
            service.service_player.onUndoStateChanged()
        }
    }

    fun undoableAction(enable: Boolean? = true, action: UndoHandler.(furtherAction: (UndoHandler.() -> Unit) -> Unit) -> Unit) {
        customUndoableAction(enable) { furtherAction ->
            action {
                furtherAction {
                    it()
                    null
                }
            }
            null
        }
    }

    // If enable is null, action will only be undoable if already in an enabled undo scope
    fun customUndoableAction(
        enable: Boolean? = true,
        action: UndoHandler.(furtherAction: (UndoHandler.() -> UndoRedoAction?) -> Unit) -> UndoRedoAction?
    ) {
        if (enable == false || (enable == null && current_action == null)) {
            action(this) { it() }?.redo(service)
            return
        }

        current_action?.also { c_action ->
            val custom_action = action(this) { further ->
                handleFurtherAction(c_action, further)
            }
            if (custom_action != null) {
                performAction(custom_action)
            }
            return
        }

        synchronized(action_list) {
            val c_action: MutableList<UndoRedoAction> = mutableListOf()
            current_action = c_action

            val custom_action = action(this) { further ->
                handleFurtherAction(c_action, further)
            }
            if (custom_action != null) {
                performAction(custom_action)
            }

            commitActionList(c_action)
            current_action = null
        }
    }

    private fun handleFurtherAction(current: MutableList<UndoRedoAction>, further: UndoHandler.() -> UndoRedoAction?) {
        synchronized(action_list) {
            current_action_is_further = true
            current_action = current

            val custom_action = further(this)
            if (custom_action != null) {
                performAction(custom_action)
            }

            current_action = null
            current_action_is_further = false
        }
    }

    private fun commitActionList(actions: List<UndoRedoAction>) {
        for (i in 0 until redo_count) {
            action_list.removeLastBuiltIn()
        }
        action_list.add(actions)
        action_head++

        player.onUndoStateChanged()
    }

    fun performAction(action: UndoRedoAction) {
        synchronized(action_list) {
            action.redo(service)

            val current = current_action
            if (current != null) {
                current.add(action)
            }
        }
    }

    fun redo() {
        synchronized(action_list) {
            if (redo_count == 0) {
                return
            }
            for (action in action_list[action_head++]) {
                action.redo(service)
            }
            player.onUndoStateChanged()
        }
    }

    fun redoAll() {
        synchronized(action_list) {
            for (i in 0 until redo_count) {
                for (action in action_list[action_head++]) {
                    action.redo(service)
                }
            }
            player.onUndoStateChanged()
        }
    }

    fun undo() {
        synchronized(action_list) {
            if (undo_count == 0) {
                return
            }
            for (action in action_list[--action_head].asReversed()) {
                action.undo(service)
            }
            player.onUndoStateChanged()
        }
    }

    fun undoAll() {
        synchronized(action_list) {
            for (i in 0 until undo_count) {
                for (action in action_list[--action_head].asReversed()) {
                    action.undo(service)
                }
            }
            player.onUndoStateChanged()
        }
    }
}
