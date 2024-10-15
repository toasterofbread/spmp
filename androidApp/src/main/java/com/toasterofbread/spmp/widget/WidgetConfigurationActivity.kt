package com.toasterofbread.spmp.widget

import SpMp
import Theme
import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.getUiLanguage
import com.toasterofbread.spmp.resources.initResources
import dev.toastbits.composekit.platform.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

class WidgetConfigurationActivity: ComponentActivity() {
    private var app_widget_id: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private val coroutine_scope: CoroutineScope = CoroutineScope(Job())

    private fun finishConfiguration(proceed: Boolean) {
        val result_intent: Intent = Intent()
        result_intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, app_widget_id)
        setResult(
            if (proceed) RESULT_OK
            else RESULT_CANCELED,
            result_intent
        )
        finish()
    }

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent.extras?.also {
            app_widget_id = it.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        }

        if (app_widget_id == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finishConfiguration(false)
            return
        }

        val info: AppWidgetProviderInfo = AppWidgetManager.getInstance(this).getAppWidgetInfo(app_widget_id)
        val widget_type: SpMpWidgetType = getSpMpWidgetTypeForActivityInfo(info.activityInfo)

        val context: AppContext = AppContext(this, coroutine_scope, ApplicationContext(this))
        initResources(context.getUiLanguage(), context)

        setContent {
            context.theme.Update()

            SpMp.Theme(context) {
                Scaffold { inner_padding ->
                    Text(widget_type.toString(), Modifier.padding(inner_padding))
//                    Content(
//                        context,
//                        Modifier
//                            .padding(inner_padding)
//                            .fillMaxSize()
//                            .background { context.theme.background }
//                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutine_scope.cancel()
    }

    @Composable
    private fun Content(context: AppContext, modifier: Modifier = Modifier) {
        Column(modifier) {
            Text("Configuration screen $app_widget_id ${AppWidgetManager.INVALID_APPWIDGET_ID}")

            Button({
                finishConfiguration(true)
            }) {
                Text("Finish")
            }

            Button({
                finishConfiguration(false)
            }) {
                Text("Cancel")
            }
        }
    }
}
