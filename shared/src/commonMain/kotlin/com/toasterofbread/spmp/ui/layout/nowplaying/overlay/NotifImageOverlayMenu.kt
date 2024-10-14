package com.toasterofbread.spmp.ui.layout.nowplaying.overlay

import androidx.compose.runtime.Composable

@Composable
expect fun notifImagePlayerOverlayMenuButtonText(): String?

expect class NotifImagePlayerOverlayMenu(): PlayerOverlayMenu
