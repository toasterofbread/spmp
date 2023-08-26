package com.toasterofbread.spmp.platform

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

actual class PlatformPreferences private constructor(private val prefs: SharedPreferences) {
    companion object {
        private var instance: PlatformPreferences? = null

        fun getInstance(context: Context): PlatformPreferences {
            return getInstance(context.getSharedPreferences("com.toasterofbread.spmp.PREFERENCES", Context.MODE_PRIVATE))
        }
        private fun getInstance(prefs: SharedPreferences): PlatformPreferences {
            if (instance == null) {
                instance = PlatformPreferences(prefs)
            }
            return instance!!
        }
    }

    actual fun getString(key: String, defValue: String?): String? = prefs.getString(key, defValue)
    actual fun getStringSet(key: String, defValues: Set<String>?): Set<String>? = prefs.getStringSet(key, defValues)
    actual fun getInt(key: String, defValue: Int?): Int? {
        if (!prefs.contains(key)) {
            return defValue
        }
        return prefs.getInt(key, 0)
    }
    actual fun getLong(key: String, defValue: Long?): Long? {
        if (!prefs.contains(key)) {
            return defValue
        }
        return prefs.getLong(key, 0)
    }
    actual fun getFloat(key: String, defValue: Float?): Float? {
        if (!prefs.contains(key)) {
            return defValue
        }
        return prefs.getFloat(key, 0f)
    }
    actual fun getBoolean(key: String, defValue: Boolean?): Boolean? {
        if (!prefs.contains(key)) {
            return defValue
        }
        return prefs.getBoolean(key, false)
    }
    actual operator fun contains(key: String): Boolean = prefs.contains(key)
    
    actual fun addListener(listener: Listener): Listener {
        prefs.registerOnSharedPreferenceChangeListener(listener)
        return listener
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
        actual fun onChanged(prefs: PlatformPreferences, key: String)
    }

    actual open class Editor(private val upstream: SharedPreferences.Editor) {
        actual fun putString(key: String, value: String): Editor {
            upstream.putString(key, value)
            return this
        }

        actual fun putStringSet(
            key: String,
            values: Set<String>
        ): Editor {
            upstream.putStringSet(key, values)
            return this
        }

        actual fun putInt(key: String, value: Int): Editor {
            upstream.putInt(key, value)
            return this
        }

        actual fun putLong(key: String, value: Long): Editor {
            upstream.putLong(key, value)
            return this
        }

        actual fun putFloat(key: String, value: Float): Editor {
            upstream.putFloat(key, value)
            return this
        }

        actual fun putBoolean(key: String, value: Boolean): Editor {
            upstream.putBoolean(key, value)
            return this
        }

        actual fun remove(key: String): Editor {
            upstream.remove(key)
            return this
        }

        actual fun clear(): Editor {
            upstream.clear()
            return this
        }
    }
}