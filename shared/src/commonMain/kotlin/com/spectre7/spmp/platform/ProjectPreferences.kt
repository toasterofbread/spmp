package com.spectre7.spmp.platform


expect class ProjectPreferences {
    fun addListener(listener: Listener)
    fun removeListener(listener: Listener)

    fun getString(key: String?, defValue: String?): String?
    fun getStringSet(key: String?, defValues: Set<String?>?): Set<String?>?
    fun getInt(key: String?, defValue: Int): Int
    fun getLong(key: String?, defValue: Long): Long
    fun getFloat(key: String?, defValue: Float): Float
    fun getBoolean(key: String?, defValue: Boolean): Boolean
    operator fun contains(key: String?): Boolean

    fun edit(action: Editor.() -> Unit)
    open class Editor {
        fun putString(key: String?, value: String?): Editor
        fun putStringSet(key: String?, values: Set<String?>?): Editor
        fun putInt(key: String?, value: Int): Editor
        fun putLong(key: String?, value: Long): Editor
        fun putFloat(key: String?, value: Float): Editor
        fun putBoolean(key: String?, value: Boolean): Editor
        fun remove(key: String?): Editor
        fun clear(): Editor
    }

    interface Listener {
        fun onChanged(prefs: ProjectPreferences, key: String)
    }
}