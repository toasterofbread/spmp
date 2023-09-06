package com.toasterofbread.spmp.ui.layout.library

class LibraryPlaylistsPage(val context: PlatformContext): LibrarySubPage(context) {
    override fun getIcon(): ImageVector =
        MediaItemType.PLAYLIST_REM.getIcon()

    override fun isHidden(): Boolean = own_channel == null
    
    private val own_channel: Artist? get() = context.ytapi.user_auth_state?.own_channel

    @Composable
    override fun Page(
        library_page: LibraryPage,
        content_padding: PaddingValues,
        multiselect_context: MediaItemMultiSelectContext,
        modifier: Modifier
    ) {
        TODO)()
    }
}