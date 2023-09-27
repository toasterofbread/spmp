@file:Suppress("MemberVisibilityCanBePrivate")

package com.toasterofbread.composesettings.ui.item

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.*
import com.toasterofbread.composesettings.ui.SettingsPage
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.platform.PlatformPreferences
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.utils.*
import com.toasterofbread.utils.common.setAlpha
import com.toasterofbread.utils.composable.*

val SETTINGS_ITEM_ROUNDED_SHAPE = RoundedCornerShape(20.dp)

abstract class SettingsItem {
    lateinit var context: PlatformContext

    private var initialised = false
    fun initialise(context: PlatformContext, prefs: PlatformPreferences = context.getPrefs(), default_provider: (String) -> Any) {
        if (initialised) {
            return
        }
        this.context = context
        initialiseValueStates(prefs, default_provider)
        initialised = true
    }

    protected abstract fun initialiseValueStates(prefs: PlatformPreferences, default_provider: (String) -> Any)
    protected abstract fun releaseValueStates(prefs: PlatformPreferences)

    abstract fun resetValues()

    @Composable
    abstract fun GetItem(
        theme: Theme,
        openPage: (Int, Any?) -> Unit,
        openCustomPage: (SettingsPage) -> Unit
    )
    
    companion object {
        @Composable
        fun ItemTitleText(text: String?, theme: Theme, modifier: Modifier = Modifier, max_lines: Int = 1) {
            if (text?.isNotBlank() == true) {
                WidthShrinkText(
                    text,
                    modifier,
                    style = MaterialTheme.typography.titleMedium.copy(color = theme.on_background),
                    max_lines = max_lines
                )
            }
        }
            
        @Composable
        fun ItemText(text: String?, theme: Theme) {
            ItemText(text, theme.on_background.setAlpha(0.75f))
        }

        @Composable
        fun ItemText(text: String?, colour: Color) {
            if (text?.isNotBlank() == true) {
                LinkifyText(text, style = MaterialTheme.typography.bodySmall.copy(color = colour))
            }
        }
    }
}

interface BasicSettingsValueState<T: Any> {
    fun get(): T
    fun set(value: T)

    fun init(prefs: PlatformPreferences, defaultProvider: (String) -> Any): BasicSettingsValueState<T>
    fun release(prefs: PlatformPreferences)

    fun reset()
    fun save()
    fun getDefault(defaultProvider: (String) -> Any): T
}

@Suppress("UNCHECKED_CAST")
class SettingsValueState<T: Any>(
    val key: String,
    private val onChanged: ((value: T) -> Unit)? = null,
    private val getValueConverter: (Any?) -> T? = { it as T },
    private val setValueConverter: (T) -> Any = { it }
): BasicSettingsValueState<T>, State<T> {
    var autosave: Boolean = true

    private lateinit var prefs: PlatformPreferences
    private lateinit var defaultProvider: (String) -> Any
    private var listener: PlatformPreferences.Listener? = null
    private var _value: T? by mutableStateOf(null)

    override val value: T get() = _value!!

    override fun get(): T = _value!!
    override fun set(value: T) {
        check(_value != null) { "State has not been initialised" }

        if (_value == value) {
            return
        }

        _value = value
        if (autosave) {
            save()
        }
        onChanged?.invoke(value)
    }

    private fun updateValue() {
        val default = defaultProvider(key) as T
        _value = getValueConverter(when (default) {
            is Boolean -> prefs.getBoolean(key, default as Boolean)
            is Float -> prefs.getFloat(key, default as Float)
            is Int -> prefs.getInt(key, default as Int)
            is Long -> prefs.getLong(key, default as Long)
            is String -> prefs.getString(key, default as String)
            is Set<*> -> prefs.getStringSet(key, default as Set<String>)
            else -> throw ClassCastException()
        })
    }

    override fun init(prefs: PlatformPreferences, defaultProvider: (String) -> Any): SettingsValueState<T> {
        if (_value != null) {
            return this
        }

        this.prefs = prefs
        this.defaultProvider = defaultProvider

        updateValue()

        listener =
            object : PlatformPreferences.Listener {
                override fun onChanged(prefs: PlatformPreferences, key: String) {
                    if (key == this@SettingsValueState.key) {
                        updateValue()
                    }
                }
            }
            .also {
                prefs.addListener(it)
            }

        return this
    }

    override fun release(prefs: PlatformPreferences) {
        listener?.also {
            prefs.removeListener(it)
        }
    }

    override fun reset() {
        _value = getValueConverter(defaultProvider(key) as T)!!
        if (autosave) {
            save()
        }
        onChanged?.invoke(_value!!)
    }

    override fun save() {
        prefs.edit {
            val value = setValueConverter(get())
            when (value) {
                is Boolean -> putBoolean(key, value)
                is Float -> putFloat(key, value)
                is Int -> putInt(key, value)
                is Long -> putLong(key, value)
                is String -> putString(key, value)
                is Set<*> -> putStringSet(key, value as Set<String>)
                else -> throw ClassCastException(value::class.toString())
            }
        }
    }

    override fun getDefault(defaultProvider: (String) -> Any): T = defaultProvider(key) as T
}
