package com.toasterofbread.spmp

import com.toasterofbread.spmp.platform.AppContext

enum class PlayerAccessibilityServiceVolumeInterceptMode {
    ALWAYS,
    APP_OPEN,
    NEVER
}

expect class PlayerAccessibilityService() {
    companion object {
        fun isSupported(): Boolean

        fun addEnabledListener(listener: (Boolean) -> Unit, context: AppContext)
        fun removeEnabledListener(listener: (Boolean) -> Unit, context: AppContext)
        fun isEnabled(context: AppContext): Boolean

        fun enableInteractive(context: AppContext)
        fun enable(context: AppContext, root: Boolean)
        fun disable()

        fun isSettingsPermissionGranted(context: AppContext): Boolean
        fun requestRootPermission(callback: (granted: Boolean) -> Unit)

        fun isOverlayPermissionGranted(context: AppContext): Boolean
        fun requestOverlayPermission(context: AppContext, callback: (success: Boolean) -> Unit)
    }
}