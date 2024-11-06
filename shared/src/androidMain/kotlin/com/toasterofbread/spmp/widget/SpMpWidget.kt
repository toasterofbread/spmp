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
import android.graphics.Bitmap
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.AppWidgetId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.wrapContentSize
import androidx.glance.unit.ColorProvider
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.updateLiked
import com.toasterofbread.spmp.model.settings.category.AccentColourSource
import com.toasterofbread.spmp.model.settings.category.FontMode
import com.toasterofbread.spmp.model.settings.category.observeCurrentTheme
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.observeUiLanguage
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.shared.R
import com.toasterofbread.spmp.ui.component.Thumbnail
import com.toasterofbread.spmp.ui.layout.nowplaying.ThemeMode
import com.toasterofbread.spmp.util.getToggleTarget
import com.toasterofbread.spmp.widget.action.TypeWidgetClickAction
import com.toasterofbread.spmp.widget.action.WidgetClickAction
import com.toasterofbread.spmp.widget.action.WidgetClickAction.CommonWidgetClickAction.NONE
import com.toasterofbread.spmp.widget.action.WidgetClickAction.CommonWidgetClickAction.OPEN_SPMP
import com.toasterofbread.spmp.widget.action.WidgetClickAction.CommonWidgetClickAction.OPEN_WIDGET_CONFIG
import com.toasterofbread.spmp.widget.action.WidgetClickAction.CommonWidgetClickAction.PLAY_PAUSE
import com.toasterofbread.spmp.widget.action.WidgetClickAction.CommonWidgetClickAction.SEEK_NEXT
import com.toasterofbread.spmp.widget.action.WidgetClickAction.CommonWidgetClickAction.SEEK_PREVIOUS
import com.toasterofbread.spmp.widget.action.WidgetClickAction.CommonWidgetClickAction.TOGGLE_LIKE
import com.toasterofbread.spmp.widget.action.WidgetClickAction.CommonWidgetClickAction.TOGGLE_VISIBILITY
import com.toasterofbread.spmp.widget.action.execute
import com.toasterofbread.spmp.widget.component.GlanceText
import com.toasterofbread.spmp.widget.component.styledcolumn.GLANCE_STYLED_COLUMN_DEFAULT_SPACING
import com.toasterofbread.spmp.widget.component.styledcolumn.GlanceStyledColumn
import com.toasterofbread.spmp.widget.configuration.SpMpWidgetConfiguration
import com.toasterofbread.spmp.widget.configuration.base.BaseWidgetConfig
import com.toasterofbread.spmp.widget.configuration.base.BaseWidgetConfig.ContentColour.DARK
import com.toasterofbread.spmp.widget.configuration.base.BaseWidgetConfig.ContentColour.LIGHT
import com.toasterofbread.spmp.widget.configuration.base.BaseWidgetConfig.ContentColour.THEME
import com.toasterofbread.spmp.widget.configuration.base.BaseWidgetConfigDefaultsMask
import com.toasterofbread.spmp.widget.configuration.enum.WidgetSectionTheme
import com.toasterofbread.spmp.widget.configuration.enum.colour
import com.toasterofbread.spmp.widget.configuration.type.TypeConfigurationDefaultsMask
import com.toasterofbread.spmp.widget.configuration.type.TypeWidgetConfig
import com.toasterofbread.spmp.widget.modifier.systemCornerRadius
import dev.toastbits.composekit.platform.composable.theme.LocalApplicationTheme
import dev.toastbits.composekit.settings.ui.NamedTheme
import dev.toastbits.composekit.settings.ui.ThemeValues
import dev.toastbits.composekit.settings.ui.ThemeValuesData
import dev.toastbits.composekit.utils.common.getThemeColour
import dev.toastbits.composekit.utils.common.thenIf
import dev.toastbits.ytmkt.model.external.SongLikedStatus
import dev.toastbits.ytmkt.model.external.ThumbnailProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.FontResource

