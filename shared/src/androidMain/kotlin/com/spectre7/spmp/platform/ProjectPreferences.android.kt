package com.spectre7.spmp.platform

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

actual class ProjectPreferences private constructor(private val prefs: SharedPreferences) {
    companion object {
        private var instance: ProjectPreferences? = null

        fun getInstance(context: Context): ProjectPreferences {
            return getInstance(context.getSharedPreferences("com.spectre7.spmp.PREFERENCES", Context.MODE_PRIVATE))
        }
        private fun getInstance(prefs: SharedPreferences): ProjectPreferences {
            if (instance == null) {
                instance = ProjectPreferences(prefs)
            }
            return instance!!
        }
    }

    actual fun getString(key: String?, defValue: String?): String? = prefs.getString(key, defValue)
    actual fun getStringSet(key: String?, defValues: Set<String?>?): Set<String?>? = prefs.getStringSet(key, defValues)
    actual fun getInt(key: String?, defValue: Int): Int = prefs.getInt(key, defValue)
    actual fun getLong(key: String?, defValue: Long): Long = prefs.getLong(key, defValue)
    actual fun getFloat(key: String?, defValue: Float): Float = prefs.getFloat(key, defValue)
    actual fun getBoolean(key: String?, defValue: Boolean): Boolean = prefs.getBoolean(key, defValue)
    actual operator fun contains(key: String?): Boolean = prefs.contains(key)
    
    actual fun addListener(listener: Listener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    actual fun removeListener(listener: Listener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    actual fun edit(action: Editor.() -> Unit) {
        prefs.edit {
            action(Editor(this))
        }
    }

    actual interface Listener: SharedPreferences.OnSharedPreferenceChangeListener {
        override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String) {
            onChanged(getInstance(prefs), key)
        }
        actual fun onChanged(prefs: ProjectPreferences, key: String)
    }

    actual open class Editor(private val upstream: SharedPreferences.Editor) {
        actual fun putString(key: String?, value: String?): Editor {
            upstream.putString(key, value)
            return this
        }

        actual fun putStringSet(
            key: String?,
            values: Set<String?>?
        ): Editor {
            upstream.putStringSet(key, values)
            return this
        }

        actual fun putInt(key: String?, value: Int): Editor {
            upstream.putInt(key, value)
            return this
        }

        actual fun putLong(key: String?, value: Long): Editor {
            upstream.putLong(key, value)
            return this
        }

        actual fun putFloat(key: String?, value: Float): Editor {
            upstream.putFloat(key, value)
            return this
        }

        actual fun putBoolean(key: String?, value: Boolean): Editor {
            upstream.putBoolean(key, value)
            return this
        }

        actual fun remove(key: String?): Editor {
            upstream.remove(key)
            return this
        }

        actual fun clear(): Editor {
            upstream.clear()
            return this
        }
    }
}