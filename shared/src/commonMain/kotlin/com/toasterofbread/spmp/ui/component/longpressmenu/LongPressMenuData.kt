package com.toasterofbread.spmp.ui.component.longpressmenu

data class LongPressMenuData(
    val item: MediaItem,
    val thumb_shape: Shape? = null,
    val infoContent: (@Composable ColumnScope.(accent: Color) -> Unit)? = null,
    val info_title: String? = null,
    val getInitialInfoTitle: (@Composable () -> String?)? = null,
    val multiselect_context: MediaItemMultiSelectContext? = null,
    val multiselect_key: Int? = null,
    val playlist_as_song: Boolean = false
) {
    var current_interaction_stage: MediaItemPreviewInteractionPressStage? by mutableStateOf(null)
    private val coroutine_scope = CoroutineScope(Dispatchers.Main)
    private val HINT_MIN_STAGE = MediaItemPreviewInteractionPressStage.LONG_1

    fun getInteractionHintScale(): Int {
        return current_interaction_stage?.let {
            if (it < HINT_MIN_STAGE) 0
            else it.ordinal - HINT_MIN_STAGE.ordinal + 1
        } ?: 0
    }

    @Composable
    fun Actions(provider: LongPressMenuActionProvider, spacing: Dp) {
        with(provider) {
            if (item is Song || (item is Playlist && playlist_as_song)) {
                SongLongPressMenuActions(item, spacing, multiselect_key) { callback ->
                    coroutine_scope.launch {
                        if (item is Song) {
                            callback(item)
                        }
                        else {
                            check(item is Playlist)
                            item.getFeedLayouts().onSuccess { layouts ->
                                layouts.firstOrNull()?.items?.firstOrNull()?.also {
                                    callback(it as Song)
                                }
                            }
                        }
                    }
                }
            }
            else if (item is Playlist) {
                PlaylistLongPressMenuActions(item)
            }
            else if (item is Artist) {
                ArtistLongPressMenuActions(item)
            }
            else {
                throw NotImplementedError(item.type.toString())
            }
        }
    }

    @Composable
    fun SideButton(modifier: Modifier, background: Color, accent: Color) {
        when (item) {
            is Song -> LikeDislikeButton(item, modifier) { background.getContrasted() }
            is Artist -> ArtistSubscribeButton(item, modifier)
        }
    }
}