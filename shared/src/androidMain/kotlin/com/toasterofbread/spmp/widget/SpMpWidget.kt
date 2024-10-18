package com.toasterofbread.spmp.widget

import LocalPlayerState
import ProgramArguments
import SpMp
import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import androidx.annotation.FontRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.AppWidgetId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.wrapContentSize
import com.toasterofbread.spmp.model.settings.category.FontMode
import com.toasterofbread.spmp.model.settings.category.observeCurrentTheme
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.observeUiLanguage
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.shared.R
import com.toasterofbread.spmp.ui.layout.nowplaying.ThemeMode
import com.toasterofbread.spmp.widget.action.TypeWidgetClickAction
import com.toasterofbread.spmp.widget.action.WidgetClickAction
import com.toasterofbread.spmp.widget.action.WidgetClickAction.CommonWidgetClickAction.NONE
import com.toasterofbread.spmp.widget.action.WidgetClickAction.CommonWidgetClickAction.OPEN_SPMP
import com.toasterofbread.spmp.widget.action.WidgetClickAction.CommonWidgetClickAction.OPEN_WIDGET_CONFIG
import com.toasterofbread.spmp.widget.action.WidgetClickAction.CommonWidgetClickAction.TOGGLE_VISIBILITY
import com.toasterofbread.spmp.widget.component.GlanceText
import com.toasterofbread.spmp.widget.configuration.BaseWidgetConfiguration
import com.toasterofbread.spmp.widget.configuration.BaseWidgetConfiguration.ContentColour.DARK
import com.toasterofbread.spmp.widget.configuration.BaseWidgetConfiguration.ContentColour.LIGHT
import com.toasterofbread.spmp.widget.configuration.BaseWidgetConfiguration.ContentColour.THEME
import com.toasterofbread.spmp.widget.configuration.SpMpWidgetConfiguration
import com.toasterofbread.spmp.widget.configuration.TypeWidgetConfiguration
import dev.toastbits.composekit.platform.composable.theme.LocalApplicationTheme
import dev.toastbits.composekit.settings.ui.NamedTheme
import dev.toastbits.composekit.utils.common.thenIf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

@Suppress("UNCHECKED_CAST")
abstract class SpMpWidget<A: TypeWidgetClickAction, T: TypeWidgetConfiguration<A>>: GlanceAppWidget() {
    companion object {
        private val active_widgets: MutableMap<Int, SpMpWidget<*, *>> = mutableMapOf()

        fun runActionOnWidget(action: WidgetClickAction<TypeWidgetClickAction>, widget_glance_id: GlanceId) {
            val widget: SpMpWidget<*, *> = SpMpWidget.active_widgets[widget_glance_id.getDatabaseId()]!!
            widget.runAction(action, widget_glance_id)
        }
    }

    protected lateinit var context: AppContext

    protected val type_configuration: T
        get() = configuration.type_configuration as T

    private val base_configuration: BaseWidgetConfiguration
        get() = configuration.base_configuration

    private val widget_type: SpMpWidgetType = SpMpWidgetType.entries.first { it.widgetClass == this::class }
    private var configuration: SpMpWidgetConfiguration<out TypeWidgetClickAction> by mutableStateOf(SpMpWidgetConfiguration(widget_type.defaultConfiguration, BaseWidgetConfiguration()))
    private var visible: Boolean by mutableStateOf(true)
    private val coroutine_scope: CoroutineScope = CoroutineScope(Job())
    private var widget_id: Int? by mutableStateOf(null)

