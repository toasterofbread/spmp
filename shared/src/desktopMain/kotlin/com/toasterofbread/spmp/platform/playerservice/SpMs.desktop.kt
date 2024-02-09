package com.toasterofbread.spmp.platform.playerservice

import org.jetbrains.skiko.OS
import org.jetbrains.skiko.hostOs
import java.io.File
import java.lang.System.getenv
import com.toasterofbread.spmp.platform.AppContext

actual fun getSpMsMachineId(context: AppContext): String {
    val id_file: File =
        when (hostOs) {
            OS.Linux -> File("/tmp/")
            OS.Windows -> File("${getenv("USERPROFILE")!!}/AppData/Local/")
            else -> throw NotImplementedError(hostOs.name)
        }.resolve("spmp_machine_id.txt")

    return getSpMsMachineIdFromFile(id_file)
}

actual fun getServerExecutableFilename(): String? =
    when (hostOs) {
        OS.Linux -> "spms.kexe"
        OS.Windows -> "spms.exe"
        else -> null
    }
