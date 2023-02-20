package com.spectre7.settings.model

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.Animatable
import androidx.compose.animation.Crossfade
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.github.krottv.compose.sliders.*
import com.spectre7.composesettings.ui.SettingsPage
import com.spectre7.utils.*
import kotlin.math.roundToInt

abstract class SettingsItem {
    lateinit var context: Context

    private var initialised = false
    fun initialise(context: Context, prefs: SharedPreferences, default_provider: (String) -> Any) {
        if (initialised) {
            return
        }
        this.context = context
        initialiseValueStates(prefs, default_provider)
        initialised = true
    }

    protected abstract fun initialiseValueStates(prefs: SharedPreferences, default_provider: (String) -> Any)
    abstract fun resetValues()

    @Composable
    abstract fun GetItem(
        theme: Theme,
        openPage: (Int) -> Unit,
        openCustomPage: (SettingsPage) -> Unit
    )
}

class SettingsGroup(var title: String?): SettingsItem() {
    override fun initialiseValueStates(prefs: SharedPreferences, default_provider: (String) -> Any) {}
    override fun resetValues() {}

    @Composable
    override fun GetItem(
        theme: Theme,
        openPage: (Int) -> Unit,
        openCustomPage: (SettingsPage) -> Unit
    ) {
        Spacer(Modifier.requiredHeight(20.dp))
        if (title != null) {
            Text(title!!.uppercase(), color = theme.getVibrantAccent(), fontSize = 20.sp, fontWeight = FontWeight.Light)
        }
    }
}

class SettingsValueState<T>(val key: String) {

    var autosave: Boolean = true

    private lateinit var prefs: SharedPreferences
    private lateinit var defaultProvider: (String) -> Any
    private var _value: T? by mutableStateOf(null)

    var value: T
        get() = _value!!
        set(new_value) {
            if (_value == null) {
                throw IllegalStateException("State has not been initialised")
            }
            _value = new_value
            if (autosave) {
                save()
            }
        }

    fun init(prefs: SharedPreferences, defaultProvider: (String) -> Any): SettingsValueState<T> {
        if (_value != null) {
            return this
        }

        this.prefs = prefs
        this.defaultProvider = defaultProvider

        val default = defaultProvider(key) as T
        _value = when (default!!::class) {
            Boolean::class -> prefs.getBoolean(key, default as Boolean)
            Float::class -> prefs.getFloat(key, default as Float)
            Int::class -> prefs.getInt(key, default as Int)
            Long::class -> prefs.getLong(key, default as Long)
            String::class -> prefs.getString(key, default as String)
            else -> throw ClassCastException()
        } as T

        return this
    }

    fun reset() {
        value = defaultProvider(key) as T
        if (!autosave) {
            save()
        }
    }

    fun save() {
        with (prefs.edit()) {
            when (value!!::class) {
                Boolean::class -> putBoolean(key, value as Boolean)
                Float::class -> putFloat(key, value as Float)
                Int::class -> putInt(key, value as Int)
                Long::class -> putLong(key, value as Long)
                String::class -> putString(key, value as String)
                else -> throw ClassCastException()
            }
            apply()
        }
    }
}

class SettingsItemToggle(
    val state: SettingsValueState<Boolean>,
    val title: String?,
    val subtitle: String?,
    val checker: ((target: Boolean, (allowChange: Boolean) -> Unit) -> Unit)? = null
): SettingsItem() {

    override fun initialiseValueStates(prefs: SharedPreferences, default_provider: (String) -> Any) {
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
                    .weight(1f)) {
                if (title != null) {
                    Text(title)
                }
                if (subtitle != null) {
                    Text(subtitle, color = theme.getOnBackground(false).setAlpha(0.75f))
                }
            }

            Switch(
                state.value,
                null,
                Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (checker == null) {
                        state.value = !state.value
                        return@clickable
                    }

                    checker.invoke(!state.value) { allow_change ->
                        if (allow_change) {
                            state.value = !state.value
                        }
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = theme.getVibrantAccent(),
                    checkedTrackColor = theme.getVibrantAccent().setAlpha(0.5f)
                )
            )
        }
    }
}

