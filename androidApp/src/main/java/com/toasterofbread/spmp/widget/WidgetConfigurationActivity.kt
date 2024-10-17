package com.toasterofbread.spmp.widget

import LocalPlayerState
import ProgramArguments
import SpMp
import Theme
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.observeUiLanguage
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.nowplaying.ThemeMode
import com.toasterofbread.spmp.widget.configuration.SpMpWidgetConfiguration
import com.toasterofbread.spmp.widget.configuration.TypeWidgetConfiguration
import com.toasterofbread.spmp.widget.configuration.ui.screen.WidgetConfigurationScreen
import dev.toastbits.composekit.navigation.Screen
import dev.toastbits.composekit.navigation.compositionlocal.LocalNavigator
import dev.toastbits.composekit.navigation.navigator.CurrentScreen
import dev.toastbits.composekit.navigation.navigator.ExtendableNavigator
import dev.toastbits.composekit.navigation.navigator.Navigator
import dev.toastbits.composekit.platform.ApplicationContext
import dev.toastbits.composekit.platform.LocalContext
import dev.toastbits.composekit.utils.common.plus
import dev.toastbits.composekit.utils.modifier.background
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class WidgetConfigurationActivity: ComponentActivity() {
    private var app_widget_id: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private val coroutine_scope: CoroutineScope = CoroutineScope(Job())
    private var dummy_player_state: PlayerState? = null

    private suspend fun finishConfiguration(
        configuration: SpMpWidgetConfiguration,
        context: AppContext,
        widget_type: SpMpWidgetType
    ) {
        val glance_id: GlanceId = GlanceAppWidgetManager(this).getGlanceIdBy(app_widget_id)

        context.database.androidWidgetQueries.insertOrReplace(glance_id.getId().toLong(), Json.encodeToString(configuration))

        val widget: GlanceAppWidget = widget_type.widgetClass.java.getDeclaredConstructor().newInstance()
        widget.update(this, glance_id)

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

        val configuration_screen: Screen =
            WidgetConfigurationScreen(
                context,
                app_widget_id,
                widget_type,
                onCancel = { setResultAndFinish(false) },
                onDone = {
                    coroutine_scope.launch {
                        finishConfiguration(it, context, widget_type)
                    }
                }
            )
        val navigator: Navigator = ExtendableNavigator(configuration_screen)

        setContent {
            val composable_coroutine_scope: CoroutineScope = rememberCoroutineScope()
            val np_theme_mode: ThemeMode by context.settings.theme.NOWPLAYING_THEME_MODE.observe()
            val swipe_sensitivity: Float by context.settings.player.EXPAND_SWIPE_SENSITIVITY.observe()

            CompositionLocalProvider(
                LocalContext provides context,
                LocalNavigator provides navigator,
                LocalPlayerState providesComputed {
                    SpMp._player_state?.also { return@providesComputed it }

                    if (dummy_player_state == null) {
                        dummy_player_state = PlayerState(context, ProgramArguments(), composable_coroutine_scope, np_theme_mode, swipe_sensitivity)
                    }

                    return@providesComputed dummy_player_state!!
                }
            ) {
                if (!context.theme.Update()) {
                    return@CompositionLocalProvider
                }

                val ui_language: String by context.observeUiLanguage()

                SpMp.Theme(context, ui_language) {
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

    override fun onDestroy() {
        super.onDestroy()
        coroutine_scope.cancel()
    }
}
