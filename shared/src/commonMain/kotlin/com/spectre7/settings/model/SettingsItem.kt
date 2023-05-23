@file:Suppress("MemberVisibilityCanBePrivate")

package com.spectre7.settings.model

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
import com.spectre7.composesettings.ui.SettingsPage
import com.spectre7.spmp.platform.LargeDropdownMenu
import com.spectre7.spmp.platform.PlatformContext
import com.spectre7.spmp.platform.ProjectPreferences
import com.spectre7.spmp.platform.composable.PlatformAlertDialog
import com.spectre7.spmp.resources.getStringTODO
import com.spectre7.spmp.ui.theme.Theme
import com.spectre7.utils.*
import com.spectre7.utils.composable.*
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
    protected fun ItemTitleText(text: String?, theme: Theme) {
        if (text?.isNotBlank() == true) {
            WidthShrinkText(text, style = LocalTextStyle.current.copy(color = theme.on_background))
        }
    }

    @Composable
    protected fun ItemText(text: String?, colour: Color, font_size: TextUnit = TextUnit.Unspecified) {
        if (text?.isNotBlank() == true) {
            Text(text, color = colour, fontSize = font_size)
        }
    }

    @Composable
    protected fun ItemText(text: String?, theme: Theme, font_size: TextUnit = TextUnit.Unspecified) {
        ItemText(text, theme.on_background.setAlpha(0.75f), font_size)
    }
}

class SettingsGroup(var title: String?): SettingsItem() {
    override fun initialiseValueStates(prefs: ProjectPreferences, default_provider: (String) -> Any) {}
    override fun resetValues() {}

    @Composable
    override fun GetItem(
        theme: Theme,
        openPage: (Int) -> Unit,
        openCustomPage: (SettingsPage) -> Unit
    ) {
        title?.also {
            Text(it, color = theme.vibrant_accent, fontSize = 20.sp, fontWeight = FontWeight.Light)
        }
    }
}

class SettingsItemComposable(val composable: @Composable () -> Unit): SettingsItem() {
    override fun initialiseValueStates(prefs: ProjectPreferences, default_provider: (String) -> Any) {}
    override fun resetValues() {}

    @Composable
    override fun GetItem(theme: Theme, openPage: (Int) -> Unit, openCustomPage: (SettingsPage) -> Unit) {
        composable()
    }
}

class SettingsItemInfoText(val text: String): SettingsItem() {
    override fun initialiseValueStates(prefs: ProjectPreferences, default_provider: (String) -> Any) {}
    override fun resetValues() {}

    @Composable
    override fun GetItem(theme: Theme, openPage: (Int) -> Unit, openCustomPage: (SettingsPage) -> Unit) {
        Text(text)
    }
}

class SettingsItemSpacer(val spacing: Dp): SettingsItem() {
    override fun initialiseValueStates(prefs: ProjectPreferences, default_provider: (String) -> Any) {}
    override fun resetValues() {}

    @Composable
    override fun GetItem(theme: Theme, openPage: (Int) -> Unit, openCustomPage: (SettingsPage) -> Unit) {
        Spacer(Modifier.height(spacing))
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

// TODO Styling
class SettingsItemTextField(
    val state: BasicSettingsValueState<String>,
    val title: String?,
    val subtitle: String?,
    val single_line: Boolean = true
): SettingsItem() {
    override fun initialiseValueStates(prefs: ProjectPreferences, default_provider: (String) -> Any) {
        state.init(prefs, default_provider)
    }

    override fun resetValues() {
        state.reset()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun GetItem(theme: Theme, openPage: (Int) -> Unit, openCustomPage: (SettingsPage) -> Unit) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            ItemTitleText(title, theme)
            ItemText(subtitle, theme)
            TextField(state.value, { state.value = it }, Modifier.fillMaxWidth(), singleLine = single_line)
        }
    }

}
class SettingsItemToggle(
    val state: BasicSettingsValueState<Boolean>,
    val title: String?,
    val subtitle: String?,
    val checker: ((target: Boolean, setLoading: (Boolean) -> Unit, (allow_change: Boolean) -> Unit) -> Unit)? = null
): SettingsItem() {

    private var loading: Boolean by mutableStateOf(false)

    override fun initialiseValueStates(prefs: ProjectPreferences, default_provider: (String) -> Any) {
        state.init(prefs, default_provider)
    }

    override fun resetValues() {
        state.reset()
    }

    @Composable
    override fun GetItem(
        theme: Theme,
        openPage: (Int) -> Unit,
        openCustomPage: (SettingsPage) -> Unit
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                ItemTitleText(title, theme)
                ItemText(subtitle, theme)
            }

            Crossfade(loading) {
                if (it) {
                    CircularProgressIndicator(color = theme.on_background)
                }
                else {
                    Switch(
                        state.value,
                        onCheckedChange = null,
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (checker == null) {
                                state.value = !state.value
                                return@clickable
                            }

                            checker.invoke(
                                !state.value,
                                { l ->
                                    loading = l
                                }
                            ) { allow_change ->
                                if (allow_change) {
                                    state.value = !state.value
                                }
                                loading = false
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = theme.vibrant_accent,
                            checkedTrackColor = theme.vibrant_accent.setAlpha(0.5f)
                        )
                    )
                }
            }
        }
    }
}