class SettingsItemSlider(
    val state: SettingsValueState<out Number>,
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

    fun setValue(value: Float) {
        value_state = value
        if (is_int) {
            (state as SettingsValueState<Int>).value = value.roundToInt()
        } else {
            (state as SettingsValueState<Float>).value = value
        }
    }

    fun getValue(): Float {
        return value_state
    }

    override fun initialiseValueStates(prefs: SharedPreferences, default_provider: (String) -> Any) {
        state.init(prefs, default_provider)
        value_state = state.value.toFloat()
        is_int = when (default_provider(state.key)) {
            is Float -> false
            is Int -> true
            else -> throw NotImplementedError(default_provider(state.key).javaClass.name)
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

            AlertDialog(
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
                title = { Text(title ?: "Edit field") },
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
                                    error = "Value is out of range ($range)"
                                    return@OutlinedTextField
                                }

                                error = null
                            }
                            catch(_: NumberFormatException) {
                                error = if (is_int) "Value is not an integer" else "Value is not a float"
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
                    Text(title)
                    IconButton({ show_edit_dialog = true }, Modifier.size(25.dp)) {
                        Icon(Icons.Filled.Edit, null)
                    }
                }
            }
            if (subtitle != null) {
                Text(subtitle, color = theme.getOnBackground(false).setAlpha(0.75f))
            }

            Spacer(Modifier.requiredHeight(10.dp))

            state.autosave = false
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (min_label != null) {
                    Text(min_label, fontSize = 12.sp)
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
                            theme.getVibrantAccent().setAlpha(0.5f),
                            theme.getVibrantAccent(),
                            colorTickProgress = theme.getVibrantAccent().getContrasted().setAlpha(0.5f)
                        )
                    },
                    thumb = { modifier, offset, interaction_source, enabled, thumb_size ->
                        val colour = theme.getVibrantAccent()
                        val scale_on_press = 1.15f
                        val animation_spec = SpringSpec<Float>(0.65f)
                        val value_text = getValueText?.invoke(getValue())

                        if (value_text != null) {
                            MeasureUnconstrainedView({ Text(value_text) }) { width, height ->

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
                                    Text(value_text)
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
                    Text(max_label, fontSize = 12.sp)
                }
            }
        }
    }
}

