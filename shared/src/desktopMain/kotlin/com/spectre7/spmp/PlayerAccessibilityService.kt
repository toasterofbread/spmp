package com.spectre7.spmp

import com.spectre7.spmp.platform.PlatformContext

actual class PlayerAccessibilityService actual constructor() {
    actual companion object {
        actual fun isSupported(): Boolean = false

        actual fun addEnabledListener(listener: (Boolean) -> Unit, context: PlatformContext) {
            throw NotImplementedError()
        }

        actual fun removeEnabledListener(listener: (Boolean) -> Unit, context: PlatformContext) {
            throw NotImplementedError()
        }

        actual fun isEnabled(context: PlatformContext): Boolean {
            throw NotImplementedError()
        }

        actual fun enable(context: PlatformContext, root: Boolean) {
            throw NotImplementedError()
        }

        actual fun disable() {
            throw NotImplementedError()
        }

        actual fun isSettingsPermissionGranted(context: PlatformContext): Boolean {
            TODO("Not yet implemented")
        }

        actual fun requestRootPermission(callback: (granted: Boolean) -> Unit) {
        }

        actual fun isOverlayPermissionGranted(context: PlatformContext): Boolean {
            TODO("Not yet implemented")
        }

        actual fun requestOverlayPermission(context: PlatformContext, callback: (success: Boolean) -> Unit) {
        }
    }
}