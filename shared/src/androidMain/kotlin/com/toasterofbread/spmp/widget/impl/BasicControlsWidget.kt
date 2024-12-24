package com.toasterofbread.spmp.widget.impl

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
//                THEME -> theme.theme.onBackground
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
//                    contentColor = ColorProvider(theme.theme.onAccent)
//                )
//            )
//        }
//    }
//}
