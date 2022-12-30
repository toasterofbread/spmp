package com.spectre7.utils

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.spectre7.spmp.MainActivity
import com.topjohnwu.superuser.Shell

class Permissions {

    enum class GrantError {
        OK,
        USER_CANCELLED,
        ROOT_NOT_GRANTED,
        SHELL_ERROR,
        HANDLED_BY_SYSTEM,
    }

    companion object {

        private var permission_request_callback: ((granted: Boolean) -> Unit)? = null
        private var permission_request: ActivityResultLauncher<String>? = null

        fun init(activity: ComponentActivity) {
            permission_request = activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                permission_request_callback?.invoke(it)
                permission_request_callback = null
            }
        }

        fun hasPermission(permission: String, context: Context): Boolean {
            return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        }

        fun requestPermission(permission: String, context: Context, callback: (result: GrantError, error: String?) -> Unit) {

            when (permission) {
                Manifest.permission.SYSTEM_ALERT_WINDOW -> {
                    val dialog = AlertDialog.Builder(context)
                    dialog.setCancelable(true)
                    dialog.setTitle("Overlay permission needed")
                    dialog.setMessage("Please allow permission for '${getAppName(context)}' on the next screen")
                    dialog.setPositiveButton("Ok") { _, _ ->
                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                        intent.addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK
                                    or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                        )
                        MainActivity.context.startActivity(intent)
                    }
                    dialog.setNegativeButton("Cancel") { _, _ -> }
                    dialog.create().show()

                    callback(GrantError.HANDLED_BY_SYSTEM, null)
                    return
                }

                Manifest.permission.WRITE_SECURE_SETTINGS -> {
                    getRootShell({ callback(GrantError.ROOT_NOT_GRANTED, null) }) { shell ->
                        val result = shell.newJob().add("pm grant ${context.packageName} ${Manifest.permission.WRITE_SECURE_SETTINGS}").exec()
                        callback(
                            if (result.isSuccess) GrantError.OK else GrantError.SHELL_ERROR,
                            result.err.joinToString("\n")
                        )
                    }
                }

                else -> {
                    if (permission_request == null) {
                        throw RuntimeException("Permissions must be initialised")
                    }

                    permission_request_callback = { callback(if (it) GrantError.OK else GrantError.USER_CANCELLED, null) }
                    permission_request!!.launch(permission)
                }
            }
        }

        // TODO | Figure this out (seems inconsistent)
        fun getRootShell(onFail: (() -> Unit)? = null, callback: (Shell) -> Unit) {
            Shell.getShell { shell ->
                if (!shell.isRoot) {
                    // Close shell so that root access can be requested again
                    shell.close()
                    onFail?.invoke()
                }
                else {
                    callback(shell)
                }
            }
        }
    }
}