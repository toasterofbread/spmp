abstract class OverlayMenu {
    @Composable
    abstract fun Menu(song: Song, seek_state: Any, openShutterMenu: (@Composable () -> Unit) -> Unit, close: () -> Unit)

    abstract fun closeOnTap(): Boolean
}