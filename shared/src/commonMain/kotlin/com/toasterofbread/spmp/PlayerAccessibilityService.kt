package com.toasterofbread.spmp

import com.toasterofbread.spmp.platform.PlatformContext

enum class PlayerAccessibilityServiceVolumeInterceptMode {
    ALWAYS,
    APP_OPEN,
    NEVER
}

expect class PlayerAccessibilityService() {
    companion object {
        fun isSupported(): Boolean

        fun addEnabledListener(listener: (Boolean) -> Unit, context: PlatformContext)
        fun removeEnabledListener(listener: (Boolean) -> Unit, context: PlatformContext)
        fun isEnabled(context: PlatformContext): Boolean

        fun enableInteractive(context: PlatformContext)
        fun enable(context: PlatformContext, root: Boolean)
        fun disable()

        fun isSettingsPermissionGranted(context: PlatformContext): Boolean
        fun requestRootPermission(callback: (granted: Boolean) -> Unit)

        fun isOverlayPermissionGranted(context: PlatformContext): Boolean
        fun requestOverlayPermission(context: PlatformContext, callback: (success: Boolean) -> Unit)
    }
}