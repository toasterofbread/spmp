package com.spectre7.utils

class UndoableList<T>() {

    private var current_action: MutableList<Action>? = null

    private abstract inner class Action() {
        protected val is_undoable: Boolean get() = current_action != null

        abstract fun redo()
        abstract fun undo()
    }
    private class AddAction(val item: T, val index: Int? = null): Action() {
        override fun redo() {
            if (index == null) list.add(item)
            else list.add(index, item)
        }
        override fun undo() {
            if (index == null) list.removeLast()
            else list.remove(index)
        }
    }
    private class RemoveAction(val index: Int): Action() {
        private lateinit var item: T
        override fun redo() {
            item = list.removeAt(index)
        }
        override fun undo() {
            list.add(index, item)
        }
    }
    private class ClearAction(): Action() {
        private var items: List<T>? = null
        override fun redo() {
            if (items == null && undoable) {
                items = list.toList()
            }
            list.clear()
        }
        override fun undo() {
            assert(items != null && list.isEmpty())
            list.addAll(items)
        }
    }

    private val list: MutableList<T> = mutableListOf()
    private val action_list: MutableList<List<Action>>
    private var action_head: Int = 0

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
            action_list.add(current_action)
            action_head++

            current_action = null
        }
    }

    fun add(item: T) {
        onActionPerformed(AddAction(list, item))
    }
    fun add(index: Int, item: T) {
        onActionPerformed(AddAction(list, item, index))
    }
    fun removeAt(index: Int) {
        onActionPerformed(RemoveAction(list, index))
    }
    fun clear() {
        onActionPerformed(ClearAction(list))
    }
}
