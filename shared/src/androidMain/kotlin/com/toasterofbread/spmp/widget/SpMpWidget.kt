package com.toasterofbread.spmp.widget

import LocalPlayerState
import ProgramArguments
import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.AppWidgetId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.wrapContentSize
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.visibility
import com.toasterofbread.spmp.model.settings.category.observeCurrentTheme
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.nowplaying.ThemeMode
import com.toasterofbread.spmp.widget.configuration.BaseWidgetConfiguration
import com.toasterofbread.spmp.widget.configuration.BaseWidgetConfiguration.ContentColour.DARK
import com.toasterofbread.spmp.widget.configuration.BaseWidgetConfiguration.ContentColour.LIGHT
import com.toasterofbread.spmp.widget.configuration.BaseWidgetConfiguration.ContentColour.THEME
import com.toasterofbread.spmp.widget.configuration.SpMpWidgetConfiguration
import com.toasterofbread.spmp.widget.configuration.TypeWidgetConfiguration
import dev.toastbits.composekit.platform.composable.theme.LocalApplicationTheme
import dev.toastbits.composekit.settings.ui.NamedTheme
import dev.toastbits.composekit.utils.common.toFloat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
abstract class SpMpWidget<T: TypeWidgetConfiguration>(
    private val typeWidgetConfigurationClass: KClass<T>
): GlanceAppWidget() {
    protected lateinit var context: AppContext
    protected var base_configuration: BaseWidgetConfiguration by mutableStateOf(BaseWidgetConfiguration())
    protected var type_configuration: T by mutableStateOf(widget_type.defaultConfiguration as T)

    protected val text_style: TextStyle
        @Composable
        get() = TextStyle(color = ColorProvider(LocalApplicationTheme.current.on_background))

    private val coroutine_scope: CoroutineScope = CoroutineScope(Job())
    private val widget_type: SpMpWidgetType
        get() = SpMpWidgetType.entries.first { it.widgetClass == this::class }

    final override suspend fun provideGlance(context: Context, id: GlanceId) {
        this.context = AppContext.create(context, coroutine_scope)

        val np_theme_mode: ThemeMode = this.context.settings.theme.NOWPLAYING_THEME_MODE.get()
        val swipe_sensitivity: Float = this.context.settings.player.EXPAND_SWIPE_SENSITIVITY.get()

        provideContent {
            val composable_coroutine_scope = rememberCoroutineScope()

            val state: PlayerState =
                SpMp._player_state ?:
                    remember {
                        PlayerState(this.context, ProgramArguments(), composable_coroutine_scope, np_theme_mode, swipe_sensitivity)
                    }

            ObserveConfiguration(widget_id = id.getId())

            CompositionLocalProvider(
                LocalPlayerState provides state,
                LocalConfiguration provides context.resources.configuration,
                LocalContext provides context,
                dev.toastbits.composekit.platform.LocalContext provides this.context,
                LocalDensity provides Density(context.resources.displayMetrics.density)
            ) {
                val has_content: Boolean = hasContent()
                if (!has_content && base_configuration.hide_when_no_content) {
                    return@CompositionLocalProvider
                }

                val theme: NamedTheme by observeCurrentTheme(base_configuration.theme_index)

                val on_background_colour: Color =
                    when (base_configuration.content_colour) {
                        THEME -> theme.theme.on_background
                        LIGHT -> Color.White
                        DARK -> Color.Black
                    }

                CompositionLocalProvider(
                    LocalApplicationTheme provides theme.theme.copy(on_background = on_background_colour)
                ) {
                    Box(
                        GlanceModifier
                            .fillMaxSize()
                            .background(theme.theme.background.copy(alpha = base_configuration.background_opacity)),
                        contentAlignment = Alignment.Center
                    ) {
                        Content(GlanceModifier.wrapContentSize())
                    }
                }
            }
        }
    }

    override suspend fun onDelete(context: Context, glanceId: GlanceId) {
        super.onDelete(context, glanceId)
        coroutine_scope.cancel()
    }

    @Composable
    private fun ObserveConfiguration(widget_id: Int) {
        val saved_configuration: SpMpWidgetConfiguration by SpMpWidgetConfiguration.observeForWidget(this.context, widget_type, widget_id)

        base_configuration = saved_configuration.base_configuration
        type_configuration =
            if (typeWidgetConfigurationClass.isInstance(saved_configuration.type_configuration)) saved_configuration.type_configuration as T
            else {
                RuntimeException(
                    "WARNING: Saved configuration for widget $widget_id is ${saved_configuration.type_configuration::class}, but expected $typeWidgetConfigurationClass"
                ).printStackTrace()

                widget_type.defaultConfiguration as T
            }
    }

    @Composable
    protected abstract fun Content(modifier: GlanceModifier)

    @Composable
    protected open fun hasContent(): Boolean = true
}

@SuppressLint("RestrictedApi")
fun GlanceId.getId(): Int =
    when (this) {
        is AppWidgetId -> appWidgetId
        else -> throw NotImplementedError(this::class.toString())
    }
