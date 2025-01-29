package com.toasterofbread.spmp.widget

import LocalPlayerState
import ProgramArguments
import SpMp
import Theme
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.nowplaying.ThemeMode
import com.toasterofbread.spmp.widget.action.TypeWidgetClickAction
import com.toasterofbread.spmp.widget.configuration.SpMpWidgetConfiguration
import com.toasterofbread.spmp.widget.configuration.type.TypeWidgetConfig
import com.toasterofbread.spmp.widget.configuration.ui.screen.WidgetConfigurationScreen
import dev.toastbits.composekit.components.utils.modifier.background
import dev.toastbits.composekit.context.ApplicationContext
import dev.toastbits.composekit.navigation.compositionlocal.LocalNavigator
import dev.toastbits.composekit.navigation.navigator.BaseNavigator
import dev.toastbits.composekit.navigation.navigator.CurrentScreen
import dev.toastbits.composekit.navigation.navigator.Navigator
import dev.toastbits.composekit.navigation.screen.Screen
import dev.toastbits.composekit.theme.core.model.NamedTheme
import dev.toastbits.composekit.theme.core.model.SerialisableTheme
import dev.toastbits.composekit.theme.core.model.ThemeReference
import dev.toastbits.composekit.theme.core.provider.ContextThemeProvider
import dev.toastbits.composekit.theme.core.provider.ThemeProvider
import dev.toastbits.composekit.theme.core.ui.LocalThemeProvider
import dev.toastbits.composekit.util.composable.plus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.widget_application_theme_label

class WidgetConfigurationActivity: ComponentActivity() {
    private var app_widget_id: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private val coroutine_scope: CoroutineScope = CoroutineScope(Job())
    private var dummy_player_state: PlayerState? = null

    private suspend fun finishConfiguration(
        configuration: SpMpWidgetConfiguration<out TypeWidgetClickAction>,
        context: AppContext,
        widget_type: SpMpWidgetType
    ) {
        val glance_id: GlanceId = GlanceAppWidgetManager(this).getGlanceIdBy(app_widget_id)

        val serialised: String = SpMpWidgetConfiguration.json.encodeToString(configuration)
        context.database.androidWidgetQueries.insertOrReplace(glance_id.getDatabaseId().toLong(), serialised)

        widget_type.update(this, glance_id)

        setResultAndFinish(true)
    }

    private fun setResultAndFinish(proceed: Boolean) {
        val result_intent: Intent = Intent()
        result_intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, app_widget_id)
        setResult(
            if (proceed) RESULT_OK
            else RESULT_CANCELED,
            result_intent
        )
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent.extras?.also {
            app_widget_id = it.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        }

        if (app_widget_id == AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResultAndFinish(false)
            return
        }

        val context: AppContext = runBlocking {
            AppContext.create(this@WidgetConfigurationActivity, coroutine_scope, ApplicationContext(this@WidgetConfigurationActivity))
        }

        val widget_info: AppWidgetProviderInfo = AppWidgetManager.getInstance(context.ctx).getAppWidgetInfo(app_widget_id)
        val widget_type: SpMpWidgetType = getSpMpWidgetTypeForActivityInfo(widget_info.provider)

        val configuration_screen: Screen = runBlocking {
            createWidgetConfigurationScreen(
                initial_configuration = SpMpWidgetConfiguration.getForWidget(context, widget_type, app_widget_id),
                context = context,
                widget_type = widget_type
            )
        }
        val navigator: Navigator = BaseNavigator(configuration_screen)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        setContent {
            val composable_coroutine_scope: CoroutineScope = rememberCoroutineScope()
            val np_theme_mode: ThemeMode by context.settings.Theme.NOWPLAYING_THEME_MODE.observe()
            val swipe_sensitivity: Float by context.settings.Player.EXPAND_SWIPE_SENSITIVITY.observe()

            CompositionLocalProvider(
                LocalNavigator provides navigator,
                LocalPlayerState providesComputed {
                    SpMp._player_state?.also { return@providesComputed it }

                    if (dummy_player_state == null) {
                        dummy_player_state = PlayerState(context, ProgramArguments(), composable_coroutine_scope, np_theme_mode, swipe_sensitivity)
                    }

                    return@providesComputed dummy_player_state!!
                }
            ) {
                SpMp.Theme(context) {
                    val themeProvider: ThemeProvider = LocalThemeProvider.current
                    val applicationTheme: ThemeReference by context.settings.Theme.CURRENT_THEME.observe()
                    val applicationThemeName: String = stringResource(Res.string.widget_application_theme_label)

                    CompositionLocalProvider(
                        LocalThemeProvider provides object : ContextThemeProvider(context) {
                            override fun getCustomTheme(index: Int): SerialisableTheme? =
                                if (index == -1)
                                    NamedTheme(
                                        NamedTheme.Type.CUSTOM,
                                        applicationThemeName,
                                        applicationTheme.getTheme(themeProvider)
                                    )
                                else themeProvider.getCustomTheme(index)

                            override fun getCustomThemes(): List<NamedTheme> =
                                themeProvider.getCustomThemes()
                        }
                    ) {
                        Scaffold { inner_padding ->
                            navigator.CurrentScreen(
                                Modifier
                                    .fillMaxSize()
                                    .background { context.theme.background },
                                inner_padding + PaddingValues(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    private fun <A: TypeWidgetClickAction> createWidgetConfigurationScreen(
        initial_configuration: SpMpWidgetConfiguration<A>,
        context: AppContext,
        widget_type: SpMpWidgetType
    ): WidgetConfigurationScreen<A> =
        WidgetConfigurationScreen(
            initial_configuration.base_configuration,
            initial_configuration.base_configuration_defaults_mask,
            initial_configuration.type_configuration,
            initial_configuration.type_configuration_defaults_mask,
            context,
            widget_type,
            app_widget_id,
            onCancel = { setResultAndFinish(false) },
            onDone = { base, base_defaults_mask, type, type_defaults_mask ->
                coroutine_scope.launch {
                    finishConfiguration(
                        SpMpWidgetConfiguration(
                            base!!,
                            base_defaults_mask!!,
                            type!!,
                            type_defaults_mask!!
                        ), context, widget_type
                    )
                }
            },
            onSetDefaultBaseConfig = { new_base_configuration ->
                coroutine_scope.launch {
                    context.settings.Widget.DEFAULT_BASE_WIDGET_CONFIGURATION.set(new_base_configuration)
                }
            },
            onSetDefaultTypeConfig = { new_type_configuration ->
                coroutine_scope.launch {
                    val types: Map<SpMpWidgetType, TypeWidgetConfig<out TypeWidgetClickAction>> =
                        context.settings.Widget.DEFAULT_TYPE_WIDGET_CONFIGURATIONS.get()
                    context.settings.Widget.DEFAULT_TYPE_WIDGET_CONFIGURATIONS.set(
                        types.toMutableMap().apply {
                            set(widget_type, new_type_configuration)
                        }
                    )
                }
            }
        )

    override fun onDestroy() {
        super.onDestroy()
        coroutine_scope.cancel()
    }
}