class SettingsItemSlider(
    val state: BasicSettingsValueState<out Number>,
    val title: String?,
    val subtitle: String?,
    val min_label: String? = null,
    val max_label: String? = null,
    val steps: Int = 0,
    val range: ClosedFloatingPointRange<Float> = 0f .. 1f,
    val getValueText: ((value: Float) -> String?)? = { it.roundToInt().toString() }
): SettingsItem() {

    private var is_int: Boolean = false
    private var value_state by mutableStateOf(0f)

    @Suppress("UNCHECKED_CAST")
    fun setValue(value: Float) {
        value_state = value
        if (is_int) {
            (state as BasicSettingsValueState<Int>).value = value.roundToInt()
        } else {
            (state as BasicSettingsValueState<Float>).value = value
        }
    }

    fun getValue(): Float {
        return value_state
    }

    override fun initialiseValueStates(prefs: ProjectPreferences, default_provider: (String) -> Any) {
        state.init(prefs, default_provider)
        value_state = state.value.toFloat()
        is_int = when (state.getDefault(default_provider)) {
            is Float -> false
            is Int -> true
            else -> throw NotImplementedError(state.getDefault(default_provider).javaClass.name)
        }
    }

    override fun resetValues() {
        state.reset()
        value_state = state.value.toFloat()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun GetItem(
        theme: Theme,
        openPage: (Int) -> Unit,
        openCustomPage: (SettingsPage) -> Unit
    ) {
        var show_edit_dialog by remember { mutableStateOf(false) }

        if (show_edit_dialog) {
            var text by remember { mutableStateOf((if (is_int) getValue().roundToInt() else getValue()).toString()) }
            var error by remember { mutableStateOf<String?>(null) }

            PlatformAlertDialog(
                {
                    show_edit_dialog = false
                },
                confirmButton = {
                    FilledTonalButton(
                        {
                            try {
                                setValue(if (is_int) text.toInt().toFloat() else text.toFloat())
                                show_edit_dialog = false
                            }
                            catch(_: NumberFormatException) {}
                        },
                        enabled = error == null
                    ) {
                        Text("Done")
                    }
                },
                dismissButton = { TextButton( { show_edit_dialog = false } ) { Text("Cancel") } },
                title = { ItemTitleText(title ?: "Edit field", theme) },
                text = {
                    OutlinedTextField(
                        value = text,
                        isError = error != null,
                        label = {
                            Crossfade(error) { error_text ->
                                if (error_text != null) {
                                    Text(error_text)
                                }
                            }
                        },
                        onValueChange = {
                            text = it

                            try {
                                val value: Float = if (is_int) text.toInt().toFloat() else text.toFloat()
                                if (!range.contains(value)) {
                                    error = getStringTODO("Value is out of range ($range)")
                                    return@OutlinedTextField
                                }

                                error = null
                            }
                            catch(_: NumberFormatException) {
                                error = if (is_int) getStringTODO("Value is not an integer") else getStringTODO("Value is not a float")
                            }
                        },
                        singleLine = true
                    )
                }
            )
        }

        Column(Modifier.fillMaxWidth()) {
            if (title != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ItemTitleText(title, theme)
                    IconButton({ show_edit_dialog = true }, Modifier.size(25.dp)) {
                        Icon(Icons.Filled.Edit, null)
                    }
                }
            }

            ItemText(subtitle, theme)

            Spacer(Modifier.requiredHeight(10.dp))

            if (state is SettingsValueState) {
                state.autosave = false
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (min_label != null) {
                    ItemText(min_label, theme, 12.sp)
                }
                SliderValueHorizontal(
                    value = getValue(),
                    onValueChange = { setValue(it) },
                    onValueChangeFinished = {
                        state.save()
                    },
                    thumbSizeInDp = DpSize(12.dp, 12.dp),
                    track = { a, b, c, d, e ->
                        DefaultTrack(a, b, c, d, e,
                            theme.vibrant_accent.setAlpha(0.5f),
                            theme.vibrant_accent,
                            colorTickProgress = theme.vibrant_accent.getContrasted().setAlpha(0.5f)
                        )
                    },
                    thumb = { modifier, offset, interaction_source, enabled, thumb_size ->
                        val colour = theme.vibrant_accent
                        val scale_on_press = 1.15f
                        val animation_spec = SpringSpec<Float>(0.65f)
                        val value_text = getValueText?.invoke(getValue())

                        if (value_text != null) {
                            MeasureUnconstrainedView({ ItemText(value_text, theme) }) { width, height ->

                                var is_pressed by remember { mutableStateOf(false) }
                                interaction_source.ListenOnPressed { is_pressed = it }
                                val scale: Float by animateFloatAsState(
                                    if (is_pressed) scale_on_press else 1f,
                                    animationSpec = animation_spec
                                )

                                Column(
                                    Modifier
                                        .offset(with(LocalDensity.current) { offset - (width.toDp() / 2) + 12.dp })
                                        .requiredHeight(55.dp)
                                        .graphicsLayer(scale, scale),
                                    verticalArrangement = Arrangement.Bottom,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Spacer(
                                        Modifier
                                            .size(12.dp)
                                            .background(
                                                if (enabled) colour else
                                                    colour.setAlpha(0.6f), CircleShape
                                            )
                                    )
                                    ItemText(value_text, theme)
                                }
                            }
                        }
                        else {
                            DefaultThumb(
                                modifier,
                                offset,
                                interaction_source,
                                true,
                                thumb_size,
                                colour,
                                scale_on_press,
                                animation_spec
                            )
                        }
                    },
                    steps = steps,
                    modifier = Modifier.weight(1f),
                    valueRange = range
                )
                if (max_label != null) {
                    ItemText(max_label, theme, 12.sp)
                }
            }
        }
    }
}

