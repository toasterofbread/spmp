package com.toasterofbread.spmp.platform

import com.beust.klaxon.Klaxon
import java.io.File

actual class ProjectPreferences private constructor(private val context: PlatformContext) {
    companion object {
        private var instance: ProjectPreferences? = null

        fun getInstance(context: PlatformContext): ProjectPreferences {
            if (instance == null) {
                instance = ProjectPreferences(context)
            }
            return instance!!
        }
    }

    private var data: MutableMap<String, Any> = mutableMapOf()
    private var listeners: MutableList<Listener> = mutableListOf()

    init {
        loadData()
    }

    private fun onKeyChanged(key: String) {
        for (listener in listeners) {
            listener.onChanged(this, key)
        }
    }
    
    private fun getPreferencesFile(): File = context.getFilesDir().resolve("preferences.json")
    private fun loadData() {
        val file = getPreferencesFile()
        if (!file.exists()) {
            data.clear()
            return
        }
        
        val stream = file.inputStream()
        data = Klaxon().parse(stream)!!
        stream.close()
    }
    private fun saveData() {
        val file = getPreferencesFile()
        file.createNewFile()
        
        val stream = file.outputStream().writer()
        stream.write(Klaxon().toJsonString(data))
        stream.flush()
        stream.close()
    }

    actual fun addListener(listener: Listener): Listener {
        listeners.add(listener)
        return listener
    }

    actual fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    actual fun getString(key: String, defValue: String?): String? =
        data.getOrDefault(key, defValue) as String?

    @Suppress("UNCHECKED_CAST")
    actual fun getStringSet(key: String, defValues: Set<String>?): Set<String>? =
        data.getOrDefault(key, defValues) as Set<String>

    actual fun getInt(key: String, defValue: Int?): Int? =
        data.getOrDefault(key, defValue) as Int?

    actual fun getLong(key: String, defValue: Long?): Long? =
        data.getOrDefault(key, defValue) as Long?

    actual fun getFloat(key: String, defValue: Float?): Float? =
        data.getOrDefault(key, defValue) as Float?

    actual fun getBoolean(key: String, defValue: Boolean?): Boolean? =
        data.getOrDefault(key, defValue) as Boolean?

    actual operator fun contains(key: String): Boolean =
        data.containsKey(key)

    actual fun edit(action: Editor.() -> Unit) {
        val changed: MutableSet<String> = mutableSetOf()
        val editor = Editor(data, changed)
        action(editor)
        saveData()

        for (key in changed) {
            onKeyChanged(key)
        }
    }

    actual open class Editor(private val data: MutableMap<String, Any>, private val changed: MutableSet<String>) {

        actual fun putString(key: String, value: String): Editor {
            data[key] = value
            changed.add(key)
            return this
        }

        actual fun putStringSet(
            key: String,
            values: Set<String>
        ): Editor {
            data[key] = values
            changed.add(key)
            return this
        }

        actual fun putInt(key: String, value: Int): Editor {
            data[key] = value
            changed.add(key)
            return this
        }

        actual fun putLong(key: String, value: Long): Editor {
            data[key] = value
            changed.add(key)
            return this
        }

        actual fun putFloat(key: String, value: Float): Editor {
            data[key] = value
            changed.add(key)
            return this
        }

        actual fun putBoolean(key: String, value: Boolean): Editor {
            data[key] = value
            changed.add(key)
            return this
        }

        actual fun remove(key: String): Editor {
            data.remove(key)
            return this
        }

        actual fun clear(): Editor {
            changed.addAll(data.keys)
            data.clear()
            return this
        }
    }

    actual interface Listener {
        actual fun onChanged(prefs: ProjectPreferences, key: String)
    }
}