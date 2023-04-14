package com.spectre7.spmp.platform

actual class ProjectPreferences {
    actual fun addListener(listener: Listener) {
    }

    actual fun removeListener(listener: Listener) {
    }

    actual fun getString(key: String?, defValue: String?): String? {
        TODO("Not yet implemented")
    }

    actual fun getStringSet(key: String?, defValues: Set<String?>?): Set<String?>? {
        TODO("Not yet implemented")
    }

    actual fun getInt(key: String?, defValue: Int): Int {
        TODO("Not yet implemented")
    }

    actual fun getLong(key: String?, defValue: Long): Long {
        TODO("Not yet implemented")
    }

    actual fun getFloat(key: String?, defValue: Float): Float {
        TODO("Not yet implemented")
    }

    actual fun getBoolean(key: String?, defValue: Boolean): Boolean {
        TODO("Not yet implemented")
    }

    actual operator fun contains(key: String?): Boolean {
        TODO("Not yet implemented")
    }

    actual fun edit(action: ProjectPreferences.Editor.() -> Unit) {
    }

    actual open class Editor {
        actual fun putString(key: String?, value: String?): Editor {
            TODO("Not yet implemented")
        }

        actual fun putStringSet(
            key: String?,
            values: Set<String?>?
        ): Editor {
            TODO("Not yet implemented")
        }

        actual fun putInt(key: String?, value: Int): Editor {
            TODO("Not yet implemented")
        }

        actual fun putLong(key: String?, value: Long): Editor {
            TODO("Not yet implemented")
        }

        actual fun putFloat(key: String?, value: Float): Editor {
            TODO("Not yet implemented")
        }

        actual fun putBoolean(key: String?, value: Boolean): Editor {
            TODO("Not yet implemented")
        }

        actual fun remove(key: String?): Editor {
            TODO("Not yet implemented")
        }

        actual fun clear(): Editor {
            TODO("Not yet implemented")
        }
    }

    actual interface Listener {
        actual fun onChanged(prefs: ProjectPreferences, key: String)
    }
}