class SettingsItemMultipleChoice(
    val state: SettingsValueState<Int>,
    val title: String?,
    val subtitle: String?,
    val choice_amount: Int,
    val radio_style: Boolean,
    val get_choice: (Int) -> String,
): SettingsItem() {

    override fun initialiseValueStates(prefs: SharedPreferences, default_provider: (String) -> Any) {
        state.init(prefs, default_provider)
    }

    override fun resetValues() {
        state.reset()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun GetItem(
        theme: Theme,
        openPage: (Int) -> Unit,
        openCustomPage: (SettingsPage) -> Unit
    ) {
        Column {
            Column(Modifier.fillMaxWidth()) {
                if (title != null) {
                    Text(title)
                }
                if (subtitle != null) {
                    Text(subtitle, color = theme.getOnBackground(false).setAlpha(0.75f), fontSize = 15.sp)
                }

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
                                        theme.getOnBackground(false),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp)
                                    .clickable(
                                        remember { MutableInteractionSource() },
                                        null
                                    ) { state.value = i }
                            ) {
                                Text(get_choice(i), color = theme.getOnAccent())
                                RadioButton(i == state.value, onClick = { state.value = i }, colors = RadioButtonDefaults.colors(theme.getVibrantAccent()))
                            }
                        }
                    }
                }
                else {
                    Column(Modifier.padding(start = 15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (i in 0 until choice_amount) {

                            val colour = remember(i) { Animatable(if (state.value == i) theme.getVibrantAccent() else Color.Transparent) }
                            LaunchedEffect(state.value, theme.getAccent()) {
                                colour.animateTo(if (state.value == i) theme.getAccent() else Color.Transparent, TweenSpec(150))
                            }

                            Box(
                                contentAlignment = Alignment.CenterStart,
                                modifier = Modifier
                                    .border(
                                        Dp.Hairline,
                                        theme.getOnBackground(false),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .clickable(remember { MutableInteractionSource() }, null) {
                                        state.value = i
                                    }
                                    .background(colour.value, RoundedCornerShape(16.dp))
                            ) {
                                Box(Modifier.padding(horizontal = 10.dp)) {
                                    Text(get_choice(i), color = if (state.value == i) theme.getOnAccent() else theme.getOnBackground(false))
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
    val state: SettingsValueState<Int>,
    val title: String,
    val subtitle: String?,
    val item_count: Int,
    val getButtonItem: ((Int) -> String)? = null,
    val getItem: (Int) -> String,
): SettingsItem() {

    override fun initialiseValueStates(prefs: SharedPreferences, default_provider: (String) -> Any) {
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
                    .weight(1f)) {
                Text(title)
                if (subtitle != null) {
                    Text(subtitle, color = theme.getOnBackground(false).setAlpha(0.75f))
                }
            }

            var open by remember { mutableStateOf(false) }

            Button(
                { open = !open },
                Modifier.requiredHeight(40.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = theme.getAccent(),
                    contentColor = theme.getOnAccent()
                )
            ) {
                Text(getButtonItem?.invoke(state.value) ?: getItem(state.value))
                Icon(
                    Icons.Filled.ArrowDropDown,
                    null,
                    tint = theme.getOnAccent()
                )
            }

            Box(contentAlignment = Alignment.CenterEnd) {
                MaterialTheme(
                    shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(16.dp))
                ){
                    DropdownMenu(
                        open,
                        { open = false },
                        Modifier.size(200.dp, 200.dp),
                        offset = DpOffset(50.dp, 0.dp)
                    ) {
                        for (i in 0 until item_count) {
                            DropdownMenuItem(onClick = { state.value = i; open = false }, text = {
                                Text(getItem(i))
                            })
                        }
                    }
                }
            }

//            Popup(Alignment.TopEnd, onDismissRequest = { open = false }) {
//                Crossfade(open) {
//                    Row(Modifier.fillMaxWidth().offset((-20).dp), horizontalArrangement = Arrangement.End) {
//                        Box(
//                            Modifier.background(theme.getAccent(), RoundedCornerShape(16.dp)), contentAlignment = Alignment.TopEnd
//                        ) {
//                            if (it) {
//                                LazyColumn(
//                                    Modifier
//                                        .width(100.dp)
//                                        .padding(10.dp)
//                                        .pointerInput(Unit) {
//                                            detectTapGestures(onTap = {
//
//                                            })
//                                        },
//                                    verticalArrangement = Arrangement.spacedBy(10.dp)
//                                ) {
//                                    items(item_count) { i ->
//                                        val item = getItem(i)
//                                        Row(
//                                            verticalAlignment = Alignment.CenterVertically,
//                                            modifier = Modifier
//                                                .height(30.dp).fillMaxWidth()
//                                                .clickable {
//                                                    open = false
//                                                    state.value = i
//                                                }
//                                        ) {
//                                            Icon(
//                                                Icons.Filled.KeyboardArrowRight,
//                                                null,
//                                                tint = theme.getOnAccent()
//                                            )
//                                            Text(
//                                                item,
//                                                color = theme.getOnAccent(),
//                                                fontWeight = FontWeight.Medium
//                                            )
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }
        }
    }
}

class SettingsItemSubpage(
    val title: String,
    val subtitle: String?,
    val target_page: Int,
): SettingsItem() {

    override fun initialiseValueStates(prefs: SharedPreferences, default_provider: (String) -> Any) {}

    override fun resetValues() {}

    @Composable
    override fun GetItem(
        theme: Theme,
        openPage: (Int) -> Unit,
        openCustomPage: (SettingsPage) -> Unit
    ) {
        Button(modifier = Modifier.fillMaxWidth(), onClick = {
            openPage(target_page)
        }, colors = ButtonDefaults.buttonColors(theme.getAccent(), theme.getOnAccent())
        ) {
            Column(Modifier.weight(1f)) {
                Text(title)
                if (subtitle != null) {
                    Text(subtitle)
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
        fun addEnabledListener(listener: (Boolean) -> Unit, context: Context)
        fun removeEnabledListener(listener: (Boolean) -> Unit, context: Context)
        fun isEnabled(context: Context): Boolean
        fun setEnabled(enabled: Boolean)
    }

    override fun initialiseValueStates(prefs: SharedPreferences, default_provider: (String) -> Any) {}
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

        val shape = RoundedCornerShape(35)

        Crossfade(service_enabled) { enabled ->
            CompositionLocalProvider(LocalContentColor provides if (enabled) theme.getOnBackground(false) else theme.getOnAccent()) {
                Row(
                    Modifier
                        .background(
                            if (enabled) theme.getBackground(false) else theme.getAccent(),
                            shape
                        )
                        .border(Dp.Hairline, theme.getAccent(), shape)
                        .padding(start = 20.dp, end = 20.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (enabled) enabled_text else disabled_text)
                    Button({ service_bridge.setEnabled(!enabled) },
                        colors = ButtonDefaults.buttonColors(if (enabled) theme.getAccent() else theme.getBackground(false), if (enabled) theme.getOnAccent() else theme.getOnBackground(false))
                    ) {
                        Text(if (enabled) disable_button else enable_button)
                    }
                }
            }
        }
    }
}
