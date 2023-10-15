package com.toasterofbread.spmp

import com.toasterofbread.spmp.platform.PlatformContext

actual class PlayerAccessibilityService actual constructor() {
    actual companion object {
        actual fun isSupported(): Boolean {
            TODO("Not yet implemented")
        }

        actual fun addEnabledListener(listener: (Boolean) -> Unit, context: PlatformContext) {
        }

        actual fun removeEnabledListener(listener: (Boolean) -> Unit, context: PlatformContext) {
        }

        actual fun isEnabled(context: PlatformContext): Boolean {
            TODO("Not yet implemented")
        }

        actual fun enableInteractive(context: PlatformContext) {
        }

        actual fun enable(context: PlatformContext, root: Boolean) {
        }

        actual fun disable() {
        }

        actual fun isSettingsPermissionGranted(context: PlatformContext): Boolean {
            TODO("Not yet implemented")
        }

        actual fun requestRootPermission(callback: (granted: Boolean) -> Unit) {
        }

        actual fun isOverlayPermissionGranted(context: PlatformContext): Boolean {
            TODO("Not yet implemented")
        }

        actual fun requestOverlayPermission(
            context: PlatformContext,
            callback: (success: Boolean) -> Unit,
        ) {
        }

    }
}