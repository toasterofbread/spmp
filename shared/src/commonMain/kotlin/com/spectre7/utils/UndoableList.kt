package com.spectre7.utils

class UndoableList<T>() {

    private var current_action: MutableList<Action>? = null

    private abstract inner class Action() {
        protected val is_undoable: Boolean get() = current_action != null

        abstract fun redo()
        abstract fun undo()
    }
    private inner class AddAction(val item: T, val index: Int? = null): Action() {
        override fun redo() {
            if (index == null) list.add(item)
            else list.add(index, item)
        }
        override fun undo() {
            if (index == null) list.removeLast()
            else list.removeAt(index)
        }
    }
    private inner class RemoveAction(val index: Int): Action() {
        private var item: T? = null
        override fun redo() {
            item = list.removeAt(index)
        }
        override fun undo() {
            list.add(index, item as T)
        }
    }
    private inner class ClearAction(): Action() {
        private var items: List<T>? = null
        override fun redo() {
            if (items == null && is_undoable) {
                items = list.toList()
            }
            list.clear()
        }
        override fun undo() {
            assert(items != null && list.isEmpty())
            list.addAll(items!!)
        }
    }

    private val action_list: MutableList<List<Action>> = mutableListOf()
    private var action_head: Int = 0

    private val list: MutableList<T> = mutableListOf()

    private fun List<Action>.redo() {
        for (action in this) {
            action.redo()
        }
    }
    private fun List<Action>.undo() {
        for (action in asReversed()) {
            action.undo()
        }
    }

    private fun onActionPerformed(action: Action) {
        action.redo()
        current_action?.add(action)
    }

    val undo_count: Int get() = action_head
    val redo_count: Int get() = action_list.size - undo_count

    fun undo() {
        synchronized(action_list) {
            check(undo_count != 0)
            action_list[--action_head].undo()
        }
    }

    fun redo() {
        synchronized(action_list) {
            check(redo_count != 0)
            action_list[action_head++].redo()
        }
    }

    fun undoableAction(action: UndoableList<T>.() -> Unit) {
        synchronized(action_list) {
            assert(current_action == null)
            current_action = mutableListOf()
            action(this)

            for (i in 0 until redo_count) {
                action_list.removeLast()
            }
            action_list.add(current_action!!)
            action_head++

            current_action = null
        }
    }

    fun add(item: T) {
        onActionPerformed(AddAction(item))
    }
    fun add(index: Int, item: T) {
        onActionPerformed(AddAction(item, index))
    }
    fun removeAt(index: Int) {
        onActionPerformed(RemoveAction(index))
    }
    fun clear() {
        onActionPerformed(ClearAction())
    }
}
