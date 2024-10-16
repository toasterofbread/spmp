package com.toasterofbread.spmp.widget.impl

import LocalPlayerState
import ProgramArguments
import SpMp
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.toasterofbread.spmp.db.Database
import com.toasterofbread.spmp.model.lyrics.SongLyrics
import com.toasterofbread.spmp.model.lyrics.SongLyrics.Term
import com.toasterofbread.spmp.model.mediaitem.loader.SongLyricsLoader
import com.toasterofbread.spmp.model.mediaitem.song.STATIC_LYRICS_SYNC_OFFSET
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.settings.category.observeCurrentTheme
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.nowplaying.ThemeMode
import com.toasterofbread.spmp.widget.SpMpWidget
import com.toasterofbread.spmp.widget.configuration.BaseWidgetConfiguration.ContentColour.*
import com.toasterofbread.spmp.widget.configuration.SpMpWidgetConfiguration
import dev.toastbits.composekit.settings.ui.NamedTheme
import dev.toastbits.composekit.settings.ui.on_accent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

//internal class BasicControlsWidget: SpMpWidget() {
//
//
//    override suspend fun provideGlance(context: Context, id: GlanceId) {
//        val app_context: AppContext = AppContext.create(context, coroutine_scope)
//
//        val np_theme_mode: ThemeMode = app_context.settings.theme.NOWPLAYING_THEME_MODE.get()
//        val swipe_sensitivity: Float = app_context.settings.player.EXPAND_SWIPE_SENSITIVITY.get()
//
//        val widget_id: Int = GlanceAppWidgetManager(context).getAppWidgetId(id)
//
//        provideContent {
//            val composable_coroutine_scope = rememberCoroutineScope()
//            val state: PlayerState =
//                remember {
//                    PlayerState(app_context, ProgramArguments(), composable_coroutine_scope, np_theme_mode, swipe_sensitivity)
//                }
//
//            CompositionLocalProvider(
//                LocalPlayerState provides state,
//                LocalConfiguration provides context.resources.configuration,
//                LocalDensity provides Density(context.resources.displayMetrics.density)
//            ) {
//                Content(app_context, widget_id)
//            }
//        }
//    }
//
//    override suspend fun onDelete(context: Context, glanceId: GlanceId) {
//        coroutine_scope.cancel()
//    }
//
//    @Composable
//    fun Content(app_context: AppContext, id: Int) {
//        val song: Song? = SpMp._player_state?.status?.m_song
//        val lyrics: SongLyricsLoader.ItemState? = song?.let { SongLyricsLoader.rememberItemState(it, app_context) }
//
//        val configuration: SpMpWidgetConfiguration by SpMpWidgetConfiguration.observeForWidget(app_context, widget_type, id)
//
//        val theme: NamedTheme by observeCurrentTheme(configuration.base_configuration.theme_index)
//
//        val on_background_colour: Color =
//            when (configuration.base_configuration.content_colour) {
//                THEME -> theme.theme.on_background
//                LIGHT -> Color.White
//                DARK -> Color.Black
//            }
//
//        Column(
//            GlanceModifier
//                .fillMaxSize()
//                .background(
//                    theme.theme.background
//                        .copy(alpha = configuration.base_configuration.background_opacity)
//                )
//        ) {
//            val text_style: TextStyle = TextStyle(color = ColorProvider(on_background_colour))
//
//            Text(configuration.toString(), style = text_style)
//
//            Text(theme.toString(), style = text_style)
//
//            Button(
//                "Test",
//                onClick = {},
//                colors = ButtonDefaults.buttonColors(
//                    backgroundColor = ColorProvider(theme.theme.accent),
//                    contentColor = ColorProvider(theme.theme.on_accent)
//                )
//            )
//        }
//    }
//}