class SettingsItemMultipleChoice(
    val state: BasicSettingsValueState<Int>,
    val title: String?,
    val subtitle: String?,
    val choice_amount: Int,
    val radio_style: Boolean,
    val get_choice: (Int) -> String,
): SettingsItem() {

    override fun initialiseValueStates(prefs: ProjectPreferences, default_provider: (String) -> Any) {
        state.init(prefs, default_provider)
    }

    override fun resetValues() {
        state.reset()
    }

    @Composable
    override fun GetItem(
        theme: Theme,
        openPage: (Int) -> Unit,
        openCustomPage: (SettingsPage) -> Unit
    ) {
        Column {
            Column(Modifier.fillMaxWidth()) {
                ItemTitleText(title, theme)
                ItemText(subtitle, theme, 15.sp)

                Spacer(Modifier.height(10.dp))

                if (radio_style) {
                    Column(Modifier.padding(start = 15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (i in 0 until choice_amount) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .border(
                                        Dp.Hairline,
                                        theme.on_background,
                                        SETTINGS_ITEM_ROUNDED_SHAPE
                                    )
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp)
                                    .clickable(
                                        remember { MutableInteractionSource() },
                                        null
                                    ) { state.value = i }
                            ) {
                                Text(get_choice(i))
                                RadioButton(i == state.value, onClick = { state.value = i }, colors = RadioButtonDefaults.colors(theme.vibrant_accent))
                            }
                        }
                    }
                }
                else {
                    Column(Modifier.padding(start = 15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (i in 0 until choice_amount) {

                            val colour = remember(i) { Animatable(if (state.value == i) theme.vibrant_accent else Color.Transparent) }
                            LaunchedEffect(state.value, theme.vibrant_accent) {
                                colour.animateTo(if (state.value == i) theme.vibrant_accent else Color.Transparent, TweenSpec(150))
                            }

                            Box(
                                contentAlignment = Alignment.CenterStart,
                                modifier = Modifier
                                    .border(
                                        Dp.Hairline,
                                        theme.on_background,
                                        SETTINGS_ITEM_ROUNDED_SHAPE
                                    )
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .clickable(remember { MutableInteractionSource() }, null) {
                                        state.value = i
                                    }
                                    .background(colour.value, SETTINGS_ITEM_ROUNDED_SHAPE)
                            ) {
                                Box(Modifier.padding(horizontal = 10.dp)) {
                                    Text(get_choice(i), color = if (state.value == i) theme.on_accent else theme.on_background)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

class SettingsItemDropdown(
    val state: BasicSettingsValueState<Int>,
    val title: String,
    val subtitle: String?,
    val item_count: Int,
    val getButtonItem: ((Int) -> String)? = null,
    val getItem: (Int) -> String,
): SettingsItem() {

    override fun initialiseValueStates(prefs: ProjectPreferences, default_provider: (String) -> Any) {
        state.init(prefs, default_provider)
    }

    override fun resetValues() {
        state.reset()
    }

    @Composable
    override fun GetItem(
        theme: Theme,
        openPage: (Int) -> Unit,
        openCustomPage: (SettingsPage) -> Unit
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {

            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                ItemTitleText(title, theme)
                ItemText(subtitle, theme)
            }

            var open by remember { mutableStateOf(false) }

            Button(
                { open = !open },
                Modifier.requiredHeight(40.dp),
                shape = SETTINGS_ITEM_ROUNDED_SHAPE,
                colors = ButtonDefaults.buttonColors(
                    containerColor = theme.vibrant_accent,
                    contentColor = theme.on_accent
                )
            ) {
                Text(getButtonItem?.invoke(state.value) ?: getItem(state.value))
                Icon(
                    Icons.Filled.ArrowDropDown,
                    null,
                    tint = theme.on_accent
                )
            }

            Box(contentAlignment = Alignment.CenterEnd) {
                MaterialTheme(
                    shapes = MaterialTheme.shapes.copy(extraSmall = SETTINGS_ITEM_ROUNDED_SHAPE)
                ){
                    LargeDropdownMenu(
                        open,
                        { open = false },
                        item_count,
                        state.value,
                        getItem,
                        selected_item_colour = Theme.current.vibrant_accent
                    ) {
                        state.value = it
                        open = false
                    }
                }
            }
        }
    }
}

class SettingsItemSubpage(
    val title: String,
    val subtitle: String?,
    val target_page: Int,
): SettingsItem() {

    override fun initialiseValueStates(prefs: ProjectPreferences, default_provider: (String) -> Any) {}

    override fun resetValues() {}

    @Composable
    override fun GetItem(
        theme: Theme,
        openPage: (Int) -> Unit,
        openCustomPage: (SettingsPage) -> Unit
    ) {
        Button(
            { openPage(target_page) },
            Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = theme.vibrant_accent,
                contentColor = theme.on_accent
            )
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, color = theme.on_accent)
                ItemText(subtitle, theme.on_accent)
            }
        }
    }
}

class SettingsItemLargeToggle(
    val state: BasicSettingsValueState<Boolean>,
    val enabled_text: String? = null,
    val disabled_text: String? = null,
    val enable_button: String,
    val disable_button: String,
    val enabled_content: (@Composable (Modifier) -> Unit)? = null,
    val disabled_content: (@Composable (Modifier) -> Unit)? = null,
    val warningDialog: (@Composable (dismiss: () -> Unit, openPage: (Int) -> Unit) -> Unit)? = null,
    val infoDialog: (@Composable (dismiss: () -> Unit, openPage: (Int) -> Unit) -> Unit)? = null,
    val onClicked: (target: Boolean, setEnabled: (Boolean) -> Unit, setLoading: (Boolean) -> Unit, openPage: (Int) -> Unit) -> Unit =
        { target, setEnabled, _, _ -> setEnabled(target) }
): SettingsItem() {
    override fun initialiseValueStates(prefs: ProjectPreferences, default_provider: (String) -> Any) {
        state.init(prefs, default_provider)
    }
    override fun resetValues() {}

    @Composable
    override fun GetItem(
        theme: Theme,
        openPage: (Int) -> Unit,
        openCustomPage: (SettingsPage) -> Unit
    ) {
        val shape = RoundedCornerShape(20.dp)
        var loading: Boolean by remember { mutableStateOf(false) }

        var showing_dialog: (@Composable (dismiss: () -> Unit, openPage: (Int) -> Unit) -> Unit)? by remember { mutableStateOf(null) }

        showing_dialog?.invoke(
            { showing_dialog = null },
            openPage
        )

        Crossfade(state.value) { enabled ->
            CompositionLocalProvider(LocalContentColor provides if (!enabled) theme.on_background else theme.on_accent) {
                Row(
                    Modifier
                        .background(
                            if (!enabled) theme.background else theme.vibrant_accent,
                            shape
                        )
                        .border(Dp.Hairline, theme.vibrant_accent, shape)
                        .padding(horizontal = 10.dp)
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically
                ) {
                    (if (enabled) enabled_content else disabled_content)?.invoke(Modifier.weight(1f).padding(vertical = 5.dp))
                    (if (enabled) enabled_text else disabled_text)?.also { Text(it, Modifier.fillMaxWidth().weight(1f)) }

                    Button(
                        {
                            if (!enabled && warningDialog != null) {
                                showing_dialog = warningDialog
                            }
                            else {
                                onClicked(
                                    !enabled,
                                    { state.value = it },
                                    { loading = it },
                                    openPage
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (enabled) theme.background else theme.vibrant_accent,
                            contentColor = if (enabled) theme.on_background else theme.on_accent
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            this@Row.AnimatedVisibility(loading, enter = fadeIn(), exit = fadeOut()) {
                                SubtleLoadingIndicator()
                            }

                            val text_alpha = animateFloatAsState(if (loading) 0f else 1f)
                            Text(
                                if (enabled) disable_button else enable_button,
                                Modifier.graphicsLayer { alpha = text_alpha.value }
                            )
                        }
                    }

                    if (infoDialog != null) {
                        ShapedIconButton(
                            { showing_dialog = if (showing_dialog == null) infoDialog else null },
                            shape = CircleShape,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (enabled) theme.background else theme.vibrant_accent,
                                contentColor = if (enabled) theme.on_background else theme.on_accent
                            )
                        ) {
                            Icon(Icons.Default.Info, null)
                        }
                    }
                }
            }
        }
    }
}


class SettingsItemAccessibilityService(
    val enabled_text: String,
    val disabled_text: String,
    val enable_button: String,
    val disable_button: String,
    val service_bridge: AccessibilityServiceBridge
): SettingsItem() {
    interface AccessibilityServiceBridge {
        fun addEnabledListener(listener: (Boolean) -> Unit, context: PlatformContext)
        fun removeEnabledListener(listener: (Boolean) -> Unit, context: PlatformContext)
        fun isEnabled(context: PlatformContext): Boolean
        fun setEnabled(enabled: Boolean)
    }

    override fun initialiseValueStates(prefs: ProjectPreferences, default_provider: (String) -> Any) {}
    override fun resetValues() {}

    @Composable
    override fun GetItem(
        theme: Theme,
        openPage: (Int) -> Unit,
        openCustomPage: (SettingsPage) -> Unit
    ) {
        var service_enabled: Boolean by remember { mutableStateOf(service_bridge.isEnabled(context)) }
        val listener: (Boolean) -> Unit = { service_enabled = it }
        DisposableEffect(Unit) {
            service_bridge.addEnabledListener(listener, context)
            onDispose {
                service_bridge.removeEnabledListener(listener, context)
            }
        }

        Crossfade(service_enabled) { enabled ->
            CompositionLocalProvider(LocalContentColor provides if (enabled) theme.on_background else theme.on_accent) {
                Row(
                    Modifier
                        .background(
                            if (enabled) theme.background else theme.vibrant_accent,
                            SETTINGS_ITEM_ROUNDED_SHAPE
                        )
                        .border(Dp.Hairline, theme.vibrant_accent, SETTINGS_ITEM_ROUNDED_SHAPE)
                        .padding(start = 20.dp, end = 20.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (enabled) enabled_text else disabled_text)
                    Button({ service_bridge.setEnabled(!enabled) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (enabled) theme.vibrant_accent else theme.background,
                            contentColor = if (enabled) theme.on_accent else theme.on_background
                        )
                    ) {
                        Text(if (enabled) disable_button else enable_button)
                    }
                }
            }
        }
    }
}
