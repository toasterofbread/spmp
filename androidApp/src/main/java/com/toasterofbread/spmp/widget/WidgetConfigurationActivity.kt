package com.toasterofbread.spmp.widget

import SpMp
import Theme
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.widget.configuration.SpMpWidgetConfiguration
import dev.toastbits.composekit.platform.ApplicationContext
import dev.toastbits.composekit.utils.modifier.background
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class WidgetConfigurationActivity: ComponentActivity() {
    private var app_widget_id: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private val coroutine_scope: CoroutineScope = CoroutineScope(Job())

    private fun finishConfiguration(
        configuration: SpMpWidgetConfiguration,
        context: AppContext
    ) {
        context.database.androidWidgetQueries.insertOrReplace(app_widget_id.toLong(), Json.encodeToString(configuration))
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

        val widget_info: AppWidgetProviderInfo = AppWidgetManager.getInstance(this).getAppWidgetInfo(app_widget_id)

        val context: AppContext = runBlocking {
            AppContext.create(this@WidgetConfigurationActivity, coroutine_scope, ApplicationContext(this@WidgetConfigurationActivity))
        }

        setContent {
            context.theme.Update()

            SpMp.Theme(context) {
                Scaffold { inner_padding ->
                    Content(
                        context,
                        widget_info,
                        Modifier
                            .padding(inner_padding)
                            .fillMaxSize()
                            .background { context.theme.background }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutine_scope.cancel()
    }

    private fun getWidgetConfiguration(context: AppContext, type: SpMpWidgetType): SpMpWidgetConfiguration =
        context.database.androidWidgetQueries.configurationById(app_widget_id.toLong())
            .executeAsOneOrNull()
            ?.let { Json.decodeFromString(it) }
            ?: type.defaultConfiguration

    @Composable
    private fun Content(context: AppContext, widget_info: AppWidgetProviderInfo, modifier: Modifier = Modifier) {
        val widget_type: SpMpWidgetType = remember(widget_info) {
            getSpMpWidgetTypeForActivityInfo(widget_info.provider)
        }

        var configuration: SpMpWidgetConfiguration by remember { mutableStateOf(getWidgetConfiguration(context, widget_type)) }

        Column(
            modifier,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Configuration screen $app_widget_id $widget_type")

            configuration.ConfigurationItems(item_modifier = Modifier) {
                configuration = it
            }

            Button({
                finishConfiguration(configuration, context)
            }) {
                Text("Finish")
            }

            Button({
                setResultAndFinish(false)
            }) {
                Text("Cancel")
            }
        }
    }
}