@Suppress("UNCHECKED_CAST")
abstract class SpMpWidget<A: TypeWidgetClickAction, T: TypeWidgetConfig<A>>(
    exact_size: Boolean = false,
    private val custom_background: Boolean = false
): GlanceAppWidget() {
    override val sizeMode: SizeMode =
        if (exact_size) SizeMode.Exact
        else SizeMode.Single

    var visible: Boolean by mutableStateOf(true)

    protected lateinit var context: AppContext

    protected val type_configuration: T
        get() = configuration.type_configuration as T

    private val base_configuration: BaseWidgetConfig
        get() = configuration.base_configuration

    protected val widget_background_colour: Color
        @Composable
        get() = LocalApplicationTheme.current.card.copy(alpha = base_configuration.background_opacity)

    private val coroutine_scope: CoroutineScope = CoroutineScope(Job())
    private var widget_id: Int? by mutableStateOf(null)
    private val widget_type: SpMpWidgetType = SpMpWidgetType.entries.first { it.widgetClass == this::class }
    private var configuration: SpMpWidgetConfiguration<A> by
        mutableStateOf(
            SpMpWidgetConfiguration(
                BaseWidgetConfig(),
                BaseWidgetConfigDefaultsMask(),
                widget_type.default_config as T,
                widget_type.default_defaults_mask as TypeConfigurationDefaultsMask<TypeWidgetConfig<A>>
            )
        )

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

            println("Widget $widget_id update received ($widget_type)")

            CompositionLocalProvider(
                // App
                LocalPlayerState provides state,
                dev.toastbits.composekit.platform.LocalContext provides this.context,

                // System
                LocalContext provides context,
                LocalConfiguration provides context.resources.configuration,
                LocalDensity provides Density(context.resources.displayMetrics.density),
                LocalLayoutDirection provides if (context.resources.getBoolean(R.bool.is_rtl)) LayoutDirection.Rtl else LayoutDirection.Ltr
            ) {
                val theme: NamedTheme by observeCurrentTheme(this.context, base_configuration.theme_index)

                val on_background_colour: Color =
                    when (base_configuration.content_colour) {
                        THEME -> theme.theme.on_background
                        LIGHT -> Color.White
                        DARK -> Color.Black
                    }

                CompositionLocalProvider(
                    *listOfNotNull(
                        LocalApplicationTheme provides theme.theme.copy(on_background = on_background_colour),
                        if (!custom_background) LocalContentColor provides on_background_colour else null
                    ).toTypedArray()
                ) {
                    Box(
                        GlanceModifier
                            .fillMaxSize()
                            .clickable(WidgetActionCallback(configuration.type_configuration.click_action)),
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
                            Column(
                                GlanceModifier
                                    .fillMaxSize()
                                    .thenIf(!custom_background) {
                                        background(widget_background_colour)
                                    }
                                    .systemCornerRadius()
                            ) {
                                if (base_configuration.show_debug_information) {
                                    DebugInfoItems(GlanceModifier)
                                }

                                Box(
                                    GlanceModifier.fillMaxSize().defaultWeight(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    WithCurrentSongImage { song, song_image ->
                                        Content(
                                            song, song_image, GlanceModifier.wrapContentSize(),
                                            PaddingValues(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
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
            is WidgetClickAction.CommonWidgetClickAction -> coroutine_scope.launch {
                action.execute(id, context, this@SpMpWidget)
            }
            is WidgetClickAction.Type -> executeTypeAction(action.actionEnum)
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
        val config: SpMpWidgetConfiguration<A> by SpMpWidgetConfiguration.observeForWidget(this.context, widget_type, widget_id) as MutableState<SpMpWidgetConfiguration<A>>
        val base_default: BaseWidgetConfig by context.settings.widget.DEFAULT_BASE_WIDGET_CONFIGURATION.observe()
        val type_defaults: Map<SpMpWidgetType, TypeWidgetConfig<out TypeWidgetClickAction>> by context.settings.widget.DEFAULT_TYPE_WIDGET_CONFIGURATIONS.observe()
        val type_default: TypeWidgetConfig<A> = (type_defaults[widget_type] ?: widget_type.default_config) as TypeWidgetConfig<A>

        configuration = remember(config, base_default, type_default) {
            config.copy(
                base_configuration = config.base_configuration_defaults_mask.applyTo(config.base_configuration, base_default),
                type_configuration = config.type_configuration_defaults_mask.applyTo(config.type_configuration, type_default)
            )
        }
    }

    protected abstract fun executeTypeAction(action: A)

    @Composable
    protected abstract fun Content(
        song: Song?,
        song_image: Bitmap?,
        modifier: GlanceModifier,
        content_padding: PaddingValues
    )

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
        colour: Color = LocalContentColor.current,
        alpha: Float = 1f,
        max_width: Dp? = null
    ) {
        val ui_language: String by context.observeUiLanguage()
        val app_font_mode: FontMode by context.settings.system.FONT.observe()
        val font: FontResource? = (base_configuration.font ?: app_font_mode).getFontResource(ui_language)

        GlanceText(
            text = text,
            font = font,
            modifier = modifier,
            font_size = font_size * base_configuration.font_size,
            max_width = max_width,
            colour = colour.copy(alpha = alpha)
        )
    }

    @Composable
    fun StyledColumn(
        section_theme_modes: List<WidgetSectionTheme>,
        vararg content: @Composable ColumnScope.() -> Unit,
        modifier: GlanceModifier = GlanceModifier,
        vertical_alignment: Alignment.Vertical = Alignment.Top,
        spacing: Dp = GLANCE_STYLED_COLUMN_DEFAULT_SPACING,
        content_padding: PaddingValues = PaddingValues(),
        order: List<Int> = section_theme_modes.indices.toList()
    ) {
        GlanceStyledColumn(
            border_mode = base_configuration.styled_border_mode,
            section_theme_modes = section_theme_modes,
            content = content,
            modifier = modifier,
            vertical_alignment = vertical_alignment,
            spacing = spacing,
            content_padding = content_padding,
            order = order,
            getBackgroundColour = {
                when (it.mode) {
                    WidgetSectionTheme.Mode.BACKGROUND -> widget_background_colour.copy(alpha = it.opacity)
                    else -> it.colour
                }
            }
        )
    }

    @Composable
    fun AppIcon(
        colour: Color,
        modifier: GlanceModifier = GlanceModifier,
        show: Boolean = true
    ) {
        val size: Dp = 20.dp

        if (show && base_configuration.show_app_icon) {
            Box(
                modifier.height(size),
                contentAlignment = Alignment.TopEnd
            ) {
                Image(
                    ImageProvider(R.drawable.ic_spmp),
                    null,
                    GlanceModifier.size(size - 2.dp),
                    colorFilter = ColorFilter.tint(ColorProvider(colour))
                )
            }
        }
        else {
            Spacer(modifier.height(size / 2))
        }
    }

    @Composable
    private fun WithCurrentSongImage(
        content: @Composable (Song?, Bitmap?) -> Unit
    ) {
        val player: PlayerState = LocalPlayerState.current
        val song: Song? = player.status.m_song

        if (song == null) {
            content(null, null)
            return
        }

        song.Thumbnail(
            ThumbnailProvider.Quality.HIGH,
            contentOverride = {
                val theme: ThemeValues = LocalApplicationTheme.current
                val app_accent_source: AccentColourSource by context.settings.theme.ACCENT_COLOUR_SOURCE.observe()
                val current_accent: Color =
                    when (base_configuration.accent_colour_source ?: app_accent_source) {
                        AccentColourSource.THEME -> theme.accent
                        AccentColourSource.THUMBNAIL -> {
                            val song_theme: Color? by song.ThemeColour.observe(player.database)
                            val image_accent: Color? = it?.getThemeColour()
                            song_theme ?: image_accent ?: theme.accent
                        }
                    }

                CompositionLocalProvider(
                    LocalApplicationTheme provides ThemeValuesData.of(theme).copy(accent = current_accent)
                ) {
                    content(song, it?.asAndroidBitmap())
                }
            }
        )
    }

    companion object {
        private val active_widgets: MutableMap<Int, SpMpWidget<*, *>> = mutableMapOf()

        fun runActionOnWidget(action: WidgetClickAction<TypeWidgetClickAction>, widget_glance_id: GlanceId) {
            val widget: SpMpWidget<*, *> = SpMpWidget.active_widgets[widget_glance_id.getDatabaseId()]!!
            widget.runAction(action, widget_glance_id)
        }
    }
}

@SuppressLint("RestrictedApi")
fun GlanceId.getDatabaseId(): Int =
    when (this) {
        is AppWidgetId -> appWidgetId
        else -> throw NotImplementedError(this::class.toString())
    }
