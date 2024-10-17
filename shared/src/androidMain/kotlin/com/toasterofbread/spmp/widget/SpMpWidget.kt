package com.toasterofbread.spmp.widget

import LocalPlayerState
import ProgramArguments
import SpMp
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import androidx.annotation.FontRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import androidx.core.util.TypedValueCompat.spToPx
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.AppWidgetId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
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
import com.toasterofbread.spmp.widget.configuration.BaseWidgetConfiguration
import com.toasterofbread.spmp.widget.configuration.BaseWidgetConfiguration.ContentColour.DARK
import com.toasterofbread.spmp.widget.configuration.BaseWidgetConfiguration.ContentColour.LIGHT
import com.toasterofbread.spmp.widget.configuration.BaseWidgetConfiguration.ContentColour.THEME
import com.toasterofbread.spmp.widget.configuration.SpMpWidgetConfiguration
import com.toasterofbread.spmp.widget.configuration.TypeWidgetConfiguration
import dev.toastbits.composekit.platform.composable.theme.LocalApplicationTheme
import dev.toastbits.composekit.settings.ui.NamedTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

// https://proandroiddev.com/jetpack-glance-no-way-to-custom-fonts-e761b789567d
fun Context.textAsBitmap(
    text: String,
    fontSize: TextUnit,
    color: Color = Color.Black,
    letterSpacing: Float = 0.1f,
    @FontRes
    font: Int?
): Bitmap? {
    val paint: TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    paint.textSize = spToPx(fontSize.value, this.resources.displayMetrics)
    paint.color = color.toArgb()
    paint.letterSpacing = letterSpacing
    paint.typeface =
        font?.let { ResourcesCompat.getFont(this, font) }
            ?: Typeface.DEFAULT

    val baseline: Float = -paint.ascent()
    val width: Int = (paint.measureText(text)).toInt()
    val height: Int = (baseline + paint.descent()).toInt()

    if (width <= 0 || height <= 0) {
        return null
    }

    val image: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas: Canvas = Canvas(image)
    canvas.drawText(text, 0f, baseline, paint)
    return image
}

@Suppress("UNCHECKED_CAST")
abstract class SpMpWidget<A: TypeWidgetClickAction, T: TypeWidgetConfiguration<A>>: GlanceAppWidget() {
    protected lateinit var context: AppContext
    protected var base_configuration: BaseWidgetConfiguration by mutableStateOf(BaseWidgetConfiguration())
    protected var type_configuration: T by mutableStateOf(widget_type.defaultConfiguration as T)

    @FontRes
    private fun FontMode.getAndroidFontResource(ui_language: String): Int? =
        when (this) {
            FontMode.DEFAULT -> FontMode.getDefaultFont(ui_language).getAndroidFontResource(ui_language)
            FontMode.SYSTEM -> null
            FontMode.HC_MARU_GOTHIC -> R.font.hc_maru_gothic
        }

    @Composable
    protected fun Text(
        text: String,
        modifier: GlanceModifier = GlanceModifier,
        font_size: TextUnit = 15.sp
    ) {
        val ui_language: String by context.observeUiLanguage()
        val app_font_mode: FontMode by context.settings.system.FONT.observe()
        val font: Int? = (base_configuration.font ?: app_font_mode).getAndroidFontResource(ui_language)
        val final_font_size = font_size * base_configuration.font_size
        val colour: Color = LocalApplicationTheme.current.on_background

        val image: Bitmap =
            remember(text, final_font_size, colour, font) {
                context.ctx.textAsBitmap(
                    text = text,
                    fontSize = final_font_size,
                    color = colour,
                    font = font,
                    letterSpacing = 0.1.sp.value
                )
            } ?: return

        Image(
            modifier = modifier,
            provider = ImageProvider(image),
            contentDescription = text
        )
    }

    private var visible: Boolean by mutableStateOf(true)
    private val coroutine_scope: CoroutineScope = CoroutineScope(Job())
    private val widget_type: SpMpWidgetType
        get() = SpMpWidgetType.entries.first { it.widgetClass == this::class }

    final override suspend fun provideGlance(context: Context, id: GlanceId) {
        this.context = AppContext.create(context, coroutine_scope)

        val np_theme_mode: ThemeMode = this.context.settings.theme.NOWPLAYING_THEME_MODE.get()
        val swipe_sensitivity: Float = this.context.settings.player.EXPAND_SWIPE_SENSITIVITY.get()

        provideContent {
            val composable_coroutine_scope: CoroutineScope = rememberCoroutineScope()

            val state: PlayerState =
                SpMp._player_state ?:
                    remember {
                        PlayerState(this.context, ProgramArguments(), composable_coroutine_scope, np_theme_mode, swipe_sensitivity)
                    }

            ObserveConfiguration(widget_id = id.getDatabaseId())

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
                            GlanceModifier.fillMaxSize().clickable { onClick(id) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (shouldHide() || !visible) {
                                return@Box
                            }

                            if (!hasContent() && base_configuration.hide_when_no_content) {
                                return@Box
                            }

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
    }

    private fun onClick(id: GlanceId) {
        val action: WidgetClickAction<A> = type_configuration.click_action
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
    }

    @Composable
    private fun ObserveConfiguration(widget_id: Int) {
        val saved_configuration: SpMpWidgetConfiguration<TypeWidgetClickAction> by SpMpWidgetConfiguration.observeForWidget(this.context, widget_type, widget_id)

        base_configuration = saved_configuration.base_configuration
        type_configuration = saved_configuration.type_configuration as T
    }

    protected abstract fun executeTypeAction(action: A)

    @Composable
    protected abstract fun Content(modifier: GlanceModifier)

    @Composable
    protected open fun hasContent(): Boolean = true

    @Composable
    protected open fun shouldHide(): Boolean = false
}

@SuppressLint("RestrictedApi")
fun GlanceId.getDatabaseId(): Int =
    when (this) {
        is AppWidgetId -> appWidgetId
        else -> throw NotImplementedError(this::class.toString())
    }
