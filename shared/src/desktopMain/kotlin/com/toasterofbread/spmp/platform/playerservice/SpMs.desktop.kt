package com.toasterofbread.spmp.platform.playerservice

import org.jetbrains.skiko.OS
import org.jetbrains.skiko.hostOs
import java.io.File
import java.lang.System.getenv

actual fun getSpMsMachineId(): String {
    val id_file: File =
        when (hostOs) {
            OS.Linux -> File("/tmp/")
            OS.Windows -> File("${getenv("USERPROFILE")!!}/AppData/Local/")
            else -> throw NotImplementedError(hostOs.name)
        }.resolve("spmp_machine_id.txt")

    if (id_file.exists()) {
        return id_file.readText()
    }

    if (!id_file.parentFile.exists()) {
        id_file.parentFile.mkdirs()
    }

    val id_length: Int = 8
    val allowed_chars: List<Char> = ('A'..'Z') + ('a'..'z') + ('0'..'9')

    val new_id: String = (1..id_length).map { allowed_chars.random() }.joinToString("")

    id_file.writeText(new_id)

    return new_id
}
