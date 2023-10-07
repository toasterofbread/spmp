package com.toasterofbread.spmp.platform.playerservice

import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.utils.common.synchronizedBlock

interface UndoRedoAction {
    fun undo(service: PlatformPlayerService)
    fun redo(service: PlatformPlayerService)
}

internal class UndoHandler(val player: PlayerServicePlayer, val service: PlatformPlayerService) {
    private var current_action: MutableList<UndoRedoAction>? = null
    private var current_action_is_further: Boolean = false
    private val action_list: MutableList<List<UndoRedoAction>> = mutableListOf()
    private var action_head: Int = 0

    val undo_count: Int get() = action_head
    val redo_count: Int get() = action_list.size - undo_count

    internal class AddAction(val song: Song, val index: Int): UndoRedoAction {
        init {
            assert(index >= 0) { index.toString() }
        }

        override fun redo(service: PlatformPlayerService) {
            service.addSong(song, index)
//            listeners.forEach { it.onSongAdded(index, item.getSong()) }
        }
        override fun undo(service: PlatformPlayerService) {
            service.removeSong(index)
//            listeners.forEach { it.onSongRemoved(index) }
        }
    }
    internal class MoveAction(val from: Int, val to: Int): UndoRedoAction {
        init {
            assert(from >= 0)
            assert(to >= 0)
        }

        override fun redo(service: PlatformPlayerService) {
            service.moveSong(from, to)
//            listeners.forEach { it.onSongMoved(from, to) }
        }
        override fun undo(service: PlatformPlayerService) {
            service.moveSong(to, from)
//            listeners.forEach { it.onSongMoved(to, from) }
        }
    }
    internal class RemoveAction(val index: Int): UndoRedoAction {
        init {
            assert(index >= 0)
        }

        private lateinit var song: Song
        override fun redo(service: PlatformPlayerService) {
            song = service.getSong(index)!!
            service.removeSong(index)
//            listeners.forEach { it.onSongRemoved(index) }
        }

        override fun undo(service: PlatformPlayerService) {
            service.addSong(song, index)
//            listeners.forEach { it.onSongAdded(index, item.getSong()) }
        }
    }

    fun undoableAction(action: UndoHandler.(furtherAction: (UndoHandler.() -> Unit) -> Unit) -> Unit) {
        customUndoableAction { furtherAction ->
            action {
                furtherAction {
                    it()
                    null
                }
            }
            null
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

    fun customUndoableAction(action: UndoHandler.(furtherAction: (UndoHandler.() -> UndoRedoAction?) -> Unit) -> UndoRedoAction?) {
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

    private fun commitActionList(actions: List<UndoRedoAction>) {
        for (i in 0 until redo_count) {
            action_list.removeLast()
        }
        action_list.add(actions)
        action_head++

//        listeners.forEach { it.onUndoStateChanged() }
    }

    fun performAction(action: UndoRedoAction) {
        synchronizedBlock(action_list) {
            action.redo(service)

            val current = current_action
            if (current != null) {
                current.add(action)
            }
            else if (!current_action_is_further) {
                // If not being performed as part of an undoableAction, commit as a single aciton
                commitActionList(listOf(action))
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
        }
    }

    fun redoAll() {
        synchronized(action_list) {
            for (i in 0 until redo_count) {
                for (action in action_list[action_head++]) {
                    action.redo(service)
                }
            }
//            listeners.forEach { it.onUndoStateChanged() }
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
//            listeners.forEach { it.onUndoStateChanged() }
        }
    }

    fun undoAll() {
        synchronized(action_list) {
            for (i in 0 until undo_count) {
                for (action in action_list[--action_head].asReversed()) {
                    action.undo(service)
                }
            }

            service.service_state
//            listeners.forEach { it.onUndoStateChanged() }
        }
    }
}