    final override suspend fun provideGlance(context: Context, id: GlanceId) {
        this.context = AppContext.create(context, coroutine_scope)

        val np_theme_mode: ThemeMode = this.context.settings.theme.NOWPLAYING_THEME_MODE.get()
        val swipe_sensitivity: Float = this.context.settings.player.EXPAND_SWIPE_SENSITIVITY.get()

        provideContent {
            // Force recomposition
            widget_type.getUpdateValue()

            val composable_coroutine_scope: CoroutineScope = rememberCoroutineScope()

            val state: PlayerState =
                SpMp._player_state ?:
                    remember {
                        PlayerState(this.context, ProgramArguments(), composable_coroutine_scope, np_theme_mode, swipe_sensitivity)
                    }

            widget_id = id.getDatabaseId()
            ObserveConfiguration(widget_id!!)
            active_widgets[widget_id!!] = this

            CompositionLocalProvider(
                LocalPlayerState provides state,
                LocalConfiguration provides context.resources.configuration,
                LocalContext provides context,
                dev.toastbits.composekit.platform.LocalContext provides this.context,
                LocalDensity provides Density(context.resources.displayMetrics.density)
            ) {
                val theme: NamedTheme by observeCurrentTheme(this.context, base_configuration.theme_index)

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
                            .clickable(WidgetActionCallback(configuration)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (base_configuration.show_debug_information) {
                            shouldHide()
                            hasContent()
                        }
                        else {
                            if (shouldHide() || !visible) {
                                return@Box
                            }

                            if (!hasContent() && base_configuration.hide_when_no_content) {
                                return@Box
                            }
                        }

                        GlanceBorderBox(
                            base_configuration.border_radius_dp.dp,
                            theme.theme.accent,
                            GlanceModifier
                                .fillMaxSize()
                                .systemCornerRadius()
                        ) {
                            // Dark: android.R.color.system_accent2_800
                            // Light: android.R.color.system_accent2_50

                            Column(
                                GlanceModifier
                                    .fillMaxSize()
                                    .background(theme.theme.card.copy(alpha = base_configuration.background_opacity))
//                                    .background(android.R.color.system_accent2_50)
                                    .systemCornerRadius()
                                    .padding(10.dp)
                            ) {
                                if (base_configuration.show_debug_information) {
                                    DebugInfoItems(GlanceModifier)
                                }

                                Box(
                                    GlanceModifier.fillMaxSize().defaultWeight(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Content(GlanceModifier.wrapContentSize())
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun GlanceModifier.systemCornerRadius(): GlanceModifier {
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            val systemCornerRadiusDefined: Boolean =
                androidx.glance.LocalContext.current.resources.getResourceName(android.R.dimen.system_app_widget_background_radius) != null
            if (systemCornerRadiusDefined) {
                return cornerRadius(android.R.dimen.system_app_widget_background_radius)
            }
        }
        return this
    }

    @Composable
    fun GlanceBorderBox(
        border_radius: Dp,
        border_colour: Color,
        modifier: GlanceModifier = GlanceModifier,
        content: @Composable () -> Unit
    ) {
        Box(
            modifier
                .thenIf(border_radius > 0.dp) {
                    background(border_colour)
                        .padding(border_radius)
                }
        ) {
            content()
        }
    }

    private fun onClick(id: GlanceId) {
        runAction(type_configuration.click_action, id)
    }

    fun runAction(action: WidgetClickAction<A>, id: GlanceId) {
        when (action) {
            is WidgetClickAction.CommonWidgetClickAction -> executeCommonAction(action, id)
            is WidgetClickAction.Type -> executeTypeAction(action.actionEnum)
        }
    }

    private fun executeCommonAction(action: WidgetClickAction.CommonWidgetClickAction, id: GlanceId) {
        when (action) {
            NONE -> {}
            OPEN_SPMP -> {
                with (context.ctx) {
                    startActivity(packageManager.getLaunchIntentForPackage(packageName))
                }
            }
            OPEN_WIDGET_CONFIG -> {
                val intent: Intent = Intent()
                intent.setComponent(ComponentName(context.ctx, "com.toasterofbread.spmp.widget.WidgetConfigurationActivity"))
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, GlanceAppWidgetManager(context.ctx).getAppWidgetId(id))
                intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
                context.ctx.startActivity(intent)
            }
            TOGGLE_VISIBILITY -> {
                visible = !visible
            }
        }
    }

    override suspend fun onDelete(context: Context, glanceId: GlanceId) {
        super.onDelete(context, glanceId)

        val app_context: AppContext = AppContext.create(context, coroutine_scope)
        app_context.database.androidWidgetQueries.remove(glanceId.getDatabaseId().toLong())
        coroutine_scope.cancel()

        val widget_id: Int = glanceId.getDatabaseId()
        if (active_widgets[widget_id] == this) {
            active_widgets.remove(widget_id)
        }
    }

    @Composable
    private fun ObserveConfiguration(widget_id: Int) {
        configuration = SpMpWidgetConfiguration.observeForWidget(this.context, widget_type, widget_id).value
    }

    protected abstract fun executeTypeAction(action: A)

    @Composable
    protected abstract fun Content(modifier: GlanceModifier)

    @Composable
    protected open fun hasContent(): Boolean = true

    @Composable
    protected open fun shouldHide(): Boolean = false

    @Composable
    protected open fun DebugInfoItems(item_modifier: GlanceModifier) {
        WidgetText("ID: $widget_id", item_modifier)
        WidgetText("Update: ${widget_type.getUpdateValue()}", item_modifier)
    }

    @Composable
    fun WidgetText(
        text: String,
        modifier: GlanceModifier = GlanceModifier,
        font_size: TextUnit = 15.sp,
        alpha: Float = 1f
    ) {
        val ui_language: String by context.observeUiLanguage()
        val app_font_mode: FontMode by context.settings.system.FONT.observe()
        val font: Int? = (base_configuration.font ?: app_font_mode).getAndroidFontResource(ui_language)

        GlanceText(
            text = text,
            font = font,
            modifier = modifier,
            font_size = font_size * base_configuration.font_size,
            alpha = alpha
        )
    }

    @FontRes
    private fun FontMode.getAndroidFontResource(ui_language: String): Int? =
        when (this) {
            FontMode.DEFAULT -> FontMode.getDefaultFont(ui_language).getAndroidFontResource(ui_language)
            FontMode.SYSTEM -> null
            FontMode.HC_MARU_GOTHIC -> R.font.hc_maru_gothic
        }
}

@SuppressLint("RestrictedApi")
fun GlanceId.getDatabaseId(): Int =
    when (this) {
        is AppWidgetId -> appWidgetId
        else -> throw NotImplementedError(this::class.toString())
    }

// Must not be private
internal class WidgetActionCallback: ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val serialisedAction: String = parameters[keyAction] ?: return
        val configuration: SpMpWidgetConfiguration<TypeWidgetClickAction> = SpMpWidgetConfiguration.decodeFromString(serialisedAction)
        SpMpWidget.runActionOnWidget(configuration.type_configuration.click_action, glanceId)
    }

    companion object {
        val keyAction: ActionParameters.Key<String> = ActionParameters.Key("action")

        operator fun <A: TypeWidgetClickAction> invoke(configuration: SpMpWidgetConfiguration<A>): Action =
            actionRunCallback<WidgetActionCallback>(
                actionParametersOf(keyAction to SpMpWidgetConfiguration.encodeToString(configuration))
            )
    }
}
