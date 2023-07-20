@file:Suppress("MemberVisibilityCanBePrivate")

package com.toasterofbread.settings.ui.item

import androidx.compose.animation.*
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.github.krottv.compose.sliders.DefaultThumb
import com.github.krottv.compose.sliders.DefaultTrack
import com.github.krottv.compose.sliders.ListenOnPressed
import com.github.krottv.compose.sliders.SliderValueHorizontal
import com.toasterofbread.composesettings.ui.SettingsPage
import com.toasterofbread.spmp.platform.LargeDropdownMenu
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.platform.ProjectPreferences
import com.toasterofbread.spmp.platform.composable.PlatformAlertDialog
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.utils.*
import com.toasterofbread.utils.composable.*
import kotlin.math.roundToInt

val SETTINGS_ITEM_ROUNDED_SHAPE = RoundedCornerShape(20.dp)

abstract class SettingsItem {
    lateinit var context: PlatformContext

    private var initialised = false
    fun initialise(context: PlatformContext, prefs: ProjectPreferences, default_provider: (String) -> Any) {
        if (initialised) {
            return
        }
        this.context = context
        initialiseValueStates(prefs, default_provider)
        initialised = true
    }

    protected abstract fun initialiseValueStates(prefs: ProjectPreferences, default_provider: (String) -> Any)
    abstract fun resetValues()

    @Composable
    abstract fun GetItem(
        theme: Theme,
        openPage: (Int) -> Unit,
        openCustomPage: (SettingsPage) -> Unit
    )

    @Composable
    protected fun ItemTitleText(text: String?, theme: Theme, modifier: Modifier = Modifier) {
        if (text?.isNotBlank() == true) {
            WidthShrinkText(
                text,
                modifier,
                style = MaterialTheme.typography.titleMedium.copy(color = theme.on_background)
            )
        }
    }

    @Composable
    protected fun ItemText(text: String?, colour: Color) {
        if (text?.isNotBlank() == true) {
            Text(text, color = colour, style = MaterialTheme.typography.bodySmall)
        }
    }

    @Composable
    protected fun ItemText(text: String?, theme: Theme) {
        ItemText(text, theme.on_background.setAlpha(0.75f))
    }
}

interface BasicSettingsValueState<T: Any> {
    var value: T
    fun init(prefs: ProjectPreferences, defaultProvider: (String) -> Any): BasicSettingsValueState<T>
    fun reset()
    fun save()
    fun getDefault(defaultProvider: (String) -> Any): T
}

@Suppress("UNCHECKED_CAST", "IMPLICIT_CAST_TO_ANY")
class SettingsValueState<T: Any>(
    val key: String,
    private val onChanged: ((value: T) -> Unit)? = null,
    private val converter: (Any?) -> T? = { it as T }
): BasicSettingsValueState<T> {
    var autosave: Boolean = true

    private lateinit var prefs: ProjectPreferences
    private lateinit var defaultProvider: (String) -> Any
    private var _value: T? by mutableStateOf(null)

    override var value: T
        get() = _value!!
        set(new_value) {
            if (_value == null) {
                throw IllegalStateException("State has not been initialised")
            }
            if (_value == new_value) {
                return
            }

            _value = new_value
            if (autosave) {
                save()
            }
            onChanged?.invoke(new_value)
        }

    override fun init(prefs: ProjectPreferences, defaultProvider: (String) -> Any): SettingsValueState<T> {
        if (_value != null) {
            return this
        }

        this.prefs = prefs
        this.defaultProvider = defaultProvider

        val default = defaultProvider(key) as T
        _value = converter(when (default!!) {
            is Boolean -> prefs.getBoolean(key, default as Boolean)
            is Float -> prefs.getFloat(key, default as Float)
            is Int -> prefs.getInt(key, default as Int)
            is Long -> prefs.getLong(key, default as Long)
            is String -> prefs.getString(key, default as String)
            is Set<*> -> prefs.getStringSet(key, default as Set<String>)
            else -> throw ClassCastException()
        })

        return this
    }

    override fun reset() {
        _value = converter(defaultProvider(key) as T)!!
        if (autosave) {
            save()
        }
        onChanged?.invoke(_value!!)
    }

    override fun save() {
        prefs.edit {
            when (value!!) {
                is Boolean -> putBoolean(key, value as Boolean)
                is Float -> putFloat(key, value as Float)
                is Int -> putInt(key, value as Int)
                is Long -> putLong(key, value as Long)
                is String -> putString(key, value as String)
                is Set<*> -> putStringSet(key, value as Set<String>)
                else -> throw ClassCastException()
            }
        }
    }

    override fun getDefault(defaultProvider: (String) -> Any): T = defaultProvider(key) as T
}
