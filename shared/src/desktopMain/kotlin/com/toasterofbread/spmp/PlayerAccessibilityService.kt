package com.toasterofbread.spmp

import com.toasterofbread.spmp.platform.AppContext

actual class PlayerAccessibilityService actual constructor() {
    actual companion object {
        actual fun isSupported(): Boolean = false

        actual fun addEnabledListener(listener: (Boolean) -> Unit, context: AppContext) {
            throw NotImplementedError()
        }

        actual fun removeEnabledListener(listener: (Boolean) -> Unit, context: AppContext) {
            throw NotImplementedError()
        }

        actual fun isEnabled(context: AppContext): Boolean {
            throw NotImplementedError()
        }

        actual fun enable(context: AppContext, root: Boolean) {
            throw NotImplementedError()
        }

        actual fun disable() {
            throw NotImplementedError()
        }

        actual fun isSettingsPermissionGranted(context: AppContext): Boolean {
            TODO("Not yet implemented")
        }

        actual fun requestRootPermission(callback: (granted: Boolean) -> Unit) {
        }

        actual fun isOverlayPermissionGranted(context: AppContext): Boolean {
            TODO("Not yet implemented")
        }

        actual fun requestOverlayPermission(context: AppContext, callback: (success: Boolean) -> Unit) {
        }
    }
}