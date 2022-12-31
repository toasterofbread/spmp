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
import kotlin.concurrent.thread

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
                    runAsRootAndWait(
                        "pm grant ${context.packageName} ${Manifest.permission.WRITE_SECURE_SETTINGS}",
                        { callback(GrantError.ROOT_NOT_GRANTED, null) }
                    ) { result ->
                        callback(
                            if (result == 0) GrantError.OK else GrantError.SHELL_ERROR,
                            if (result == 0) null else getStderr()
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

        fun requestRootPermission(callback: (granted: Boolean) -> Unit) {
            runAsRootAndWait("ls", { callback(false) }) {
                callback(true)
            }
        }

        fun runAsRoot(command: String): Process {
            return Runtime.getRuntime().exec(arrayOf("su", "-c", command))
        }

        class OutputProvider(private val _process: Process) {
            fun getStdout(): String {
                val stream = _process.inputStream
                val ret = stream.bufferedReader().readText()
                stream.close()
                return ret
            }

            fun getStderr(): String {
                val stream = _process.errorStream
                val ret = stream.bufferedReader().readText()
                stream.close()
                return ret
            }
        }

        fun runAsRootAndWait(
            command: String,
            onRootNotGranted: () -> Unit,
            block: Boolean = false,
            callback: OutputProvider.(result: Int) -> Unit
        ): Process? {

            val process = runAsRoot(command)

            fun run(): Int {
                val result = process.waitFor()
                if (result == 13) {
                    onRootNotGranted()
                    return result
                }
                callback(OutputProvider(process), result)
                return result
            }

            if (block) {
                if (run() != 0) {
                    return null
                }
            }
            else {
                thread {
                    run()
                }
            }

            return process
        }
    }
}